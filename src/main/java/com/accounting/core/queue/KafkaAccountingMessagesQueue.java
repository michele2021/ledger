package com.accounting.core.queue;

import com.accounting.commons.conf.AWSConfig;
import com.accounting.commons.conf.KafkaConfig;
import com.accounting.commons.kafka.KafkaMessage;
import com.accounting.commons.kafka.KafkaMessageProducer;
import com.accounting.commons.utils.MappingUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;

@Slf4j
public class KafkaAccountingMessagesQueue {

    private KafkaMessageProducer<String, String> kafkaMessageProducer;
    private final boolean isEnabled;
    private final String defaultQueueTopic;

    @Inject
    public KafkaAccountingMessagesQueue(KafkaMessageProducer<String, String> kafkaMessageProducer, AWSConfig awsConfig,
                                        KafkaConfig kafkaConfig) {
        this.kafkaMessageProducer = kafkaMessageProducer;
        this.isEnabled = awsConfig.isQueueEnabled();
        this.defaultQueueTopic = kafkaConfig.getDefaultQueueTopic();
    }


    public KafkaAccountingMessagesQueue(AWSConfig config, KafkaConfig kafkaConfig) {
        this.defaultQueueTopic = kafkaConfig.getDefaultQueueTopic();
        this.isEnabled = config.isQueueEnabled();
        if (isEnabled) {
            String queueName = config.getQueueAccountingName();
            log.info("SQS queue enabled. Accounting Messages queue name: {}", queueName);
        } else {
            log.info("SQS queue disabled in config.");
        }
    }

    public static KafkaAccountingMessagesQueue of(AWSConfig config, KafkaConfig kafkaConfig) {
        return new KafkaAccountingMessagesQueue(config, kafkaConfig);
    }

    public RecordMetadata sendMessage(String message) {
        if (!isEnabled) {
            return null;
        }
        log.info("Pushing message to the main accounting queue: {}", message);
        var kafkaMessage = KafkaMessage.builder().key("MAIN").value(message).build();
        try {
            var serializedMessage = MappingUtils.OBJECT_MAPPER.writeValueAsString(kafkaMessage);
            return kafkaMessageProducer.send(defaultQueueTopic, "MAIN", serializedMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
