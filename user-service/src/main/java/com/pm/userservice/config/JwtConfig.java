package com.pm.userservice.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfig {
    @Value("${jwt.secret:my-very-secret-key-for-jwt-signing}")
    private String jwtSecret;

    @Bean
    public SecretKey jwtSecretKey() {
        // Use the same secret as API Gateway
        byte[] keyBytes = Base64.getEncoder().encode(jwtSecret.getBytes());
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
    }

    public static final long JWT_EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours
}
