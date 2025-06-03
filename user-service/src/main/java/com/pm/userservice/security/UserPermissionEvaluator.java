package com.pm.userservice.security;

import java.io.Serializable;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.pm.commonsecurity.security.Action;
import com.pm.commonsecurity.security.PermissionEvaluator;

/**
 * User-specific permission evaluator that extends the base RBAC system
 * to handle user self-access scenarios (users accessing their own profiles).
 * Implements Spring's PermissionEvaluator interface to be used by MethodSecurityExpressionHandler.
 */
@Component
public class UserPermissionEvaluator implements org.springframework.security.access.PermissionEvaluator { // Implement Spring's PermissionEvaluator

    private final PermissionEvaluator basePermissionEvaluator; // This is com.pm.commonsecurity.security.PermissionEvaluator

    public UserPermissionEvaluator(PermissionEvaluator basePermissionEvaluator) {
        this.basePermissionEvaluator = basePermissionEvaluator;
    }

    /**
     * Evaluates permissions for user operations with self-access support.
     * Users can always read and update their own profiles regardless of role.
     * This method signature matches Spring's PermissionEvaluator.
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }

        try {
            Action action = Action.valueOf(permission.toString().toUpperCase());
            String currentUserId = getCurrentUserId(authentication);
            
            // Check if user is trying to access their own data
            if (targetDomainObject instanceof String) { // Assuming targetDomainObject is userId as String
                String targetUserId = (String) targetDomainObject;
                if (currentUserId != null && currentUserId.equals(targetUserId)) {
                    // Users can read and update their own profiles
                    return action == Action.USER_READ || action == Action.USER_UPDATE;
                }
            }
            
            // Fall back to role-based permission evaluation
            List<String> userRoles = getUserRoles(authentication);
            return basePermissionEvaluator.hasPermission(userRoles, action);
            
        } catch (IllegalArgumentException e) {
            // Invalid action string
            return false;
        }
    }

    /**
     * Evaluates permissions with target ID and type (matches Spring's PermissionEvaluator).
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }

        try {
            Action action = Action.valueOf(permission.toString().toUpperCase());
            String currentUserId = getCurrentUserId(authentication);

            // Check for self-access if targetType is "User" and targetId is the current user's ID
            if ("User".equalsIgnoreCase(targetType) && targetId instanceof String) {
                String targetUserId = (String) targetId;
                if (currentUserId != null && currentUserId.equals(targetUserId)) {
                    return action == Action.USER_READ || action == Action.USER_UPDATE;
                }
            }

            // Fall back to role-based permission evaluation
            List<String> userRoles = getUserRoles(authentication);
            // Assuming the basePermissionEvaluator might not need targetId and targetType directly for role-based checks
            // or it has its own way to handle them if necessary.
            return basePermissionEvaluator.hasPermission(userRoles, action);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String getCurrentUserId(Authentication authentication) {
        // Extract user ID from authentication - this depends on your JWT implementation
        // You might store this in the principal or as a custom claim
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        }
        // You might need to adapt this based on your JWT structure
        return null;
    }

    private List<String> getUserRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(auth -> auth.startsWith("ROLE_"))
                .toList();
    }
}
