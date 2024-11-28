package com.accounting.core.jobs.datafix;

import com.accounting.commons.Constants;
import com.accounting.commons.Message;
import com.accounting.core.db.DataFixDAO;
import com.accounting.core.exceptions.JobProcessingException;
import com.accounting.core.jobs.entities.PaymentView;
import com.accounting.core.queue.SQSAccountingMessagesQueue;
import com.accounting.core.services.RepositoryHelper;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class PaymentsReverseExecutor {

    private final Jdbi jdbi;
    private final SQSAccountingMessagesQueue accountingQueue;

    @Inject
    public PaymentsReverseExecutor(RepositoryHelper repositoryHelper, SQSAccountingMessagesQueue accountingMessagesQueue) {
        this.jdbi = repositoryHelper.getJdbi();
        this.accountingQueue = accountingMessagesQueue;
    }

    public void execute(String mode, Long txId, Long newPaymentId, boolean test) throws JobProcessingException {
        try (Handle handle = jdbi.open()) {
            DataFixDAO dataFixDAO = handle.attach(DataFixDAO.class);

            List<PaymentView> pReverse = dataFixDAO.getPaymentsJournalToReverse(txId);
            for(PaymentView p : pReverse) {
                BigDecimal amount = Constants.fromMU(p.getCurrencyCode(), p.getQuantityAsLong());
                log.info("Fetched payment to be reversed with txId [{}], accountCode [{}], amount [{}]", p.getTxId(), p.getAccountCode(), amount);
                if(test) {
                    log.info("Job running in 'test' mode, nothing to do.");
                    continue;
                }
                JsonObject message = buildPaymentReceived(p, Message.EventType.PAYMENT_RECEIVED_CORRECTION);
                SendMessageResponse sendMessageResponse = accountingQueue.sendMessage(message.toString());
                log.info("Message PAYMENT_RECEIVED_CORRECTION submitted to the accounting processing queue with id: {}", sendMessageResponse.messageId());
            }
            if("SUBMIT_NEW_PAYMENTS".equalsIgnoreCase(mode)) {
                List<PaymentView> pSubmit = dataFixDAO.getNewPaymentsToSubmit(newPaymentId);
                for(PaymentView p : pSubmit) {
                    log.info("Fetched payment to be submitted: [{}]", p);
                    if (test) {
                        log.info("Job running in 'test' mode, nothing to do.");
                        continue;
                    }
                    JsonObject message = buildPaymentReceived(p, Message.EventType.PAYMENT_RECEIVED);
                    SendMessageResponse sendMessageResponse = accountingQueue.sendMessage(message.toString());
                    log.info("Message PAYMENT_RECEIVED submitted to the accounting processing queue with id: {}", sendMessageResponse.messageId());
                }
            }
        }  catch (Exception e) {
            log.error("Exception while processing the data fix: ", e);
            throw new JobProcessingException("Exception while processing the data fix: ", e);
        }
    }

    private JsonObject buildPaymentReceived(PaymentView p, Message.EventType type) {
        String description;
        JsonObject message = new JsonObject();

        message.addProperty("type", type.toString());
        message.addProperty("accountCode", p.getAccountCode());

        // amount
        BigDecimal quantity;
        if(type == Message.EventType.PAYMENT_RECEIVED_CORRECTION) {
            quantity = Constants.fromMU(p.getCurrencyCode(), p.getQuantityAsLong());
            description = "HAMMER. Reversing payment with txId: " + p.getTxId();
        } else {
            quantity = p.getAmount();
            description = "HAMMER. Backdated payment to fix Fee/Discount/VAT amounts.";
        }
        JsonObject amountElement = new JsonObject();
        amountElement.addProperty("quantity", quantity);
        amountElement.addProperty("currencyCode", p.getCurrencyCode().toString());
        message.add("amount", amountElement);

        // additionalData
        JsonObject additionalData = new JsonObject();
        additionalData.addProperty(Message.AdditionalData.bankAccountCode.toString(), p.getBankAccountCode());
        // fee/discount/vat only when creating new payments (PAYMENT_RECEIVED).
        if(p.getFee() != null) {
            additionalData.addProperty(Message.AdditionalData.handlingFeeAmount.toString(), p.getFee());
        }
        if(p.getDiscount() != null) {
            additionalData.addProperty(Message.AdditionalData.discountAmount.toString(), p.getDiscount());
        }
        if(p.getVat() != null) {
            additionalData.addProperty(Message.AdditionalData.vatAmount.toString(), p.getVat());
        }

        String bookingDate = p.getCreationDateAsOffset().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        additionalData.addProperty(Message.AdditionalData.bookingDate.toString(), bookingDate);
        additionalData.addProperty(Message.AdditionalData.description.toString(),description);
        message.add("additionalData", additionalData);

        // add a timestamp to make the message unique (fifo queue have a deduplication feature)
        long millis = System.currentTimeMillis();
        message.addProperty("timestamp", millis);
        // message source
        message.addProperty("source", Message.Source.ACCOUNTING_CORE.toString());

        return message;
    }
}
