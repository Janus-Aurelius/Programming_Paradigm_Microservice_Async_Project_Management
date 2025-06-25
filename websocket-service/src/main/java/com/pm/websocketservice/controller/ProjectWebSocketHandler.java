package com.pm.websocketservice.controller; // Or handler package

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.websocketservice.service.SubscriptionRegistry; // Updated import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
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

    private final SubscriptionRegistry registry; // Updated field name
    private final ObjectMapper objectMapper; // Inject ObjectMapper for parsing client messages

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull WebSocketSession session) {
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
            log.info("WebSocket session [{}] closing with signal: {}. Removing from all subscriptions.", session.getId(), signal);
            registry.removeSessionFromAll(session);
        });
    }

    /**
     * Processes incoming text messages from the WebSocket client. Expects JSON
     * like: {"type": "subscribe", "topic": "project:123"}, {"type":
     * "unsubscribe", "topic": "project:123"} or {"type": "user-auth", "userId":
     * "..."}
     */
    private Mono<Void> processClientMessage(WebSocketSession session, String jsonPayload) {
        try {
            // Use TypeReference for generic Map deserialization
            Map<String, String> command = objectMapper.readValue(jsonPayload, new TypeReference<Map<String, String>>() {
            });
            String type = command.get("type");
            String topic = command.get("topic");
            String userId = command.get("userId");
            String projectId = command.get("projectId"); // Legacy support

            if ("user-auth".equalsIgnoreCase(type) && userId != null && !userId.isBlank()) {
                log.info("Session [{}] authenticating as user [{}]", session.getId(), userId);
                registry.addSubscription("user:" + userId, session);
                return Mono.empty(); // Consume the message
            }

            // Handle legacy projectId format by converting to topic format
            if (topic == null && projectId != null && !projectId.isBlank()) {
                topic = "project:" + projectId;
                log.debug("Converting legacy projectId [{}] to topic [{}] for session [{}]", projectId, topic, session.getId());
            }

            if (topic == null || topic.isBlank()) {
                log.warn("Received command without topic from session [{}]: {}", session.getId(), jsonPayload);
                return Mono.empty(); // Ignore invalid command
            }

            if ("subscribe".equalsIgnoreCase(type)) {
                log.info("Session [{}] subscribing to topic [{}]", session.getId(), topic);
                registry.addSubscription(topic, session);
            } else if ("unsubscribe".equalsIgnoreCase(type)) {
                log.info("Session [{}] unsubscribing from topic [{}]", session.getId(), topic);
                registry.removeSubscription(topic, session);
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
