package com.pm.apigateway.Filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Development JWT Filter - Bypasses JWT validation and forwards mock user
 * headers This is now the default behavior unless JWT is explicitly enabled
 * Activated when jwt.enabled=false or not set (default)
 */
@Component
@ConditionalOnProperty(name = "jwt.enabled", havingValue = "false", matchIfMissing = true)
public class DevJwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DevJwtAuthenticationFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        log.info("DEV MODE: JWT Filter bypassed for request: {} {}", method, path);

        // Forward mock user info to downstream services for development
        ServerHttpRequest.Builder mutatedRequestBuilder = exchange.getRequest().mutate()
                .header("X-User-Id", "dev-user-123")
                .header("X-User-Email", "dev@example.com")
                .header("X-User-Role", "ROLE_ADMIN"); // Give admin role for development convenience

        // If the original request contains a ?token=<jwt>, forward it as Authorization header
        String jwtToken = request.getQueryParams().getFirst("token");
        if (jwtToken != null && !jwtToken.isBlank()) {
            mutatedRequestBuilder.header("Authorization", "Bearer " + jwtToken);
            log.debug("DEV MODE: Forwarding Authorization header with JWT token");
        }

        ServerHttpRequest mutatedRequest = mutatedRequestBuilder.build();

        log.info("DEV MODE: Forwarding mock headers (and Authorization if present) to downstream.");

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -2; // Higher priority than regular JWT filter
    }
}
