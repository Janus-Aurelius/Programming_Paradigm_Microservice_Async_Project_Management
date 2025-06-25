package com.pm.websocketservice.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.pm.commoncontracts.envelope.EventEnvelope;
import com.pm.commoncontracts.events.notification.NotificationToSendEventPayload;
import com.pm.commoncontracts.events.project.ProjectCreatedEventPayload;
import com.pm.commoncontracts.events.project.ProjectDeletedEventPayload;
import com.pm.commoncontracts.events.project.ProjectStatusChangedEventPayload;
import com.pm.commoncontracts.events.project.ProjectTaskCreatedEventPayload;
import com.pm.commoncontracts.events.project.ProjectUpdatedEventPayload;
import com.pm.commoncontracts.events.task.TaskAssignedEventPayload;
import com.pm.commoncontracts.events.task.TaskCreatedEventPayload;
import com.pm.commoncontracts.events.task.TaskDeletedEventPayload;
import com.pm.commoncontracts.events.task.TaskPriorityChangedEventPayload;
import com.pm.commoncontracts.events.task.TaskStatusChangedEventPayload;
import com.pm.commoncontracts.events.task.TaskUpdatedEventPayload;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventDispatcher {

    private final List<KafkaReceiver<String, EventEnvelope<?>>> eventReceivers;
    private final SubscriptionRegistry registry;
    private final List<Disposable> subscriptions = new ArrayList<>();

    @PostConstruct
    public void start() {
        log.info("Starting unified Kafka event dispatcher...");
        for (KafkaReceiver<String, EventEnvelope<?>> receiver : eventReceivers) {
            subscriptions.add(
                    receiver.receive()
                            .flatMap(this::dispatchRecord)
                            .subscribe()
            );
        }
        log.info("Kafka event dispatcher subscriptions started.");
    }

    private Mono<Void> dispatchRecord(ReceiverRecord<String, EventEnvelope<?>> record) {
        EventEnvelope<?> envelope = record.value();
        if (envelope == null) {
            log.warn("Received null EventEnvelope from Kafka record");
            record.receiverOffset().acknowledge();
            return Mono.empty();
        }

        String correlationId = envelope.correlationId() != null ? envelope.correlationId() : "N/A-kafka" + envelope.eventId();
        Object payload = envelope.payload();
        String eventType = envelope.eventType();

        MDC.put("correlationId", correlationId);
        MDC.put("kafkaEventId", envelope.eventId().toString());
        MDC.put("kafkaEventType", eventType);

        log.info("Processing event envelope. Type {}, CorrID: {}", eventType, correlationId);

        // Determine all relevant topics for this payload
        List<String> topics = determineTopics(payload);

        if (topics.isEmpty()) {
            log.warn("No topics determined for event type: {}. CorrID: {}", eventType, correlationId);
            record.receiverOffset().acknowledge();
            clearMdc();
            return Mono.empty();
        }

        // Fan-out to every topic
        return Flux.fromIterable(topics)
                .flatMap(topic -> registry.sendToTopic(topic, envelope))
                .then(doAck(record))
                .doOnSuccess(v -> log.debug("Successfully processed and acknowledged Kafka record. CorrID: {}", correlationId))
                .doOnError(e -> log.error("Error sending WebSocket message for CorrID: {}. Error: {}", correlationId, e.getMessage()))
                .onErrorResume(e -> handleError(record, correlationId, e))
                .doFinally(signal -> clearMdc());
    }

    private Mono<Void> doAck(ReceiverRecord<?, ?> record) {
        record.receiverOffset().acknowledge();
        return Mono.empty();
    }

    private Mono<Void> handleError(ReceiverRecord<?, ?> record, String correlationId, Throwable e) {
        // TODO: Optionally send to dead-letter topic here
        log.error("Unrecoverable error sending WebSocket message for CorrID: {}. Skipping record.", correlationId, e);
        record.receiverOffset().acknowledge();
        clearMdc();
        return Mono.empty();
    }

    private void clearMdc() {
        MDC.remove("correlationId");
        MDC.remove("kafkaEventId");
        MDC.remove("kafkaEventType");
    }

    private List<String> determineTopics(Object payload) {
        if (payload == null) {
            return List.of();
        }

        // Project events
        if (payload instanceof ProjectCreatedEventPayload p) {
            return p.projectDto() != null ? List.of("project:" + p.projectDto().getId()) : List.of();
        } else if (payload instanceof ProjectDeletedEventPayload p) {
            return List.of("project:" + p.projectDto().getId());
        } else if (payload instanceof ProjectUpdatedEventPayload p) {
            return p.projectDto() != null ? List.of("project:" + p.projectDto().getId()) : List.of();
        } else if (payload instanceof ProjectStatusChangedEventPayload p) {
            return p.projectDto() != null ? List.of("project:" + p.projectDto().getId()) : List.of();
        } else if (payload instanceof ProjectTaskCreatedEventPayload p) {
            return List.of("project:" + p.projectId()); // This has projectId directly
        } // Task events - can target both project and task topics
        else if (payload instanceof TaskCreatedEventPayload t) {
            if (t.taskDto() != null) {
                return List.of("project:" + t.taskDto().getProjectId(),
                        "task:" + t.taskDto().getId());
            }
            return List.of();
        } else if (payload instanceof TaskStatusChangedEventPayload t) {
            if (t.taskDto() != null) {
                return List.of("project:" + t.taskDto().getProjectId(),
                        "task:" + t.taskDto().getId());
            }
            return List.of();
        } else if (payload instanceof TaskDeletedEventPayload t) {
            return List.of("project:" + t.taskDto().getProjectId(),
                    "task:" + t.taskDto().getId());
        } else if (payload instanceof TaskUpdatedEventPayload t) {
            if (t.taskDto() != null) {
                return List.of("project:" + t.taskDto().getProjectId(),
                        "task:" + t.taskDto().getId());
            }
            return List.of();
        } else if (payload instanceof TaskPriorityChangedEventPayload t) {
            if (t.dto() != null) {
                return List.of("project:" + t.dto().getProjectId(),
                        "task:" + t.dto().getId());
            }
            return List.of();
        } else if (payload instanceof TaskAssignedEventPayload t) {
            if (t.taskDto() != null) {
                return List.of("project:" + t.taskDto().getProjectId(),
                        "task:" + t.taskDto().getId());
            }
            return List.of();
        } // Notification events
        else if (payload instanceof NotificationToSendEventPayload n) {
            return List.of("user:" + n.notification().getRecipientUserId());
        }

        log.warn("Unknown payload type {}", payload.getClass().getName());
        return List.of();
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping Kafka event dispatcher...");
        subscriptions.forEach(disposable -> {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }
        });
        subscriptions.clear();
        log.info("All Kafka event dispatcher subscriptions stopped.");
    }
}
