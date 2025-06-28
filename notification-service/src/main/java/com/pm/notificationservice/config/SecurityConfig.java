package com.pm.notificationservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.pm.notificationservice.security.NotificationPermissionEvaluator;

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Configuration
@ConditionalOnProperty(name = "jwt.enabled", havingValue = "true", matchIfMissing = false)
public class SecurityConfig {

    private final NotificationPermissionEvaluator notificationPermissionEvaluator;
    private final HeaderBasedSecurityContextRepository securityContextRepository;

    public SecurityConfig(NotificationPermissionEvaluator notificationPermissionEvaluator,
            HeaderBasedSecurityContextRepository securityContextRepository) {
        this.notificationPermissionEvaluator = notificationPermissionEvaluator;
        this.securityContextRepository = securityContextRepository;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .securityContextRepository(securityContextRepository)
                .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**", "/health/**").permitAll()
                .anyExchange().authenticated()
                );
        return http.build();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(notificationPermissionEvaluator);
        return expressionHandler;
    }
}
