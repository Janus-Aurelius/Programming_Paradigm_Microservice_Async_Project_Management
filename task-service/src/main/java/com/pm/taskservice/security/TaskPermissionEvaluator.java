package com.pm.taskservice.security;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.pm.commonsecurity.security.Action;
import com.pm.commonsecurity.security.PermissionEvaluator;
import com.pm.taskservice.model.Task;
import com.pm.taskservice.repository.TaskRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Reactive Task-specific permission evaluator for use in reactive chains. This
 * maintains the same permission logic but returns Mono<Boolean> instead of
 * blocking boolean values.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TaskPermissionEvaluator {

    private final TaskRepository taskRepository;
    private final PermissionEvaluator basePermissionEvaluator;

    /**
     * Check permission for a specific task reactively
     *
     * @param authentication The user's authentication
     * @param taskId The task ID
     * @param action The action/permission to check
     * @return Mono<Boolean> indicating if user has permission
     */
    public Mono<Boolean> hasPermission(Authentication authentication, String taskId, String action) {
        if (authentication == null || !authentication.isAuthenticated() || action == null) {
            return Mono.just(false);
        }

        try {
            Action actionEnum = Action.valueOf(action.toUpperCase());
            String currentUserId = getCurrentUserId(authentication);

            // Get task reactively and check permissions
            return taskRepository.findById(taskId)
                    .map(task -> hasTaskAccess(authentication, task, actionEnum, currentUserId))
                    .switchIfEmpty(Mono.just(false)) // Task not found = no permission
                    .onErrorResume(e -> {
                        log.error("Error checking permissions for task {} and action {}: {}", taskId, action, e.getMessage());
                        return Mono.just(false);
                    });
        } catch (IllegalArgumentException e) {
            log.warn("Invalid action string: {}", action);
            return Mono.just(false);
        }
    }

    /**
     * Check permission when we already have the task entity
     *
     * @param authentication The user's authentication
     * @param task The task entity
     * @param action The action/permission to check
     * @return Mono<Boolean> indicating if user has permission
     */
    public Mono<Boolean> hasPermission(Authentication authentication, Task task, String action) {
        if (authentication == null || !authentication.isAuthenticated() || action == null || task == null) {
            return Mono.just(false);
        }

        try {
            Action actionEnum = Action.valueOf(action.toUpperCase());
            String currentUserId = getCurrentUserId(authentication);
            boolean hasAccess = hasTaskAccess(authentication, task, actionEnum, currentUserId);
            return Mono.just(hasAccess);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid action string: {}", action);
            return Mono.just(false);
        } catch (Exception e) {
            log.error("Error checking permissions for task {} and action {}: {}", task.getId(), action, e.getMessage());
            return Mono.just(false);
        }
    }

    /**
     * Check general permissions (not task-specific)
     *
     * @param authentication The user's authentication
     * @param action The action/permission to check
     * @return Mono<Boolean> indicating if user has permission
     */
    public Mono<Boolean> hasGeneralPermission(Authentication authentication, String action) {
        if (authentication == null || !authentication.isAuthenticated() || action == null) {
            return Mono.just(false);
        }

        try {
            Action actionEnum = Action.valueOf(action.toUpperCase());
            List<String> userRoles = getUserRoles(authentication);
            boolean hasPermission = basePermissionEvaluator.hasPermission(userRoles, actionEnum);
            return Mono.just(hasPermission);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid action string: {}", action);
            return Mono.just(false);
        } catch (Exception e) {
            log.error("Error checking general permission for action {}: {}", action, e.getMessage());
            return Mono.just(false);
        }
    }

    private boolean hasTaskAccess(Authentication authentication, Task task, Action action, String currentUserId) {
        // Check if user is task creator or assignee
        boolean isCreator = currentUserId != null && currentUserId.equals(task.getCreatedBy());
        boolean isAssignee = currentUserId != null && currentUserId.equals(task.getAssigneeId());

        // Task-specific permissions based on ownership/assignment
        return switch (action) {
            case TASK_CREATE -> {
                // Anyone with TASK_CREATE permission can create tasks - delegate to role-based evaluation
                List<String> userRoles = getUserRoles(authentication);
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
            case TASK_READ -> {
                // Creators, assignees and users with role-based permissions can read the task
                if (isCreator || isAssignee) {
                    yield true;
                }
                // Fall back to role-based evaluation
                List<String> userRoles = getUserRoles(authentication);
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
            case TASK_UPDATE, TASK_STATUS_CHANGE, TASK_PRIORITY_CHANGE -> {
                // Creators, assignees and project managers can update tasks
                if (isCreator || isAssignee) {
                    yield true;
                }
                // Fall back to role-based evaluation
                List<String> userRoles = getUserRoles(authentication);
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
            case TASK_DELETE -> {
                // Only creators and admins can delete tasks
                if (isCreator) {
                    yield true;
                }
                // Fall back to role-based evaluation
                List<String> userRoles = getUserRoles(authentication);
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
            case TASK_ASSIGN -> {
                // Creators and project managers can assign tasks
                if (isCreator) {
                    yield true;
                }
                // Fall back to role-based evaluation
                List<String> userRoles = getUserRoles(authentication);
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
            // For all other actions, use role-based evaluation
            default -> {
                List<String> userRoles = getUserRoles(authentication);
                yield basePermissionEvaluator.hasPermission(userRoles, action);
            }
        };
    }

    private String getCurrentUserId(Authentication authentication) {
        // Extract user ID from authentication - this depends on your JWT implementation
        Object principal = authentication.getPrincipal();
        if (principal instanceof String stringPrincipal) {
            return stringPrincipal;
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
