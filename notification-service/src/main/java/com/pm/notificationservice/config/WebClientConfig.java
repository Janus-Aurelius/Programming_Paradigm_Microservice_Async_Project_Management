package com.pm.notificationservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);

    @Value("${services.api-gateway.url:http://api-gateway:8080}")
    private String apiGatewayUrl;

    private final ServiceTokenProvider serviceTokenProvider;

    public WebClientConfig(ServiceTokenProvider serviceTokenProvider) {
        this.serviceTokenProvider = serviceTokenProvider;
    }

    @Bean
    @Qualifier("userWebClient")
    public WebClient userWebClient() {
        return WebClient.builder()
                .baseUrl(apiGatewayUrl + "/api/users")
                .filter((request, next) -> {
                    String token = serviceTokenProvider.createServiceToken();
                    var modifiedRequest = org.springframework.web.reactive.function.client.ClientRequest
                            .from(request)
                            .header("X-Service-Token", token)
                            .build();
                    return next.exchange(modifiedRequest);
                })
                .build();
    }

    @Bean
    @Qualifier("taskWebClient")
    public WebClient taskWebClient() {
        return WebClient.builder()
                .baseUrl(apiGatewayUrl + "/api/tasks")
                .filter((request, next) -> {
                    String token = serviceTokenProvider.createServiceToken();
                    var modifiedRequest = org.springframework.web.reactive.function.client.ClientRequest
                            .from(request)
                            .header("X-Service-Token", token)
                            .build();
                    return next.exchange(modifiedRequest);
                })
                .build();
    }

    @Bean
    @Qualifier("projectWebClient")
    public WebClient projectWebClient() {
        return WebClient.builder()
                .baseUrl(apiGatewayUrl + "/api/projects")
                .filter((request, next) -> {
                    String token = serviceTokenProvider.createServiceToken();
                    logger.info("Adding X-Service-Token header for project request. URL: {}, Token preview: {}...",
                            request.url(), token.length() > 10 ? token.substring(0, 10) : token);
                    var modifiedRequest = org.springframework.web.reactive.function.client.ClientRequest
                            .from(request)
                            .header("X-Service-Token", token)
                            .build();
                    return next.exchange(modifiedRequest);
                })
                .build();
    }

    @Bean
    @Qualifier("commentWebClient")
    public WebClient commentWebClient() {
        return WebClient.builder()
                .baseUrl(apiGatewayUrl + "/api/comments")
                .filter((request, next) -> {
                    String token = serviceTokenProvider.createServiceToken();
                    var modifiedRequest = org.springframework.web.reactive.function.client.ClientRequest
                            .from(request)
                            .header("X-Service-Token", token)
                            .build();
                    return next.exchange(modifiedRequest);
                })
                .build();
    }
}
