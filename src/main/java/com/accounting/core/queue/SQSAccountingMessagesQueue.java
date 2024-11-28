package com.accounting.core.queue;

import com.accounting.commons.SystemConstants;
import com.accounting.commons.conf.AWSConfig;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Slf4j
public class SQSAccountingMessagesQueue {

    private String url;
    private SqsClient client;
    private final boolean isEnabled;

    public SQSAccountingMessagesQueue(AWSConfig config) {
        this.isEnabled = config.isQueueEnabled();

        if (isEnabled) {
            String queueName = config.getQueueAccountingName();
            log.info("SQS queue enabled. Accounting Messages queue name: {}", queueName);
            this.client = config.createSqsClient();
            this.url = config.getQueueUrl(client, queueName);
        } else {
            log.info("SQS queue disabled in config.");
        }
    }

    public static SQSAccountingMessagesQueue of(AWSConfig config) {
        return new SQSAccountingMessagesQueue(config);
    }

    public SendMessageResponse sendMessage(String message) {
        if (!isEnabled) {
            return null;
        }
        log.info("Pushing message to the main accounting queue: {}", message);
        SendMessageRequest smr = SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(message)
                .messageGroupId(SystemConstants.SQSMessageGroupId.mgr1.toString())
                .build();
        return client.sendMessage(smr);
    }
}
