package com.pm.websocketservice.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.commoncontracts.envelope.EventEnvelope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionRegistry {

    // Unified topic-based subscriptions: topic -> Set of active WebSocketSessions
    private final Map<String, Set<WebSocketSession>> topicSubscriptions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper; // Inject ObjectMapper for serialization

    // Generic subscription management
    public void addSubscription(String topic, WebSocketSession session) {
        topicSubscriptions.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("Session [{}] subscribed to topic [{}]", session.getId(), topic);
    }

    public void removeSubscription(String topic, WebSocketSession session) {
        topicSubscriptions.computeIfPresent(topic, (k, sessions) -> {
            sessions.remove(session);
            log.debug("Session [{}] explicitly unsubscribed from topic [{}]", session.getId(), topic);
            return sessions.isEmpty() ? null : sessions; // Clean up empty sets
        });
    }

    // Remove session from all subscriptions (on disconnect)
    public void removeSessionFromAll(WebSocketSession session) {
        String sessionId = session.getId();
        topicSubscriptions.forEach((topic, sessions) -> {
            if (sessions.remove(session)) {
                log.debug("Removed closed session [{}] from topic [{}]", sessionId, topic);
            }
        });
        // Clean up map entries where the set became empty
        topicSubscriptions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        log.info("Session [{}] removed from all subscriptions.", sessionId);
    }

    // Unified send method - the core of the new design
    public Mono<Void> sendToTopic(String topic, EventEnvelope<?> envelope) {
        return sendToTopic(topic, envelope, null);
    }

    // Overloaded send method with exclusion capability
    public Mono<Void> sendToTopic(String topic, EventEnvelope<?> envelope, String excludeUserId) {
        Set<WebSocketSession> sessions = topicSubscriptions.getOrDefault(topic, Collections.emptySet());

        if (sessions.isEmpty()) {
            log.debug("No active subscribers found for topic [{}]. Skipping send.", topic);
            return Mono.empty(); // No one to send to
        }

        // If we're excluding a user and this is a user topic for that user, skip entirely
        if (excludeUserId != null && topic.equals("user:" + excludeUserId)) {
            log.debug("Skipping send to topic [{}] because it belongs to excluded user [{}]", topic, excludeUserId);
            return Mono.empty();
        }

        log.debug("Attempting to send event type [{}] for topic [{}] to {} session(s). CorrID: {}{}",
                envelope.eventType(), topic, sessions.size(), envelope.correlationId(),
                excludeUserId != null ? " (excluding user: " + excludeUserId + ")" : "");

        try {
            // Serialize the entire envelope once
            String jsonPayload = objectMapper.writeValueAsString(envelope);

            // Create a list of send Monos for concurrent execution
            List<Mono<Void>> sendMonos = sessions.stream()
                    .filter(WebSocketSession::isOpen) // Send only to open sessions
                    .map(session -> {
                        WebSocketMessage wsMessage = session.textMessage(jsonPayload);
                        log.trace("Sending message to session [{}] for topic [{}]: {}", session.getId(), topic, jsonPayload);
                        return session.send(Mono.just(wsMessage))
                                .doOnError(e -> log.warn("Failed to send message to session [{}] for topic [{}]: {}", session.getId(), topic, e.getMessage()))
                                .onErrorResume(e -> Mono.empty()); // Continue even if one send fails
                    })
                    .toList();

            if (sendMonos.isEmpty()) {
                log.debug("No open sessions found for topic [{}] among subscribers.", topic);
                return Mono.empty();
            }

            // Execute all sends concurrently and wait for completion (or errors)
            return Mono.when(sendMonos)
                    .doOnSuccess(v -> log.debug("Successfully sent messages to relevant sessions for topic [{}]. CorrID: {}", topic, envelope.correlationId()));

        } catch (JsonProcessingException e) {
            // Log serialization error - this is critical
            log.error("CRITICAL: Failed to serialize EventEnvelope for WebSocket broadcast! Topic: {}, CorrID: {}, Error: {}",
                    topic, envelope.correlationId(), e.getMessage(), e);
            return Mono.error(e); // Propagate serialization error
        }
    }

    // Legacy compatibility methods (deprecated - for gradual migration)
    @Deprecated
    public void addUserSession(String userId, WebSocketSession session) {
        addSubscription("user:" + userId, session);
    }

    @Deprecated
    public void removeSessionFromUser(String userId, WebSocketSession session) {
        removeSubscription("user:" + userId, session);
    }

    @Deprecated
    public void removeSessionFromAllUsers(WebSocketSession session) {
        removeSessionFromAll(session);
    }

    @Deprecated
    public Mono<Void> sendToUserSessions(String userId, EventEnvelope<?> envelope) {
        return sendToTopic("user:" + userId, envelope);
    }

    @Deprecated
    public Mono<Void> sendToProjectSubscribers(String projectId, EventEnvelope<?> envelope) {
        return sendToTopic("project:" + projectId, envelope);
    }
}
