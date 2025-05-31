package com.pm.websocketservice.service;

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
public class KafkaEventListenAndProcess {
    private final KafkaReceiver<String, EventEnvelope<?>> projectEventsReceiver;
    private final KafkaReceiver<String, EventEnvelope<?>> taskEventsReceiver;
    private final KafkaReceiver<String, EventEnvelope<?>> notificationEventsReceiver;
    private final WebSocketSessionManager sessionManager;

    private Disposable projectSubscriptionDisposable;
    private Disposable taskSubscriptionDisposable;
    private Disposable notificationSubscriptionDisposable;

    @PostConstruct
    public void startConsuming() {
        log.info("Starting Kafka consumers for project and task events...");
        this.projectSubscriptionDisposable = processEvents(projectEventsReceiver.receive(), "Project");
        this.taskSubscriptionDisposable = processEvents(taskEventsReceiver.receive(), "Task");
        this.notificationSubscriptionDisposable = processNotificationEvents();
        log.info("Kafka Consumer subscriptions started.");
    }

    private Disposable processEvents(Flux<ReceiverRecord<String, EventEnvelope<?>>> receiverFlux, String eventTypeLabel) {
        return receiverFlux
                .flatMap(record -> {
                    EventEnvelope<?> envelope = record.value();
                    if (envelope == null) {
                        log.warn("Received null EventEnvelope from Kafka record");
                        // TODO: Optionally send to dead-letter topic here
                        record.receiverOffset().acknowledge();
                        return Mono.empty();
                    }

                    String correlationId = envelope.correlationId() != null ? envelope.correlationId() : "N/A-kafka" + envelope.eventId();
                    Object payload = envelope.payload();
                    String eventType = envelope.eventType();

                    MDC.put(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, correlationId);
                    MDC.put("kafkaEventId", envelope.eventId().toString());
                    MDC.put("kafkaEventType", eventType);

                    log.info("Processing {} event envelope. Type {}, CorrID: {}", eventTypeLabel, eventType, correlationId);

                    String targetProjectId = determineProjectId(payload);

                    if (targetProjectId == null) {
                        log.warn("Cannot determine target project ID for event type: {}. CorrID: {}", eventType, correlationId);
                        record.receiverOffset().acknowledge();
                        clearMdc();
                        return Mono.empty();
                    }

                    return sessionManager.sendToProjectSubscribers(targetProjectId, envelope)
                            .doOnSuccess(v -> {
                                record.receiverOffset().acknowledge();
                                log.debug("Successfully processed and acknowledged Kafka record. CorrID: {}", correlationId);
                            })
                            .doOnError(e -> log.error("Error sending WebSocket message for CorrID: {}. Error: {}",
                                    correlationId, e.getMessage()))
                            .onErrorResume(e -> {
                                // TODO: Optionally send to dead-letter topic here
                                log.error("Unrecoverable error sending WebSocket message for CorrID: {}. Skipping record.", correlationId, e);
                                record.receiverOffset().acknowledge();
                                clearMdc();
                                return Mono.empty();
                            })
                            .doFinally(signal -> clearMdc());
                })
                .onErrorContinue((throwable, recordObject) -> {
                    log.error("Unhandled error processing Kafka stream, skipping record", throwable);
                    if (recordObject instanceof ReceiverRecord<?, ?> record) {
                        // TODO: Optionally send to dead-letter topic here
                        record.receiverOffset().acknowledge();
                        log.warn("Acknowledged potentially problematic record to prevent reprocessing loop");
                    }
                    clearMdc();
                })
                .subscribe();
    }

    private Disposable processNotificationEvents() {
        return notificationEventsReceiver.receive()
            .flatMap(record -> {
                EventEnvelope<?> envelope = record.value();
                if (envelope == null) {
                    log.warn("Received null EventEnvelope from Kafka record (notification)");
                    record.receiverOffset().acknowledge();
                    return Mono.empty();
                }
                String correlationId = envelope.correlationId() != null ? envelope.correlationId() : "N/A-kafka" + envelope.eventId();
                Object payload = envelope.payload();
                String eventType = envelope.eventType();
                MDC.put(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, correlationId);
                MDC.put("kafkaEventId", envelope.eventId().toString());
                MDC.put("kafkaEventType", eventType);
                log.info("Processing Notification event envelope. Type {}, CorrID: {}", eventType, correlationId);
                if (payload instanceof NotificationToSendEventPayload notification) {
                    String recipientId = notification.notification().getRecipientUserId();
                    return sessionManager.sendToUserSessions(recipientId, envelope)
                        .doOnSuccess(v -> {
                            record.receiverOffset().acknowledge();
                            log.debug("Notification sent to user [{}] and acknowledged. CorrID: {}", recipientId, correlationId);
                        })
                        .doOnError(e -> log.error("Error sending notification to user [{}]. CorrID: {}. Error: {}", recipientId, correlationId, e.getMessage()))
                        .onErrorResume(e -> {
                            // TODO: Optionally send to dead-letter topic here
                            log.error("Unrecoverable error sending notification to user [{}]. CorrID: {}. Skipping record.", recipientId, correlationId, e);
                            record.receiverOffset().acknowledge();
                            clearMdc();
                            return Mono.empty();
                        })
                        .doFinally(signal -> clearMdc());
                } else {
                    log.warn("Notification event payload is not of expected type. CorrID: {}", correlationId);
                    record.receiverOffset().acknowledge();
                    clearMdc();
                    return Mono.empty();
                }
            })
            .onErrorContinue((throwable, recordObject) -> {
                log.error("Unhandled error processing notification Kafka stream, skipping record", throwable);
                if (recordObject instanceof ReceiverRecord<?, ?> record) {
                    // TODO: Optionally send to dead-letter topic here
                    record.receiverOffset().acknowledge();
                    log.warn("Acknowledged problematic notification record to prevent reprocessing loop");
                }
                clearMdc();
            })
            .subscribe();
    }

