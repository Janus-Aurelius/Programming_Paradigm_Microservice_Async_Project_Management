package com.pm.apigateway.Filter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:defaultsecret}")
    private String jwtSecret;

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    // Simulated in-memory revoked user list (replace with Redis or DB in production)
    private static final java.util.Set<String> revokedUserIds = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    // Simulated user existence check (replace with real call to user-service in production)
    private boolean isUserActive(String userId) {
        // TODO: Replace with actual user-service call
        return !revokedUserIds.contains(userId);
    }

    // For demo: method to revoke a user (call this on password change, delete, etc.)
    public static void revokeUser(String userId) {
        revokedUserIds.add(userId);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        // Allow login and registration endpoints without JWT
        if (path.startsWith("/api/users/login") || (path.startsWith("/api/users") && request.getMethod() != null && request.getMethod().name().equals("POST"))) {
            return chain.filter(exchange);
        }
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        String token = authHeader.substring(7);
        try {
            SecretKey key = getSigningKey();
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String userId = claims.getSubject();
            String role = (String) claims.get("role");
            // JWT Expiry is checked by parser; add user existence/revocation check
            if (!isUserActive(userId)) {
                log.warn("Rejected request for revoked or deleted user: {}", userId);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String requiredRole = null;
            if (path.startsWith("/api/admin")) {
                requiredRole = "ADMIN";
            } else if (path.startsWith("/api/manager")) {
                requiredRole = "PROJECT_MANAGER";
            }

            if (requiredRole != null && (role == null || !role.equals(requiredRole))) {
                log.warn("Access denied for user {} with role {} to path: {}", userId, role, path);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // Audit log for access
            log.info("User {} with role {} accessed {}", userId, role, path);
            // Optionally, add user info to headers for downstream services
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", (String) claims.get("email"))
                    .header("X-User-Role", role)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private SecretKey getSigningKey() {
        // Use the same secret as user-service JwtConfig
        byte[] keyBytes = Base64.getEncoder().encode(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
    }

    @Override
    public int getOrder() {
        return -1; // High precedence
    }
}
