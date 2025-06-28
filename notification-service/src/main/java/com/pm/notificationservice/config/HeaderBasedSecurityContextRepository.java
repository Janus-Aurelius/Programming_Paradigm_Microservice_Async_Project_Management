package com.pm.notificationservice.config;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Security context repository that reads user information from headers
 * forwarded by the API Gateway for notification service.
 */
@Component
@ConditionalOnProperty(name = "jwt.enabled", havingValue = "true", matchIfMissing = false)
public class HeaderBasedSecurityContextRepository implements ServerSecurityContextRepository {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        // Stateless: nothing to save
        return Mono.empty();
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        String userId = request.getHeaders().getFirst(USER_ID_HEADER);
        String userEmail = request.getHeaders().getFirst(USER_EMAIL_HEADER);
        String userRole = request.getHeaders().getFirst(USER_ROLE_HEADER);

        System.out.println("DEBUG: Headers - UserId: " + userId + ", Email: " + userEmail + ", Role: " + userRole);

        if (userId != null && userEmail != null && userRole != null) {
            // Ensure the role has the ROLE_ prefix
            String formattedRole = userRole.startsWith("ROLE_") ? userRole : "ROLE_" + userRole;

            System.out.println("DEBUG: Formatted role: " + formattedRole);

            // Create authentication with the role as authority
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(formattedRole));

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities
            );

            // Add additional user info as details
            auth.setDetails(new UserAuthenticationDetails(userId, userEmail, userRole));

            return Mono.just(new SecurityContextImpl(auth));
        }

        return Mono.empty();
    }

    /**
     * Additional user authentication details
     */
    public static class UserAuthenticationDetails {

        private final String userId;
        private final String email;
        private final String role;

        public UserAuthenticationDetails(String userId, String email, String role) {
            this.userId = userId;
            this.email = email;
            this.role = role;
        }

        public String getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
        }
    }
}
