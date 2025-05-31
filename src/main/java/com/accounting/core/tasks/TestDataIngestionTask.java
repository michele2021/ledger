package com.accounting.core.tasks;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.commons.conf.SystemConfig;
import com.accounting.commons.kafka.KafkaMessage;
import com.accounting.commons.kafka.KafkaMessageProducer;
import com.accounting.core.dto.QueueMessageAmountDTO;
import com.accounting.core.dto.QueueMessageDTO;
import com.accounting.core.entities.Order;
import com.accounting.core.entities.OrderItem;
import com.accounting.core.entities.Payment;
import com.accounting.core.entities.TaxLine;
import io.dropwizard.servlets.tasks.Task;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.accounting.commons.tasks.TasksSwitcher.getSingleMandatoryParameter;
import static com.accounting.commons.utils.MappingUtils.OBJECT_MAPPER;

public class TestDataIngestionTask extends Task {
    private final KafkaMessageProducer<String, String> kafkaMessageProducer;
    private static final String COMPANY_ACCOUNT_CODE_PARAM = "accountCode";
    private static final String NUM_ORDERS_PARAM = "numOrders";

    public TestDataIngestionTask(KafkaMessageProducer<String,String> kafkaMessageProducer) {
        super("ingest-test-data");
        this.kafkaMessageProducer = kafkaMessageProducer;
    }

    private List<QueueMessageDTO> buildMessages(String accountCode, int num) {
        var ret = new ArrayList<QueueMessageDTO>();
        for (int i = 1; i <= num; i++) {
            var even = i % 2 == 0;
            buildOrderAndPayment(accountCode, i, even, ret);
        }
        return ret;
    }

    private static void buildOrderAndPayment(String accountCode, int i, boolean even, List<QueueMessageDTO> ret) {
        var uuid = UUID.randomUUID();
        var amount = QueueMessageAmountDTO
                .builder()
                .quantity(randomBigDecimal())
                .currencyCode(Constants.Currencies.EUR.toString())
                .build();
        var platform = even ? Constants.Platform.SHOPIFY : Constants.Platform.AMAZON;
        var taxLine = new TaxLine(amount.getQuantity().divide(BigDecimal.TEN, RoundingMode.CEILING).toString(), BigDecimal.TEN, "title");
        var orderItem = OrderItem.builder().sku("sku1").price("13.3").title("dopobarba").quantity(2).build();
        var order = Order.builder()
                .orderNumber(i + 50)
                .billingAddressCountry("via Roma, " + i)
                .platform(platform)
                .totalTax(taxLine.price())
                .orderItems(List.of(orderItem))
                .taxLines(List.of(taxLine))
                .build();
        var additionalDataOrder = OBJECT_MAPPER.valueToTree(order);
        var orderTxReference = "ORDER_REF_".concat(uuid.toString());
        var orderQueueMessage = QueueMessageDTO.builder()
                .type(Message.EventType.ORDER_RECEIVED.toString())
                .amount(amount)
                .txReference(orderTxReference)
                .accountCode(accountCode)
                .additionalData(additionalDataOrder)
                .source(Message.Source.MANUAL.toString())
                .build();
        ret.add(orderQueueMessage);

        var paymentProvider = even ? Constants.PaymentProvider.MOLLIE : Constants.PaymentProvider.STRIPE;
        var payment = Payment.builder()
                .paymentMethod("card")
                .paymentProvider(paymentProvider)
                .build();
        var additionalDataPayment = OBJECT_MAPPER.valueToTree(payment);
        var paymentTxReference = even ? orderTxReference : "PAYMENT_REF".concat(uuid.toString());
        var paymentQueueMessage = QueueMessageDTO.builder()
                .type(Message.EventType.PAYMENT_RECEIVED.toString())
                .amount(amount)
                .txReference(paymentTxReference)
                .uuid(UUID.randomUUID().toString())
                .accountCode(accountCode)
                .additionalData(additionalDataPayment)
                .source(Message.Source.MANUAL.toString())
                .build();
        ret.add(paymentQueueMessage);
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter printWriter) throws Exception {
        String accountCode = getSingleMandatoryParameter(parameters, COMPANY_ACCOUNT_CODE_PARAM);
        int num = Integer.parseInt(getSingleMandatoryParameter(parameters, NUM_ORDERS_PARAM));

        // test data can only be inserted on localhost
        if (SystemConfig.getInstanceType() != SystemConfig.LOCALHOST_SYSTEM) {
            return;
        }
        var queueMessageDTOS = buildMessages(accountCode, num);
        for(QueueMessageDTO qm : queueMessageDTOS) {
            var m = OBJECT_MAPPER.writeValueAsString(qm);
            var kafkaMessage = KafkaMessage.builder().key("MAIN").value(m).build();
            var serializedMessage = OBJECT_MAPPER.writeValueAsString(kafkaMessage);
            kafkaMessageProducer.send("default", "key", serializedMessage);
        }
    }

    private static BigDecimal randomBigDecimal() {
        double fraction = ThreadLocalRandom.current().nextDouble();
        var max = BigDecimal.valueOf(5000L);
        var result = BigDecimal.valueOf(fraction)
                .multiply(max);
        return result.setScale(2, RoundingMode.HALF_UP);
    }
}
