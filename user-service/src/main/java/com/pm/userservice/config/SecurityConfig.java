package com.pm.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {    private final ServerSecurityContextRepository securityContextRepository;

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
                .pathMatchers("/admin/**").hasRole("ADMIN") // Example role-based access
                .anyExchange().authenticated()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
