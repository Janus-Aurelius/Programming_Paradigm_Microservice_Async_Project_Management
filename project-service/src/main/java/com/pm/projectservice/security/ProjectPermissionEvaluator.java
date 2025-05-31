package com.pm.projectservice.security;

import com.pm.commonsecurity.security.Action;
import com.pm.commonsecurity.security.PermissionEvaluator;
import com.pm.projectservice.model.Project;
import com.pm.projectservice.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * Project-specific permission evaluator that extends the base RBAC system
 * to handle project-specific access scenarios (project members, creators).
 * Implements Spring's PermissionEvaluator interface to be used by MethodSecurityExpressionHandler.
 */
@Component
@RequiredArgsConstructor
public class ProjectPermissionEvaluator implements org.springframework.security.access.PermissionEvaluator {

    private final ProjectRepository projectRepository;
    private final PermissionEvaluator basePermissionEvaluator; // This is com.pm.commonsecurity.security.PermissionEvaluator

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }

        try {
            Action action = Action.valueOf(permission.toString().toUpperCase());
            String currentUserId = getCurrentUserId(authentication);
            
            // Check if user is trying to access a specific project
            if (targetDomainObject instanceof Project project) {
                return hasProjectAccess(authentication, project, action, currentUserId);
            }
            
            // Check if targetDomainObject is a project ID string
            if (targetDomainObject instanceof String projectId) {
                // For operations that need project context but only have the ID
                Project project = projectRepository.findById(projectId).block();
                if (project != null) {
                    return hasProjectAccess(authentication, project, action, currentUserId);
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

            // Check for project-specific access if targetType is "Project"
            if ("Project".equalsIgnoreCase(targetType) && targetId instanceof String) {
                String projectId = (String) targetId;
                Project project = projectRepository.findById(projectId).block();
                if (project != null) {
                    return hasProjectAccess(authentication, project, action, currentUserId);
                }
            }

            // Fall back to role-based permission evaluation
            List<String> userRoles = getUserRoles(authentication);
            return basePermissionEvaluator.hasPermission(userRoles, action);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean hasProjectAccess(Authentication authentication, Project project, Action action, String currentUserId) {
        // Check if user is project creator
        boolean isCreator = currentUserId != null && currentUserId.equals(project.getCreatedBy());
        
        // Check if user is project member
        boolean isMember = currentUserId != null && project.getMemberIds() != null && 
                          project.getMemberIds().contains(currentUserId);
        
        // Project-specific permissions based on membership/ownership
        switch (action) {
            case PRJ_READ:
                // Members and creators can read the project, plus role-based access
                if (isCreator || isMember) {
                    return true;
                }
                break;
            case PRJ_UPDATE:
            case PRJ_STATUS_CHANGE:
                // Only creators and project managers can update projects
                if (isCreator) {
                    return true;
                }
                break;
            case PRJ_DELETE:
            case PRJ_ARCHIVE:
                // Only creators and admins can delete/archive projects
                if (isCreator) {
                    return true;
                }
                break;
            case PRJ_MEMBER_ADD:
            case PRJ_MEMBER_REMOVE:
                // Only creators and project managers can manage members
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
