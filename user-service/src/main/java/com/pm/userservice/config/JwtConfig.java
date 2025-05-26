package com.pm.userservice.config;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.jsonwebtoken.security.Keys;

@Configuration
public class JwtConfig {
    @Value("${jwt.secret:my-very-secret-key-for-jwt-signing}")
    private String jwtSecret;    @Bean
    public SecretKey jwtSecretKey() {
        // Use the same secret as API Gateway - generate a proper key
        String secret = jwtSecret;
        if (secret.length() < 32) {
            // Pad the secret if it's too short
            secret = secret + "0".repeat(32 - secret.length());
        }
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public static final long JWT_EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours
}
