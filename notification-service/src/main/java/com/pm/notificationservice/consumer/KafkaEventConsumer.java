package com.pm.notificationservice.consumer;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.pm.commoncontracts.envelope.EventEnvelope;
import com.pm.notificationservice.config.MdcLoggingFilter;
import com.pm.notificationservice.service.NotificationService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

@Component
public class KafkaEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final NotificationService notificationService;
    private final KafkaReceiver<String, EventEnvelope<?>> projectEventsReceiver;
    private final KafkaReceiver<String, EventEnvelope<?>> taskEventsReceiver;
    private final KafkaReceiver<String, EventEnvelope<?>> userEventsReceiver;
    private final KafkaReceiver<String, EventEnvelope<?>> commentEventsReceiver;
    private List<Disposable> eventSubscriptions;

    public KafkaEventConsumer(
            NotificationService notificationService,
            KafkaReceiver<String, EventEnvelope<?>> projectEventsReceiver,
            KafkaReceiver<String, EventEnvelope<?>> taskEventsReceiver,
            KafkaReceiver<String, EventEnvelope<?>> userEventsReceiver,
            KafkaReceiver<String, EventEnvelope<?>> commentEventsReceiver) {
        this.notificationService = notificationService;
        this.projectEventsReceiver = projectEventsReceiver;
        this.taskEventsReceiver = taskEventsReceiver;
        this.userEventsReceiver = userEventsReceiver;
        this.commentEventsReceiver = commentEventsReceiver;
        this.eventSubscriptions = new ArrayList<>();
    }

    @PostConstruct
    public void start() {
        log.info("Starting Kafka consumers for all event topics");
        
        subscribeToReceiver(projectEventsReceiver, "project-events");
        subscribeToReceiver(taskEventsReceiver, "task-events");
        subscribeToReceiver(userEventsReceiver, "user-events");
        subscribeToReceiver(commentEventsReceiver, "comment-events");
    }

    private void subscribeToReceiver(KafkaReceiver<String, EventEnvelope<?>> receiver, String topicName) {
        log.info("Subscribing to topic: {}", topicName);
        Disposable subscription = receiver
                .receive()
                .flatMap(this::processRecord)
                .onErrorContinue((err, obj) -> log.error("Error processing Kafka record from topic {}, skipping. Record: {}", topicName, obj, err))
                .subscribe();
        eventSubscriptions.add(subscription);
    }

    private Mono<Void> processRecord(ReceiverRecord<String, EventEnvelope<?>> record) {
        EventEnvelope<?> envelope = record.value();
        String correlationId = envelope.correlationId() != null ? envelope.correlationId() : "N/A-kafka";

        // Set MDC for logging within this record's processing scope
        MDC.put(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, correlationId);
        MDC.put("kafkaEventId", envelope.eventId().toString());
        MDC.put("kafkaEventType", envelope.eventType());
        MDC.put("kafkaOffset", String.valueOf(record.offset()));
        MDC.put("kafkaTopic", record.topic());

        log.debug("Received record from Kafka. Offset: {}", record.offset());

        // Delegate processing to the service layer
        return notificationService.processIncomingEvent(envelope)
                .doOnSuccess(v -> record.receiverOffset().acknowledge()) // ACK on success
                .doOnError(e -> log.error("Failed to process event after consuming. CorrID: {}", correlationId, e)) // Log error, consider NACK?
                .doFinally(signalType -> { // Clean up MDC regardless of success/error
                    MDC.remove(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY);
                    MDC.remove("kafkaEventId");
                    MDC.remove("kafkaEventType");
                    MDC.remove("kafkaOffset");
                    MDC.remove("kafkaTopic");
                });
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping Kafka consumers...");
        eventSubscriptions.forEach(subscription -> {
            if (subscription != null && !subscription.isDisposed()) {
                subscription.dispose();
            }
        });
        log.info("Kafka consumers stopped.");
    }
}