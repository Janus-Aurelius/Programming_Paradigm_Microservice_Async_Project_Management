package com.pm.notificationservice.config;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;

import com.pm.commoncontracts.envelope.EventEnvelope;

import reactor.kafka.sender.SenderOptions;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.key-serializer}")
    private String keySerializer;

    @Value("${spring.kafka.producer.value-serializer}")
    private String valueSerializer;

    @Bean
    public ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> kafkaTemplate() {
        // Use Spring Boot's KafkaProperties to inherit application.yml settings
        Map<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);

        // Essential: Add type mappings to match what consumers expect
        props.put(org.springframework.kafka.support.serializer.JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        props.put(org.springframework.kafka.support.serializer.JsonSerializer.TYPE_MAPPINGS,
                "eventEnvelope:com.pm.commoncontracts.envelope.EventEnvelope,"
                + "notificationToSendEventPayload:com.pm.commoncontracts.events.notification.NotificationToSendEventPayload"
        );
        props.put("spring.json.trusted.packages", "com.pm.commoncontracts.*");

        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(props));
    }
}
