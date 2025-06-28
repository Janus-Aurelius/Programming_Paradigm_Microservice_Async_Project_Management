package com.pm.notificationservice.security;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.pm.commonsecurity.security.Action;
import com.pm.commonsecurity.security.PermissionEvaluator;
import com.pm.notificationservice.model.Notification;
import com.pm.notificationservice.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Reactive notification-specific permission evaluator that extends the base
 * RBAC system to handle notification-specific access scenarios (recipient
 * ownership).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveNotificationPermissionEvaluator {

    private final NotificationRepository notificationRepository;
    private final PermissionEvaluator basePermissionEvaluator;

    /**
     * Reactive permission check for notification operations
     */
    public Mono<Boolean> hasPermission(Authentication authentication, Object targetDomainObject, String permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return Mono.just(false);
        }

        try {
            Action action = Action.valueOf(permission.toUpperCase());
            String currentUserId = getCurrentUserId(authentication);

            // Check if user is trying to access a specific notification
            if (targetDomainObject instanceof Notification notification) {
                return Mono.just(hasNotificationAccess(authentication, notification, action, currentUserId));
            }

            // Check if targetDomainObject is a notification ID string
            if (targetDomainObject instanceof String notificationId) {
                // For operations that need notification context but only have the ID
                return notificationRepository.findById(notificationId)
                        .map(notification -> hasNotificationAccess(authentication, notification, action, currentUserId))
                        .defaultIfEmpty(false);
            }

            // Fall back to role-based permission evaluation for general actions
            List<String> userRoles = getUserRoles(authentication);
            return Mono.just(basePermissionEvaluator.hasPermission(userRoles, action));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid permission string: {}", permission);
            return Mono.just(false);
        } catch (Exception e) {
            log.error("Error evaluating permission", e);
            return Mono.just(false);
        }
    }

    /**
     * Reactive permission check specifically for notification ID
     */
    public Mono<Boolean> hasPermission(Authentication authentication, String notificationId, String permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return Mono.just(false);
        }

        try {
            Action action = Action.valueOf(permission.toUpperCase());
            String currentUserId = getCurrentUserId(authentication);

            return notificationRepository.findById(notificationId)
                    .map(notification -> hasNotificationAccess(authentication, notification, action, currentUserId))
                    .defaultIfEmpty(false)
                    .onErrorResume(e -> {
                        log.error("Error finding notification {}, falling back to role-based permission", notificationId, e);
                        List<String> userRoles = getUserRoles(authentication);
                        return Mono.just(basePermissionEvaluator.hasPermission(userRoles, action));
                    });

        } catch (IllegalArgumentException e) {
            log.warn("Invalid permission string: {}", permission);
            return Mono.just(false);
        } catch (Exception e) {
            log.error("Error evaluating permission", e);
            return Mono.just(false);
        }
    }

    /**
     * Reactive permission check for general notification operations (not
     * specific to a notification)
     */
    public Mono<Boolean> hasGeneralPermission(Authentication authentication, String permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            log.warn("Permission check failed: authentication={}, permission={}", authentication, permission);
            return Mono.just(false);
        }

        try {
            Action action = Action.valueOf(permission.toUpperCase());
            List<String> userRoles = getUserRoles(authentication);
            String currentUserId = getCurrentUserId(authentication);

            log.debug("Permission check - User: {}, Roles: {}, Action: {}", currentUserId, userRoles, action);

            // Use the base permission evaluator for all actions to ensure consistency with permissions.yml
            boolean hasPermission = basePermissionEvaluator.hasPermission(userRoles, action);

            log.debug("Permission result: {} for user {} with roles {} and action {}",
                    hasPermission, currentUserId, userRoles, action);

            return Mono.just(hasPermission);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid permission string: {}", permission);
            return Mono.just(false);
        } catch (Exception e) {
            log.error("Error evaluating general permission", e);
            return Mono.just(false);
        }
    }

    /**
     * Evaluates notification-specific access rules
     */
    private boolean hasNotificationAccess(Authentication authentication, Notification notification, Action action, String currentUserId) {
        List<String> userRoles = getUserRoles(authentication);

        // Check if the user is the recipient of the notification
        boolean isRecipient = currentUserId.equals(notification.getRecipientUserId());

        return switch (action) {
            case NOTI_READ ->
                // Recipients can read their notifications, admins can read all
                isRecipient || userRoles.contains("ROLE_ADMIN");

            case NOTI_MARK_READ ->
                // Recipients can mark their notifications as read, admins can mark any as read
                isRecipient || userRoles.contains("ROLE_ADMIN");

            case NOTI_SEND ->
                // Only system/admin can send notifications (typically system-generated)
                userRoles.contains("ROLE_ADMIN") || userRoles.contains("ROLE_SYSTEM");

            default ->
                // For other actions, fall back to role-based permission evaluation
                basePermissionEvaluator.hasPermission(userRoles, action);
        };
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
    private List<String> getUserRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(role -> role.startsWith("ROLE_"))
                .toList();
    }
}
