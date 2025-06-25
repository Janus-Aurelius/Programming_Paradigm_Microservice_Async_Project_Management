package com.pm.userservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Development Security Configuration - Disables JWT authentication by default
 * This is now the default behavior unless JWT is explicitly enabled Activated
 * when jwt.enabled=false or not set (default)
 */
@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(name = "jwt.enabled", havingValue = "false", matchIfMissing = true)
public class DevSecurityConfig {

    @Bean
    @Primary
    public SecurityWebFilterChain devSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll() // Allow all requests without authentication
                )
                .build();
    }    // Password encoder removed - using plaintext passwords
}
