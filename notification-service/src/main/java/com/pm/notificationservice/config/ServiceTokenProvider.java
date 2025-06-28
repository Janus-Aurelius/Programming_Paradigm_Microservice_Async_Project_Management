package com.pm.notificationservice.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServiceTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.application.name}")
    private String serviceName;

    public String createServiceToken() {
        try {
            // Create a simple service token using HMAC-like approach
            String payload = serviceName + ":" + System.currentTimeMillis() / 60000; // Valid for 1 minute
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String toHash = payload + ":" + jwtSecret;
            byte[] hash = md.digest(toHash.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(hash);
            return Base64.getEncoder().encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
