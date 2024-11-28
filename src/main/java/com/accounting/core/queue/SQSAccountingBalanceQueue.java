package com.accounting.core.queue;

import com.accounting.commons.conf.AWSConfig;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Slf4j
public class SQSAccountingBalanceQueue {

    private String url;
    private SqsClient client;
    private final boolean isEnabled;

    public SQSAccountingBalanceQueue(AWSConfig config) {
        this.isEnabled = config.isQueueEnabled();

        if (isEnabled) {
            String queueName = config.getQueueAccountingBalanceName();
            log.info("SQS queue enabled. Accounting-Balance queue name: {}", queueName);
            this.client = config.createSqsClient();
            this.url = config.getQueueUrl(client, queueName);
        } else {
            log.info("SQS queue disabled in config.");
        }
    }

    public static SQSAccountingBalanceQueue of(AWSConfig config) {
        return new SQSAccountingBalanceQueue(config);
    }

    public SendMessageResponse sendMessage(String message) {
        if (!isEnabled) {
            return null;
        }
        log.info("Pushing message to the accounting-balance queue: {}", message);
        SendMessageRequest smr = SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(message)
                .messageGroupId("mgr1")
                .build();
        return client.sendMessage(smr);
    }
}
