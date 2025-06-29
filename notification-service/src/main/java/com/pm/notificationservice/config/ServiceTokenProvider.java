package com.pm.notificationservice.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServiceTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(ServiceTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.application.name}")
    private String serviceName;

    public String createServiceToken() {
        try {
            // Create a simple service token using HMAC-like approach
            long currentMinute = System.currentTimeMillis() / 60000;
            String payload = serviceName + ":" + currentMinute; // Valid for 1 minute
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String toHash = payload + ":" + jwtSecret;
            byte[] hash = md.digest(toHash.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(hash);
            String token = Base64.getEncoder().encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));

            logger.debug("Created service token for service: {}, timestamp: {}, token preview: {}...",
                    serviceName, currentMinute, token.length() > 10 ? token.substring(0, 10) : token);

            return token;
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
