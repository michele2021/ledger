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
    public KafkaAccountingMessagesQueue(KafkaMessageProducer<String, String> kafkaMessageProducer, KafkaConfig kafkaConfig) {
        this.kafkaMessageProducer = kafkaMessageProducer;
        this.isEnabled = kafkaConfig.isQueueWriteEnabled();
        this.defaultQueueTopic = kafkaConfig.getDefaultQueueTopic();
    }

    public KafkaAccountingMessagesQueue(KafkaConfig kafkaConfig) {
        this.isEnabled = kafkaConfig.isQueueWriteEnabled();
        this.defaultQueueTopic = kafkaConfig.getDefaultQueueTopic();
        if (isEnabled) {
            log.info("Queue enabled. Messages queue name: {}", defaultQueueTopic);
        } else {
            log.info("Queue disabled in config.");
        }
    }

    public static KafkaAccountingMessagesQueue of(KafkaConfig kafkaConfig) {
        return new KafkaAccountingMessagesQueue(kafkaConfig);
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
