package com.pm.websocketservice.security;

import com.pm.commonsecurity.security.Action;
import com.pm.commonsecurity.security.PermissionEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * WebSocket-specific permission evaluator that extends the base RBAC system
 * to handle real-time communication access scenarios.
 * Implements Spring's PermissionEvaluator interface to be used by MethodSecurityExpressionHandler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketPermissionEvaluator implements org.springframework.security.access.PermissionEvaluator {

    private final PermissionEvaluator basePermissionEvaluator; // This is com.pm.commonsecurity.security.PermissionEvaluator

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }

        try {
            Action action = Action.valueOf(permission.toString().toUpperCase());
            String currentUserId = getCurrentUserId(authentication);
            
            // WebSocket permissions are primarily about real-time communication
            // Check for specific ownership scenarios first
            if (hasWebSocketSpecificAccess(authentication, targetDomainObject, action, currentUserId)) {
                return true;
            }
            
            // Fall back to role-based permission evaluation for general actions
            List<String> userRoles = getUserRoles(authentication);
            return basePermissionEvaluator.hasPermission(userRoles, action);
            
        } catch (IllegalArgumentException e) {
            // Invalid action string
            return false;
        }
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }

        try {
            Action action = Action.valueOf(permission.toString().toUpperCase());
            String currentUserId = getCurrentUserId(authentication);

            // For WebSocket operations with specific target types
            if ("Notification".equalsIgnoreCase(targetType) && targetId instanceof String) {
                String recipientId = (String) targetId;
                // Users can only receive their own notifications via WebSocket
                if (action == Action.NOTI_READ && currentUserId.equals(recipientId)) {
                    return true;
                }
            }
            
            // Fall back to role-based permission evaluation
            List<String> userRoles = getUserRoles(authentication);
            return basePermissionEvaluator.hasPermission(userRoles, action);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean hasWebSocketSpecificAccess(Authentication authentication, Object targetDomainObject, Action action, String currentUserId) {
        // WebSocket permissions are primarily based on real-time communication needs
        switch (action) {
            case PRJ_READ:
            case TASK_READ:
            case CMT_CREATE:
                // Allow users with basic read permissions to receive real-time updates
                return true;
            
            case NOTI_READ:
                // For notifications, check if user is the recipient
                if (targetDomainObject instanceof String recipientId) {
                    return currentUserId.equals(recipientId);
                }
                return true;
            
            default:
                return false;
        }
    }

    private String getCurrentUserId(Authentication authentication) {
        // Extract user ID from authentication - this depends on your JWT implementation
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        }
        return authentication.getName();
    }

    private List<String> getUserRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(auth -> auth.startsWith("ROLE_"))
                .toList();
    }
}
