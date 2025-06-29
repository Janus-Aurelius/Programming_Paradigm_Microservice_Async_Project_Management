package com.pm.websocketservice.config;

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
        // Start with Spring Boot's auto-configured properties from application.yml
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);

        // Only override specific settings that we need for ReactiveKafka
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // These should already be set by application.yml, but ensure they're present
        props.putIfAbsent(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.pm.commoncontracts.envelope.EventEnvelope");
        props.putIfAbsent(JsonDeserializer.TRUSTED_PACKAGES, "com.pm.commoncontracts.*");
        props.putIfAbsent(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        // CRITICAL: Add type mappings to handle the notification service's type aliases
        props.put(JsonDeserializer.TYPE_MAPPINGS,
                "eventEnvelope:com.pm.commoncontracts.envelope.EventEnvelope,"
                + "notificationToSendEventPayload:com.pm.commoncontracts.events.notification.NotificationToSendEventPayload,"
                + "commentAddedEventPayload:com.pm.commoncontracts.events.comment.CommentAddedEventPayload,"
                + "commentEditedEventPayload:com.pm.commoncontracts.events.comment.CommentEditedEventPayload,"
                + "commentDeletedEventPayload:com.pm.commoncontracts.events.comment.CommentDeletedEventPayload"
        );

        return ReceiverOptions.<String, EventEnvelope<?>>create(props)
                .subscription(Collections.singleton(topic));
    }

    @Bean
    public KafkaReceiver<String, EventEnvelope<?>> notificationEventsReceiver(
            @Value("${kafka.topic.notification-dispatch:notifications-to-send}") String notificationTopic) {
        return KafkaReceiver.create(createEventEnvelopeReceiverOptions(notificationTopic));
    }

    @Bean
    public KafkaReceiver<String, EventEnvelope<?>> websocketDispatchReceiver(
            @Value("${kafka.topic.websocket-dispatch:websocket-dispatch}") String websocketDispatchTopic) {
        return KafkaReceiver.create(createEventEnvelopeReceiverOptions(websocketDispatchTopic));
    }
}
