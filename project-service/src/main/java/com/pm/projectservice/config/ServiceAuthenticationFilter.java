package com.pm.projectservice.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ServiceAuthenticationFilter implements WebFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String serviceToken = exchange.getRequest().getHeaders().getFirst("X-Service-Token");

        if (serviceToken != null && isValidServiceToken(serviceToken)) {
            // Create a service authentication
            UsernamePasswordAuthenticationToken authentication
                    = new UsernamePasswordAuthenticationToken(
                            "notification-service",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
                    );
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
        }

        // Continue with normal filter chain
        return chain.filter(exchange);
    }

    private boolean isValidServiceToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 3) {
                return false;
            }
            String serviceName = parts[0];
            long timestamp = Long.parseLong(parts[1]);
            String signature = parts[2];

            // Check if token is not too old (within 2 minutes)
            long currentMinute = System.currentTimeMillis() / 60000;
            if (Math.abs(currentMinute - timestamp) > 2) {
                return false;
            }

            // Verify signature
            String payload = serviceName + ":" + timestamp;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String toHash = payload + ":" + jwtSecret;
            byte[] hash = md.digest(toHash.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            return signature.equals(expectedSignature) && "notification-service".equals(serviceName);
        } catch (Exception e) {
            return false;
        }
    }
}
