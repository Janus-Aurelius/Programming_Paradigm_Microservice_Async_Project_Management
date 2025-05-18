package com.pm.projectservice.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;

import com.pm.commoncontracts.envelope.EventEnvelope;

import reactor.kafka.sender.SenderOptions;


@Configuration
public class KafkaProducerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;    public static final String PROJECT_EVENTS_TOPIC = "project-events"; // Define topic name
    
    @Bean
    public ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> reactiveKafkaProducerTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);
        // Enable type headers for proper deserialization of generic types
        props.put(org.springframework.kafka.support.serializer.JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        props.put(org.springframework.kafka.support.serializer.JsonSerializer.TYPE_MAPPINGS, "eventEnvelope:com.pm.commoncontracts.envelope.EventEnvelope");

        SenderOptions<String, EventEnvelope<?>> senderOptions = SenderOptions.create(props);
        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }
}
