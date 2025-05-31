package com.pm.notificationservice.security;

import com.pm.commonsecurity.security.Action;
import com.pm.commonsecurity.security.PermissionEvaluator;
import com.pm.notificationservice.model.Notification;
import com.pm.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * Notification-specific permission evaluator that extends the base RBAC system
 * to handle notification-specific access scenarios (recipient ownership).
 * Implements Spring's PermissionEvaluator interface to be used by MethodSecurityExpressionHandler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPermissionEvaluator implements org.springframework.security.access.PermissionEvaluator {

    private final NotificationRepository notificationRepository;
    private final PermissionEvaluator basePermissionEvaluator; // This is com.pm.commonsecurity.security.PermissionEvaluator

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }

        try {
            Action action = Action.valueOf(permission.toString().toUpperCase());
            String currentUserId = getCurrentUserId(authentication);
            
            // Check if user is trying to access a specific notification
            if (targetDomainObject instanceof Notification notification) {
                return hasNotificationAccess(authentication, notification, action, currentUserId);
            }
            
            // Check if targetDomainObject is a notification ID string
            if (targetDomainObject instanceof String notificationId) {
                // For operations that need notification context but only have the ID
                Notification notification = notificationRepository.findById(notificationId).block();
                if (notification != null) {
                    return hasNotificationAccess(authentication, notification, action, currentUserId);
                }
            }
              // Fall back to role-based permission evaluation for general actions
            List<String> userRoles = getUserRoles(authentication);
            return basePermissionEvaluator.hasPermission(userRoles, action);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid permission string: {}", permission);
            return false;
        } catch (Exception e) {
            log.error("Error evaluating permission", e);
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
            
            if ("NOTIFICATION".equals(targetType) && targetId != null) {
                Notification notification = notificationRepository.findById(targetId.toString()).block();
                if (notification != null) {
                    return hasNotificationAccess(authentication, notification, action, currentUserId);
                }
            }
              // Fall back to role-based permission evaluation
            List<String> userRoles = getUserRoles(authentication);
            return basePermissionEvaluator.hasPermission(userRoles, action);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid permission string: {}", permission);
            return false;
        } catch (Exception e) {
            log.error("Error evaluating permission", e);
            return false;
        }
    }

    /**
     * Evaluates notification-specific access rules
     */
    private boolean hasNotificationAccess(Authentication authentication, Notification notification, Action action, String currentUserId) {
        List<String> userRoles = getUserRoles(authentication);
        
        // Check if the user is the recipient of the notification
        boolean isRecipient = currentUserId.equals(notification.getRecipientUserId());
        
        switch (action) {
            case NOTI_READ:
                // Recipients can read their notifications, admins can read all
                return isRecipient || userRoles.contains("ROLE_ADMIN");
                
            case NOTI_MARK_READ:
                // Recipients can mark their notifications as read, admins can mark any as read
                return isRecipient || userRoles.contains("ROLE_ADMIN");
                
            case NOTI_SEND:
                // Only system/admin can send notifications (typically system-generated)
                return userRoles.contains("ROLE_ADMIN") || userRoles.contains("ROLE_SYSTEM");
                  default:
                // For other actions, fall back to role-based permission evaluation
                return basePermissionEvaluator.hasPermission(userRoles, action);
        }
    }

    /**
     * Extracts user ID from authentication
     */
    private String getCurrentUserId(Authentication authentication) {
        return authentication.getName();
    }

    /**
     * Extracts user roles from authentication
     */
    @SuppressWarnings("unchecked")
    private List<String> getUserRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(role -> role.startsWith("ROLE_"))
                .toList();
    }
}
