package com.pm.websocketservice.config;

import com.pm.websocketservice.controller.ProjectWebSocketHandler; // Import the new handler
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    // Inject the specific handler bean
    private final ProjectWebSocketHandler projectWebSocketHandler;

    @Bean
    public HandlerMapping webSocketHandlerMapping() { // Renamed bean method for clarity
        Map<String, WebSocketHandler> map = new HashMap<>();
        // Map the URL path to your specific handler instance
        map.put("/ws/updates", projectWebSocketHandler);

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