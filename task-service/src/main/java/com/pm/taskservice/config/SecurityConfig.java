package com.pm.taskservice.config;

import com.pm.taskservice.security.TaskPermissionEvaluator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter;

import com.pm.commonsecurity.security.UserIdHeaderWebFilter;

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Configuration
public class SecurityConfig {

    @Value("${security.devMode:true}")
    private boolean devMode;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf(csrf -> csrf.disable());
        if (devMode) {
            http.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll());
        } else {
            http.authorizeExchange(exchanges -> exchanges
                    .pathMatchers("/actuator/**").permitAll()
                    .anyExchange().authenticated()
            );
        }
        return http.build();
    }

    @Bean
    public WebFilter userIdHeaderWebFilter() {
        return new UserIdHeaderWebFilter();
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            TaskPermissionEvaluator taskPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(taskPermissionEvaluator);
        return expressionHandler;
    }
}
