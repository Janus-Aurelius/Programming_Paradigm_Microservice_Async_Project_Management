package com.pm.taskservice.security;

import com.pm.commonsecurity.security.Action;
import com.pm.commonsecurity.security.PermissionEvaluator;
import com.pm.taskservice.model.Task;
import com.pm.taskservice.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * Task-specific permission evaluator that extends the base RBAC system
 * to handle task-specific access scenarios (task assignees, creators).
 * Implements Spring's PermissionEvaluator interface to be used by MethodSecurityExpressionHandler.
 */
@Component
@RequiredArgsConstructor
public class TaskPermissionEvaluator implements org.springframework.security.access.PermissionEvaluator {

    private final TaskRepository taskRepository;
    private final PermissionEvaluator basePermissionEvaluator; // This is com.pm.commonsecurity.security.PermissionEvaluator

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }

        try {
            Action action = Action.valueOf(permission.toString().toUpperCase());
            String currentUserId = getCurrentUserId(authentication);
            
            // Check if user is trying to access a specific task
            if (targetDomainObject instanceof Task task) {
                return hasTaskAccess(authentication, task, action, currentUserId);
            }
            
            // Check if targetDomainObject is a task ID string
            if (targetDomainObject instanceof String taskId) {
                // For operations that need task context but only have the ID
                Task task = taskRepository.findById(taskId).block();
                if (task != null) {
                    return hasTaskAccess(authentication, task, action, currentUserId);
                }
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

            // Check for task-specific access if targetType is "Task"
            if ("Task".equalsIgnoreCase(targetType) && targetId instanceof String) {
                String taskId = (String) targetId;
                Task task = taskRepository.findById(taskId).block();
                if (task != null) {
                    return hasTaskAccess(authentication, task, action, currentUserId);
                }
            }
            
            // Fall back to role-based permission evaluation
            List<String> userRoles = getUserRoles(authentication);
            return basePermissionEvaluator.hasPermission(userRoles, action);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean hasTaskAccess(Authentication authentication, Task task, Action action, String currentUserId) {
        // Check if user is task creator or assignee
        boolean isCreator = currentUserId != null && currentUserId.equals(task.getCreatedBy());
        boolean isAssignee = currentUserId != null && currentUserId.equals(task.getAssigneeId());
        
        // Task-specific permissions based on ownership/assignment
        switch (action) {
            case TASK_READ:
                // Creators, assignees and users with role-based permissions can read the task
                if (isCreator || isAssignee) {
                    return true;
                }
                break;
            case TASK_UPDATE:
            case TASK_STATUS_CHANGE:
            case TASK_PRIORITY_CHANGE:
                // Creators, assignees and project managers can update tasks
                if (isCreator || isAssignee) {
                    return true;
                }
                break;
            case TASK_DELETE:
                // Only creators and admins can delete tasks
                if (isCreator) {
                    return true;
                }
                break;
            case TASK_ASSIGN:
                // Creators and project managers can assign tasks
                if (isCreator) {
                    return true;
                }
                break;
        }
        
        // Fall back to role-based evaluation
        List<String> userRoles = getUserRoles(authentication);
        return basePermissionEvaluator.hasPermission(userRoles, action);
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
