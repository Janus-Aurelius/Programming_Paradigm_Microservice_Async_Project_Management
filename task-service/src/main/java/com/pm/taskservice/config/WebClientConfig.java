package com.pm.taskservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Value("${services.user-service.url}")
    private String userServiceUrl;

    @Value("${services.comment-service.url}")
    private String commentServiceUrl;

    @Value("http://project-service:8082")
    private String projectServiceUrl;



    @Bean
    @Qualifier("userWebClient")
    public WebClient userWebClient() {
        return WebClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    @Bean
    @Qualifier("commentWebClient")
    public WebClient commentWebClient() {
        return WebClient.builder()
                .baseUrl(commentServiceUrl)
                .build();
    }

    @Bean
    @Qualifier("projectWebClient")
    public WebClient projectWebClient() {
        return WebClient.builder()
                .baseUrl(projectServiceUrl)
                .build();
    }


}