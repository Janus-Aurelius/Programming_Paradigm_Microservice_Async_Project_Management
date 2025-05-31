package com.pm.websocketservice.service; // Adjust package as needed

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
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketSessionManager {

    // Map: ProjectID -> Set of active WebSocketSessions subscribed to that project
    private final Map<String, Set<WebSocketSession>> projectSubscriptions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper; // Inject ObjectMapper for serialization

    // Map: userId -> Set of active WebSocketSessions for that user
    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    // Called by WebSocketHandler when a client subscribes
    public void addSubscription(String projectId, WebSocketSession session) {
        projectSubscriptions.computeIfAbsent(projectId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("Session [{}] subscribed to project [{}]", session.getId(), projectId);
    }

    // Called by WebSocketHandler when a client unsubscribes
    public void removeSubscription(String projectId, WebSocketSession session) {
        projectSubscriptions.computeIfPresent(projectId, (k, sessions) -> {
            sessions.remove(session);
            log.debug("Session [{}] explicitly unsubscribed from project [{}]", session.getId(), projectId);
            return sessions.isEmpty() ? null : sessions; // Clean up empty sets
        });
    }

    // Called by WebSocketHandler when a session closes/errors out
    public void removeSessionFromAll(WebSocketSession session) {
        String sessionId = session.getId();
        projectSubscriptions.forEach((projectId, sessions) -> {
            if (sessions.remove(session)) {
                log.debug("Removed closed session [{}] from project [{}] subscription", sessionId, projectId);
            }
        });
        // Clean up map entries where the set became empty
        projectSubscriptions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        log.info("Session [{}] removed from all subscriptions.", sessionId);
    }

    // Called by WebSocketHandler when a client authenticates (or subscribes for user notifications)
    public void addUserSession(String userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("Session [{}] associated with user [{}]", session.getId(), userId);
    }

    // Called by WebSocketHandler when a session closes/errors out
    public void removeSessionFromUser(String userId, WebSocketSession session) {
        userSessions.computeIfPresent(userId, (k, sessions) -> {
            sessions.remove(session);
            log.debug("Session [{}] removed from user [{}]", session.getId(), userId);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    // Remove session from all user associations (on disconnect)
    public void removeSessionFromAllUsers(WebSocketSession session) {
        String sessionId = session.getId();
        userSessions.forEach((userId, sessions) -> {
            if (sessions.remove(session)) {
                log.debug("Removed closed session [{}] from user [{}]", sessionId, userId);
            }
        });
        userSessions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    // Send notification to all sessions for a user
    public Mono<Void> sendToUserSessions(String userId, EventEnvelope<?> envelope) {
        Set<WebSocketSession> sessions = userSessions.getOrDefault(userId, Collections.emptySet());
        if (sessions.isEmpty()) {
            log.debug("No active sessions found for user [{}]. Skipping send.", userId);
            return Mono.empty();
        }
        try {
            String jsonPayload = objectMapper.writeValueAsString(envelope);
            List<Mono<Void>> sendMonos = sessions.stream()
                    .filter(WebSocketSession::isOpen)
                    .map(session -> {
                        WebSocketMessage wsMessage = session.textMessage(jsonPayload);
                        log.trace("Sending user notification to session [{}]: {}", session.getId(), jsonPayload);
                        return session.send(Mono.just(wsMessage))
                                .doOnError(e -> log.warn("Failed to send user notification to session [{}]: {}", session.getId(), e.getMessage()))
                                .onErrorResume(e -> Mono.empty());
                    })
                    .toList();
            if (sendMonos.isEmpty()) return Mono.empty();
            return Mono.when(sendMonos)
                    .doOnSuccess(v -> log.debug("Successfully sent user notification to user [{}]", userId));
        } catch (JsonProcessingException e) {
            log.error("CRITICAL: Failed to serialize EventEnvelope for user notification! CorrID: {}, Error: {}", envelope.correlationId(), e.getMessage(), e);
            return Mono.error(e);
        }
    }

    // Called by KafkaEventProcessor to send events
    public Mono<Void> sendToProjectSubscribers(String projectId, EventEnvelope<?> envelope) {
        Set<WebSocketSession> sessions = projectSubscriptions.getOrDefault(projectId, Collections.emptySet());

        if (sessions.isEmpty()) {
            log.debug("No active subscribers found for project [{}]. Skipping send.", projectId);
            return Mono.empty(); // No one to send to
        }

        log.debug("Attempting to send event type [{}] for project [{}] to {} session(s). CorrID: {}",
                envelope.eventType(), projectId, sessions.size(), envelope.correlationId());

        try {
            // Serialize the entire envelope once
            String jsonPayload = objectMapper.writeValueAsString(envelope);

            // Create a list of send Monos for concurrent execution
            List<Mono<Void>> sendMonos = sessions.stream()
                    .filter(WebSocketSession::isOpen) // Send only to open sessions
                    .map(session -> {
                        WebSocketMessage wsMessage = session.textMessage(jsonPayload);
                        log.trace("Sending message to session [{}]: {}", session.getId(), jsonPayload); // Trace level for payload
                        return session.send(Mono.just(wsMessage))
                                .doOnError(e -> log.warn("Failed to send message to session [{}]: {}", session.getId(), e.getMessage()))
                                .onErrorResume(e -> Mono.empty()); // Continue even if one send fails
                    })
                    .toList();

            if (sendMonos.isEmpty()) {
                log.debug("No open sessions found for project [{}] among subscribers.", projectId);
                return Mono.empty();
            }

            // Execute all sends concurrently and wait for completion (or errors)
            return Mono.when(sendMonos)
                    .doOnSuccess(v -> log.debug("Successfully sent messages to relevant sessions for project [{}]. CorrID: {}", projectId, envelope.correlationId()));

        } catch (JsonProcessingException e) {
            // Log serialization error - this is critical
            log.error("CRITICAL: Failed to serialize EventEnvelope for WebSocket broadcast! CorrID: {}, Error: {}",
                    envelope.correlationId(), e.getMessage(), e);
            return Mono.error(e); // Propagate serialization error
        }
    }
}