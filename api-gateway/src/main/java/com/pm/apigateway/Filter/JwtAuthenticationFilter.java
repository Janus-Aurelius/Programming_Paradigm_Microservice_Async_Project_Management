package com.pm.apigateway.Filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.pm.commoncontracts.domain.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "jwt.enabled", havingValue = "true", matchIfMissing = false)
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:my-very-secret-key-for-jwt-signing}")
    private String jwtSecret;

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final java.util.Set<String> revokedUserIds = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    private boolean isUserActive(String userId) {
        return !revokedUserIds.contains(userId);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        log.info("JWT Filter processing request: {} {}", method, path);

        // Allow requests without JWT for login and test endpoints
        if (path.startsWith("/api/users/auth/login")
                || path.startsWith("/api/users/auth/register")
                || path.contains("/test/")
                || path.endsWith("/test")
                || path.startsWith("/test")) {
            log.info("Request allowed without JWT: {} {}", method, path);
            return chain.filter(exchange);
        }

        // Check for service token first
        String serviceToken = request.getHeaders().getFirst("X-Service-Token");
        if (serviceToken != null && !serviceToken.trim().isEmpty()) {
            log.info("Found service token for request: {} {}, token preview: {}...", method, path,
                    serviceToken.length() > 10 ? serviceToken.substring(0, 10) : serviceToken);
            if (validateServiceToken(serviceToken)) {
                log.info("Service token valid, allowing request: {} {}", method, path);
                // Add service identity headers for downstream services
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-Service-Request", "true")
                        .header("X-Service-Name", extractServiceNameFromToken(serviceToken))
                        .build();
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } else {
                log.warn("Invalid service token for request: {} {}", method, path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        } else {
            log.debug("No X-Service-Token header found for request: {} {}", method, path);
        }

        String token = null;

        // Handle WebSocket authentication via query parameter
        if (path.startsWith("/ws/")) {
            token = request.getQueryParams().getFirst("token");
            if (token == null || token.trim().isEmpty()) {
                log.warn("Missing token query parameter for WebSocket path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            log.info("Using token from query parameter for WebSocket authentication");
        } else {
            // Handle regular HTTP requests via Authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header for path: {}", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            token = authHeader.substring(7);
        }
        Claims claims;

        try {
            SecretKey key = getSigningKey();
            claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.getSubject();
            String userRole = (String) claims.get("role"); // Single role as string

            log.info("JWT claims - userId: {}, role: {}", userId, userRole);

            if (!isUserActive(userId)) {
                log.warn("Rejected request for revoked or deleted user: {}", userId);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Role-based access control
            final String requiredRole;
            if (path.startsWith("/api/admin")) {
                requiredRole = UserRole.ROLE_ADMIN.getRole();
            } else if (path.startsWith("/api/manager")) {
                requiredRole = UserRole.ROLE_PROJECT_MANAGER.getRole();
            } else {
                requiredRole = null;
            }

            if (requiredRole != null) {
                if (userRole == null || !userRole.equals(requiredRole)) {
                    log.warn("Access denied for user {} with role {} to path {}. Required role: {}",
                            userId, userRole, path, requiredRole);
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
            }
            log.info("User {} with role {} accessed {}", userId, userRole, path);

            // Forward user info to downstream services
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", (String) claims.get("email"))
                    .header("X-User-Role", userRole != null ? userRole : "ROLE_USER")
                    .build();

            log.info("Forwarding headers to downstream: X-User-Id={}, X-User-Email={}, X-User-Role={}",
                    userId, claims.get("email"), userRole != null ? userRole : "ROLE_USER");

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        } catch (io.jsonwebtoken.JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        } catch (Exception e) {
            log.error("Unexpected error during JWT processing: {}", e.getMessage(), e);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private SecretKey getSigningKey() {
        String secret = jwtSecret;
        if (secret.getBytes().length < 32) {
            log.warn("JWT secret key is less than 32 bytes, which is not recommended for HS256. Consider using a stronger key.");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private boolean validateServiceToken(String serviceToken) {
        try {
            // Decode the service token
            String decoded = new String(Base64.getDecoder().decode(serviceToken), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");

            if (parts.length != 3) {
                log.warn("Invalid service token format");
                return false;
            }

            String serviceName = parts[0];
            long timestamp = Long.parseLong(parts[1]);
            String signature = parts[2];

            // Check if token is not expired (valid for 1 minute)
            long currentMinute = System.currentTimeMillis() / 60000;
            if (Math.abs(currentMinute - timestamp) > 1) {
                log.warn("Service token expired for service: {}", serviceName);
                return false;
            }

            // Verify signature
            String payload = serviceName + ":" + timestamp;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String toHash = payload + ":" + jwtSecret;
            byte[] hash = md.digest(toHash.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            boolean isValid = expectedSignature.equals(signature);
            if (isValid) {
                log.info("Service token validated successfully for service: {}", serviceName);
            } else {
                log.warn("Service token signature validation failed for service: {}", serviceName);
            }

            return isValid;
        } catch (NumberFormatException e) {
            log.error("Error parsing timestamp in service token: {}", e.getMessage());
            return false;
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error validating service token: {}", e.getMessage());
            return false;
        }
    }

    private String extractServiceNameFromToken(String serviceToken) {
        try {
            String decoded = new String(Base64.getDecoder().decode(serviceToken), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            return parts.length > 0 ? parts[0] : "unknown";
        } catch (Exception e) {
            log.error("Error extracting service name from token: {}", e.getMessage());
            return "unknown";
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
