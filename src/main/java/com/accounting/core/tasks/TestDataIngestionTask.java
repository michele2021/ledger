package com.accounting.core.tasks;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.conf.SystemConfig;
import com.accounting.commons.kafka.KafkaMessage;
import com.accounting.commons.kafka.KafkaMessageProducer;
import com.accounting.commons.utils.MappingUtils;
import com.accounting.core.dto.QueueMessageAmountDTO;
import com.accounting.core.dto.QueueMessageDTO;
import com.accounting.core.entities.Order;
import com.accounting.core.entities.OrderItem;
import com.accounting.core.entities.TaxLine;
import io.dropwizard.servlets.tasks.Task;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.accounting.commons.utils.MappingUtils.OBJECT_MAPPER;

public class TestDataIngestionTask extends Task {
    private final KafkaMessageProducer<String, String> kafkaMessageProducer;
    private static final String ACCOUNT_CODE = "CMPZERO";

    public TestDataIngestionTask(KafkaMessageProducer<String,String> kafkaMessageProducer) {
        super("ingest-test-data");
        this.kafkaMessageProducer = kafkaMessageProducer;
    }

    private List<QueueMessageDTO> buildOrderMessages() {
        List<QueueMessageDTO> ret = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            var amount = QueueMessageAmountDTO
                    .builder()
                    .quantity(BigDecimal.valueOf(i * 400))
                    .currencyCode(Constants.Currencies.EUR.toString())
                    .build();
            var platform = ((i % 2) == 0) ? Constants.Platform.SHOPIFY : Constants.Platform.AMAZON;
            var taxLine = new TaxLine(amount.getQuantity().divide(BigDecimal.TEN, RoundingMode.CEILING).toString(), BigDecimal.TEN, "title");
            var orderItem = OrderItem.builder().sku("sku1").price("13.3").title("dopobarba").quantity(2).build();
            Order order = Order.builder()
                    .orderNumber(i + 50)
                    .billingAddressCountry("via Roma " + i)
                    .platform(platform)
                    .totalTax(taxLine.price())
                    .orderItems(List.of(orderItem))
                    .taxLines(List.of(taxLine))
                    .build();

            var additionalData = MappingUtils.OBJECT_MAPPER.valueToTree(order);

            var txReference = "ORDER_REF_" + i;
            var queueMessage = QueueMessageDTO.builder()
                    .type(Message.EventType.ORDER_RECEIVED.toString())
                    .amount(amount)
                    .txReference(txReference)
                    .accountCode(ACCOUNT_CODE)
                    .additionalData(additionalData)
                    .source(Message.Source.MANUAL.toString())
                    .build();
            ret.add(queueMessage);
        }
        return ret;
    }

    @Override
    public void execute(Map<String, List<String>> map, PrintWriter printWriter) throws Exception {
        // test data can only be inserted on localhost
        if (SystemConfig.getInstanceType() != SystemConfig.LOCALHOST_SYSTEM) {
            return;
        }
        var orderMessages = buildOrderMessages();
        for(QueueMessageDTO qm : orderMessages) {
            var m = MappingUtils.OBJECT_MAPPER.writeValueAsString(qm);
            var kafkaMessage = KafkaMessage.builder().key("MAIN").value(m).build();
            var serializedMessage = OBJECT_MAPPER.writeValueAsString(kafkaMessage);
            kafkaMessageProducer.send("default", "key", serializedMessage);
        }
    }
}
