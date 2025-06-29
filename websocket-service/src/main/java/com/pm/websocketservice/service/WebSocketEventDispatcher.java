package com.pm.websocketservice.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.pm.commoncontracts.envelope.EventEnvelope;
import com.pm.commoncontracts.events.notification.NotificationToSendEventPayload;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

/**
 * WebSocket Event Dispatcher for real-time notifications.
 *
 * This service is responsible ONLY for broadcasting
 * NotificationToSendEventPayload events to connected WebSocket clients. All
 * domain events (project, task, comment, etc.) should be processed by the
 * notification service first, which creates unified notifications before they
 * reach this dispatcher.
 *
 * Architecture Flow: 1. Domain services emit domain events
 * (CommentDeletedEventPayload, TaskCreatedEventPayload, etc.) 2. Notification
 * service consumes domain events and creates NotificationToSendEventPayload 3.
 * WebSocketEventDispatcher broadcasts NotificationToSendEventPayload to
 * relevant users
 */
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
        String recordKey = record.key(); // This could be the target WebSocket topic

        MDC.put("correlationId", correlationId);
        MDC.put("kafkaEventId", envelope.eventId().toString());
        MDC.put("kafkaEventType", eventType);

        log.info("Processing event envelope. Type {}, Key: {}, CorrID: {}", eventType, recordKey, correlationId);

        // Determine all relevant topics for this payload
        List<String> topics;

        // If the record key looks like a WebSocket topic (e.g., "project:123", "task:456", "user:789"), use it directly
        if (recordKey != null && (recordKey.startsWith("project:") || recordKey.startsWith("task:") || recordKey.startsWith("user:"))) {
            topics = List.of(recordKey);
            log.debug("Using Kafka record key as WebSocket topic: {}", recordKey);
        } else {
            // Fallback to determining topics from payload
            topics = determineTopics(payload);
        }

        if (topics.isEmpty()) {
            log.warn("No topics determined for event type: {}. CorrID: {}", eventType, correlationId);
            record.receiverOffset().acknowledge();
            clearMdc();
            return Mono.empty();
        }

        // Fan-out to every topic
        return Flux.fromIterable(topics)
                .flatMap(topic -> {
                    // If we have fallback deserialization, we need to create a proper envelope
                    EventEnvelope<?> envelopeToSend = envelope;
                    if (payload instanceof java.util.Map<?, ?> && !(payload instanceof NotificationToSendEventPayload)) {
                        // Create a new envelope with the original data but ensure proper structure for WebSocket
                        envelopeToSend = new EventEnvelope<>(
                                envelope.eventId(),
                                envelope.correlationId(),
                                envelope.eventType(),
                                envelope.sourceService(),
                                envelope.timestamp(),
                                envelope.version(),
                                payload // Keep the map structure for now
                        );
                    }
                    return registry.sendToTopic(topic, envelopeToSend);
                })
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

        // Handle notification events - route to user-specific topics
        if (payload instanceof NotificationToSendEventPayload n) {
            return List.of("user:" + n.notification().getRecipientUserId());
        }

        // Handle comment events from websocket-dispatch topic - these come with topic information
        if (payload instanceof com.pm.commoncontracts.events.comment.CommentAddedEventPayload commentAdded) {
            return getCommentEventTopics(commentAdded.commentDto());
        }

        if (payload instanceof com.pm.commoncontracts.events.comment.CommentEditedEventPayload commentEdited) {
            return getCommentEventTopics(commentEdited.commentDto());
        }

        if (payload instanceof com.pm.commoncontracts.events.comment.CommentDeletedEventPayload commentDeleted) {
            return getCommentEventTopics(commentDeleted.commentDto());
        }

        // Fallback handling for deserialization issues
        if (payload instanceof java.util.Map<?, ?> payloadMap) {
            // Try to extract notification data from the payload map
            Object notificationObj = payloadMap.get("notification");
            if (notificationObj instanceof java.util.Map<?, ?> notificationMap) {
                Object recipientUserId = notificationMap.get("recipientUserId");
                if (recipientUserId instanceof String userId) {
                    log.info("Using fallback deserialization for NotificationToSendEventPayload. Recipient: {}", userId);
                    return List.of("user:" + userId);
                }
            }

            // Try to extract comment data from the payload map
            Object commentObj = payloadMap.get("commentDto");
            if (commentObj instanceof java.util.Map<?, ?> commentMap) {
                return getCommentEventTopicsFromMap(commentMap);
            }
        }

        log.warn("WebSocketEventDispatcher received unsupported payload type: {}. All domain events should be processed by notification service first.", payload.getClass().getName());
        return List.of();
    }

    /**
     * Helper method to determine topics for comment events
     */
    private List<String> getCommentEventTopics(com.pm.commoncontracts.dto.CommentDto commentDto) {
        List<String> topics = new ArrayList<>();

        if (commentDto.getParentType() != null && commentDto.getParentId() != null) {
            String parentType = commentDto.getParentType().name().toLowerCase();
            String parentId = commentDto.getParentId();

            // Add the main parent topic (project:<id> or task:<id>)
            topics.add(parentType + ":" + parentId);

            log.debug("Comment event will be sent to topic: {}:{}", parentType, parentId);
        }

        return topics;
    }

    /**
     * Helper method to determine topics for comment events from fallback map
     * deserialization
     */
    private List<String> getCommentEventTopicsFromMap(java.util.Map<?, ?> commentMap) {
        List<String> topics = new ArrayList<>();

        Object parentType = commentMap.get("parentType");
        Object parentId = commentMap.get("parentId");

        if (parentType instanceof String pType && parentId instanceof String pId) {
            String parentTypeLower = pType.toLowerCase();
            topics.add(parentTypeLower + ":" + pId);

            log.debug("Comment event (fallback) will be sent to topic: {}:{}", parentTypeLower, pId);
        }

        return topics;
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
