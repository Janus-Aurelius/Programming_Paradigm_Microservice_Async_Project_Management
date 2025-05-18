package com.pm.projectservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

// Use record for immutability and conciseness
@JsonInclude(JsonInclude.Include.NON_NULL) // Don't include null fields (like details) in JSON
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error, // Short error description (e.g., "Not Found", "Bad Request")
        String message, // More detailed error message
        String path, // Request path where error occurred
        List<String> details // Optional: For validation errors
) {
    // Constructor for general errors
    public ErrorResponse(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, null);
    }

    // Constructor including details (for validation)
    public ErrorResponse(int status, String error, String message, String path, List<String> details) {
        this(Instant.now(), status, error, message, path, details);
    }
}