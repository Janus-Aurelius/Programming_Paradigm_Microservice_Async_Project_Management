package com.pm.taskservice.config;

import java.util.Collections;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
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
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.pm.commoncontracts.envelope.EventEnvelope");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.pm.commoncontracts.*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        props.put(JsonDeserializer.TYPE_MAPPINGS, "eventEnvelope:com.pm.commoncontracts.envelope.EventEnvelope");
        return ReceiverOptions.<String, EventEnvelope<?>>create(props)
                .subscription(Collections.singleton(topic));
    }

    @Bean
    public KafkaReceiver<String, EventEnvelope<?>> userEventsReceiver(
            @Value("${kafka.topic.user-events:user-events}") String userEventsTopic) {
        return KafkaReceiver.create(createEventEnvelopeReceiverOptions(userEventsTopic));
    }
}
