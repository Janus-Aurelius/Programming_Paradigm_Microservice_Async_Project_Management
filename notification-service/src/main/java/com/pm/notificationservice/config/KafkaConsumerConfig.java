package com.pm.notificationservice.config;

import java.util.Collections;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.pm.commoncontracts.envelope.EventEnvelope;

import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;

@Configuration
public class KafkaConsumerConfig {
    private final KafkaProperties kafkaProperties;

    public KafkaConsumerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    private ReceiverOptions<String, EventEnvelope<?>> createEventEnvelopeReceiverOptions(String topic) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.pm.commoncontracts.envelope.EventEnvelope");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.pm.commoncontracts.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        props.put(JsonDeserializer.TYPE_MAPPINGS, "eventEnvelope:com.pm.commoncontracts.envelope.EventEnvelope");

        return ReceiverOptions.<String, EventEnvelope<?>>create(props)
                .subscription(Collections.singleton(topic));
    }

    @Bean
    public KafkaReceiver<String, EventEnvelope<?>> projectEventsReceiver(
            @Value("${kafka.topic.project-events}") String projectEventsTopic) {
        return KafkaReceiver.create(createEventEnvelopeReceiverOptions(projectEventsTopic));
    }

    @Bean
    public KafkaReceiver<String, EventEnvelope<?>> taskEventsReceiver(
            @Value("${kafka.topic.task-events}") String taskEventsTopic) {
        return KafkaReceiver.create(createEventEnvelopeReceiverOptions(taskEventsTopic));
    }

    @Bean
    public KafkaReceiver<String, EventEnvelope<?>> userEventsReceiver(
            @Value("${kafka.topic.user-events}") String userEventsTopic) {
        return KafkaReceiver.create(createEventEnvelopeReceiverOptions(userEventsTopic));
    }

    @Bean
    public KafkaReceiver<String, EventEnvelope<?>> commentEventsReceiver(
            @Value("${kafka.topic.comment-events}") String commentEventsTopic) {
        return KafkaReceiver.create(createEventEnvelopeReceiverOptions(commentEventsTopic));
    }
}