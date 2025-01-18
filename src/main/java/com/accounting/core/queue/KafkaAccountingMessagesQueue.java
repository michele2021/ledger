package com.accounting.core.queue;

import com.accounting.commons.conf.AWSConfig;
import com.accounting.commons.kafka.KafkaMessage;
import com.accounting.commons.kafka.KafkaMessageProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;

@Slf4j
public class KafkaAccountingMessagesQueue {

    private KafkaMessageProducer kafkaMessageProducer;
    private final boolean isEnabled;

    @Inject
    public KafkaAccountingMessagesQueue(KafkaMessageProducer kafkaMessageProducer, AWSConfig awsConfig) {
        this.kafkaMessageProducer = kafkaMessageProducer;
        this.isEnabled = awsConfig.isQueueEnabled();
    }


    public KafkaAccountingMessagesQueue(AWSConfig config) {
        this.isEnabled = config.isQueueEnabled();
        if (isEnabled) {
            String queueName = config.getQueueAccountingName();
            log.info("SQS queue enabled. Accounting Messages queue name: {}", queueName);
        } else {
            log.info("SQS queue disabled in config.");
        }
    }

    public static KafkaAccountingMessagesQueue of(AWSConfig config) {
        return new KafkaAccountingMessagesQueue(config);
    }

    public RecordMetadata sendMessage(String message) {
        if (!isEnabled) {
            return null;
        }
        log.info("Pushing message to the main accounting queue: {}", message);
        var kafkaMessage = KafkaMessage.builder().key("MAIN").value(message).build();
        try {
            return kafkaMessageProducer.sendMessage(kafkaMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
