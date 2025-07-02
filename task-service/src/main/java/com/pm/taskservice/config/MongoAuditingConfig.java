package com.pm.taskservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;

import reactor.core.publisher.Mono;

@Configuration
@EnableReactiveMongoAuditing
public class MongoAuditingConfig {

    @Bean
    public ReactiveAuditorAware<String> auditorProvider() {
        return new ReactiveAuditorAwareImpl();
    }

    public static class ReactiveAuditorAwareImpl implements ReactiveAuditorAware<String> {

        @Override
        @NonNull
        public Mono<String> getCurrentAuditor() {
            return ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)
                    .filter(Authentication::isAuthenticated)
                    .map(authentication -> {
                        // Try to get user ID from the JWT principal
                        Object principal = authentication.getPrincipal();
                        if (principal instanceof String) {
                            return (String) principal;
                        }
                        // Fallback to name
                        return authentication.getName();
                    })
                    .switchIfEmpty(Mono.just("system")); // Fallback for system operations
        }
    }
}
