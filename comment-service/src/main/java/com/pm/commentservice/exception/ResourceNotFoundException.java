package com.pm.commentservice.exception;
    // Example for resource not found errors
    public class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
// You might add others like ValidationException, BusinessRuleException etc.
