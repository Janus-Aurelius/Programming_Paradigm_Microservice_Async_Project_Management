package com.pm.userservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;

import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;

import com.pm.userservice.security.UserPermissionEvaluator;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@ConditionalOnProperty(name = "jwt.enabled", havingValue = "true", matchIfMissing = false)
public class SecurityConfig {

    private final ServerSecurityContextRepository securityContextRepository;

    public SecurityConfig(ServerSecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .securityContextRepository(securityContextRepository)
                .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/auth/login", "/auth/test-login", "/test/**", "/api/users/auth/login", "/api/users/auth/test-login", "/api/users/test/**", "/", "").permitAll()
                .anyExchange().authenticated()
                );
        return http.build();
    }    // Password encoder removed - using plaintext passwords

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            UserPermissionEvaluator userPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(userPermissionEvaluator);
        return expressionHandler;
    }
}
