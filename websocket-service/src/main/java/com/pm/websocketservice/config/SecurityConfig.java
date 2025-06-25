package com.pm.websocketservice.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter;

import com.pm.websocketservice.security.WebSocketPermissionEvaluator;

import reactor.core.publisher.Mono;

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Configuration
public class SecurityConfig {

    private final WebSocketPermissionEvaluator webSocketPermissionEvaluator;

    public SecurityConfig(WebSocketPermissionEvaluator webSocketPermissionEvaluator) {
        this.webSocketPermissionEvaluator = webSocketPermissionEvaluator;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**").permitAll()
                .anyExchange().permitAll() // Allow all for development - TODO: Configure proper auth for production
                );
        return http.build();
    }

    @Bean
    public WebFilter userIdHeaderWebFilter() {
        return (exchange, chain) -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            String email = exchange.getRequest().getHeaders().getFirst("X-User-Email");
            String role = exchange.getRequest().getHeaders().getFirst("X-User-Role");

            // For development, provide default values if headers are missing
            if (userId == null) {
                userId = "dev-user-123";
                email = "dev@example.com";
                role = "ROLE_ADMIN";
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, role != null ? List.of(new SimpleGrantedAuthority(role)) : List.of()
            );
            auth.setDetails(email);

            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(auth))));
        };
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(webSocketPermissionEvaluator);
        return expressionHandler;
    }
}
