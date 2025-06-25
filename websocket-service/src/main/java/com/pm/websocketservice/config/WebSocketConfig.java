package com.pm.websocketservice.config;

import java.util.HashMap; // Import the new handler
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import com.pm.websocketservice.controller.ProjectWebSocketHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    // Inject the specific handler bean
    private final ProjectWebSocketHandler projectWebSocketHandler;

    @Bean
    public HandlerMapping webSocketHandlerMapping() { // Renamed bean method for clarity
        Map<String, WebSocketHandler> map = new HashMap<>();
        // Map the URL path to your specific handler instance
        // Gateway sends /ws/updates to this service
        map.put("/ws/updates", projectWebSocketHandler);
        // Also map /updates in case the path is different
        map.put("/updates", projectWebSocketHandler);

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(1); // Ensure high priority
        handlerMapping.setUrlMap(map);
        return handlerMapping;
    }

    // WebSocketHandlerAdapter is needed to process WebSocket requests
    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
