package com.pm.websocketservice.controller; // Or handler package

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.websocketservice.service.WebSocketSessionManager; // Import the manager
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component // Make it a Spring bean
@Slf4j
@RequiredArgsConstructor
public class ProjectWebSocketHandler implements WebSocketHandler { // Renamed class

    private final WebSocketSessionManager sessionManager; // Inject the manager
    private final ObjectMapper objectMapper; // Inject ObjectMapper for parsing client messages

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        log.info("WebSocket session established: {}", session.getId());

        // --- Inbound Message Handling (Client -> Server: Subscriptions & User Auth) ---
        Mono<Void> input = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> processClientMessage(session, payload))
                .doOnError(e -> log.error("Error processing inbound message for session [{}]: {}", session.getId(), e.getMessage(), e))
                .doFinally(sig -> log.info("Inbound processing finished for session [{}] with signal: {}", session.getId(), sig))
                .then();

        // --- Session Lifecycle Management ---
        return input.doFinally(signal -> {
            log.info("WebSocket session [{}] closing with signal: {}. Removing from subscriptions and user associations.", session.getId(), signal);
            sessionManager.removeSessionFromAll(session);
            sessionManager.removeSessionFromAllUsers(session);
        });
    }

    /**
     * Processes incoming text messages from the WebSocket client.
     * Expects JSON like: {"type": "subscribe", "projectId": "..."}, {"type": "unsubscribe", "projectId": "..."} or {"type": "user-auth", "userId": "..."}
     */
    private Mono<Void> processClientMessage(WebSocketSession session, String jsonPayload) {
        try {
            // Use TypeReference for generic Map deserialization
            Map<String, String> command = objectMapper.readValue(jsonPayload, new TypeReference<Map<String, String>>() {});
            String type = command.get("type");
            String projectId = command.get("projectId");
            String userId = command.get("userId");

            if ("user-auth".equalsIgnoreCase(type) && userId != null && !userId.isBlank()) {
                log.info("Session [{}] authenticating as user [{}]", session.getId(), userId);
                sessionManager.addUserSession(userId, session);
                return Mono.empty(); // Consume the message
            }

            if (projectId == null || projectId.isBlank()) {
                log.warn("Received command without projectId from session [{}]: {}", session.getId(), jsonPayload);
                return Mono.empty(); // Ignore invalid command
            }

            if ("subscribe".equalsIgnoreCase(type)) {
                log.info("Session [{}] subscribing to project [{}]", session.getId(), projectId);
                sessionManager.addSubscription(projectId, session);
            } else if ("unsubscribe".equalsIgnoreCase(type)) {
                log.info("Session [{}] unsubscribing from project [{}]", session.getId(), projectId);
                sessionManager.removeSubscription(projectId, session);
            } else {
                log.warn("Received unknown command type '{}' from session [{}]: {}", type, session.getId(), jsonPayload);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse command JSON from client [{}]: {}", session.getId(), jsonPayload, e);
            // Optionally send an error message back to the client?
        } catch (Exception e) {
            // Catch other potential errors during processing
            log.error("Error processing command from client [{}]: {}", session.getId(), jsonPayload, e);
        }
        return Mono.empty(); // Consume the message
    }

    // Remove the old broadcast logic, Sink, UniversalMessage etc.
    // The broadcasting is now handled by KafkaEventProcessor -> WebSocketSessionManager
}