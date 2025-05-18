package com.pm.taskservice.consumer;

import com.pm.commoncontracts.envelope.EventEnvelope;
import com.pm.commoncontracts.events.user.UserDeletedEventPayload;
import com.pm.taskservice.service.TaskService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

@Component
public class UserEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

    private final KafkaReceiver<String, EventEnvelope<?>> userEventsReceiver;
    private final TaskService taskService;
    private Disposable subscription;

    public UserEventConsumer(KafkaReceiver<String, EventEnvelope<?>> userEventsReceiver,
                             TaskService taskService) {
        this.userEventsReceiver = userEventsReceiver;
        this.taskService = taskService;
    }

    @PostConstruct
    public void start() {
        log.info("Starting Kafka consumer for user-events");
        this.subscription = userEventsReceiver.receive()
                .flatMap(this::processRecord)
                .onErrorContinue((err, obj) -> log.error("Error processing Kafka record from user-events, skipping. Record: {}", obj, err))
                .subscribe();
    }

    private Mono<Void> processRecord(ReceiverRecord<String, EventEnvelope<?>> record) {
        EventEnvelope<?> envelope = record.value();
        if (envelope == null) {
            log.warn("Received null EventEnvelope from Kafka record");
            record.receiverOffset().acknowledge();
            return Mono.empty();
        }
        String correlationId = envelope.correlationId() != null ? envelope.correlationId() : "N/A-kafka";
        MDC.put("correlationId", correlationId);
        MDC.put("kafkaEventType", envelope.eventType());
        try {
            if (envelope.payload() instanceof UserDeletedEventPayload payload) {
                String userId = payload.userDto().getId();
                log.info("Processing UserDeletedEvent for userId: {}", userId);
                return taskService.removeUserFromAllTasks(userId)
                        .doOnSuccess(v -> record.receiverOffset().acknowledge())
                        .doOnError(e -> log.error("Failed to process UserDeletedEvent for userId: {}", userId, e))
                        .doFinally(signal -> {
                            MDC.remove("correlationId");
                            MDC.remove("kafkaEventType");
                        });
            } else {
                record.receiverOffset().acknowledge();
                MDC.remove("correlationId");
                MDC.remove("kafkaEventType");
                return Mono.empty();
            }
        } catch (Exception e) {
            log.error("Exception processing user-events record", e);
            record.receiverOffset().acknowledge();
            MDC.remove("correlationId");
            MDC.remove("kafkaEventType");
            return Mono.empty();
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping Kafka consumer for user-events");
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }
}