    private void clearMdc() {
        MDC.remove(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY);
        MDC.remove("kafkaEventId");
        MDC.remove("kafkaEventType");
    }    private String determineProjectId(Object eventPayload) {
        if (eventPayload == null) return null;

        // Project events
        if (eventPayload instanceof ProjectCreatedEventPayload) {
            ProjectCreatedEventPayload p = (ProjectCreatedEventPayload) eventPayload;
            return p.projectDto() != null ? p.projectDto().getId() : null;
        } else if (eventPayload instanceof ProjectDeletedEventPayload) {
            ProjectDeletedEventPayload p = (ProjectDeletedEventPayload) eventPayload;
            return p.projectDto().getId();
        } else if (eventPayload instanceof ProjectUpdatedEventPayload) {
            ProjectUpdatedEventPayload p = (ProjectUpdatedEventPayload) eventPayload;
            return p.projectDto() != null ? p.projectDto().getId() : null;
        } else if (eventPayload instanceof ProjectStatusChangedEventPayload) {
            ProjectStatusChangedEventPayload p = (ProjectStatusChangedEventPayload) eventPayload;
            return p.projectDto() != null ? p.projectDto().getId() : null;
        } else if (eventPayload instanceof ProjectTaskCreatedEventPayload) {
            ProjectTaskCreatedEventPayload p = (ProjectTaskCreatedEventPayload) eventPayload;
            return p.projectId(); // This has projectId directly
        }
        
        // Task events
        else if (eventPayload instanceof TaskCreatedEventPayload) {
            TaskCreatedEventPayload t = (TaskCreatedEventPayload) eventPayload;
            return t.taskDto() != null ? t.taskDto().getProjectId() : null;
        } else if (eventPayload instanceof TaskStatusChangedEventPayload) {
            TaskStatusChangedEventPayload t = (TaskStatusChangedEventPayload) eventPayload;
            return t.taskDto() != null ? t.taskDto().getProjectId() : null;
        } else if (eventPayload instanceof TaskDeletedEventPayload) {
            TaskDeletedEventPayload t = (TaskDeletedEventPayload) eventPayload;
            return t.taskDto().getProjectId();
        } else if (eventPayload instanceof TaskUpdatedEventPayload) {
            TaskUpdatedEventPayload t = (TaskUpdatedEventPayload) eventPayload;
            return t.taskDto() != null ? t.taskDto().getProjectId() : null;
        } else if (eventPayload instanceof TaskPriorityChangedEventPayload) {
            TaskPriorityChangedEventPayload t = (TaskPriorityChangedEventPayload) eventPayload;
            return t.dto() != null ? t.dto().getProjectId() : null;
        } else if (eventPayload instanceof TaskAssignedEventPayload) {
            TaskAssignedEventPayload t = (TaskAssignedEventPayload) eventPayload;
            return t.taskDto() != null ? t.taskDto().getProjectId() : null;
        }

        log.warn("Unknown event payload type encountered: {}", eventPayload.getClass().getName());
        return null;
    }

    @PreDestroy
    public void stopConsuming() {
        log.info("Stopping Kafka Consumers...");
        if (this.projectSubscriptionDisposable != null && !this.projectSubscriptionDisposable.isDisposed()) {
            this.projectSubscriptionDisposable.dispose();
            log.info("Project events consumer stopped.");
        }
        if (this.taskSubscriptionDisposable != null && !this.taskSubscriptionDisposable.isDisposed()) {
            this.taskSubscriptionDisposable.dispose();
            log.info("Task events consumer stopped.");
        }
        if (this.notificationSubscriptionDisposable != null && !this.notificationSubscriptionDisposable.isDisposed()) {
            this.notificationSubscriptionDisposable.dispose();
            log.info("Notification events consumer stopped.");
        }
        log.info("All Kafka Consumers stopped.");
    }
}