package com.pm.commonsecurity.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Utility class for common permission checking patterns.
 * Provides convenient methods for typical authorization scenarios.
 */
@Component
@RequiredArgsConstructor
public class PermissionUtils {
    
    private final PermissionEvaluator permissionEvaluator;
    
    /**
     * Check if user can read their own profile
     */
    public boolean canReadOwnProfile(List<String> userRoles) {
        return permissionEvaluator.hasPermission(userRoles, Action.USER_SELF_READ);
    }
    
    /**
     * Check if user can update their own profile
     */
    public boolean canUpdateOwnProfile(List<String> userRoles) {
        return permissionEvaluator.hasPermission(userRoles, Action.USER_SELF_UPDATE);
    }
    
    /**
     * Check if user can manage other users (admin function)
     */
    public boolean canManageUsers(List<String> userRoles) {
        return permissionEvaluator.hasPermission(userRoles, Action.USER_READ) ||
               permissionEvaluator.hasPermission(userRoles, Action.USER_UPDATE) ||
               permissionEvaluator.hasPermission(userRoles, Action.USER_CREATE) ||
               permissionEvaluator.hasPermission(userRoles, Action.USER_DELETE);
    }
    
    /**
     * Check if user can create projects
     */
    public boolean canCreateProjects(List<String> userRoles) {
        return permissionEvaluator.hasPermission(userRoles, Action.PRJ_CREATE);
    }
    
    /**
     * Check if user can manage project (full project management)
     */
    public boolean canManageProject(List<String> userRoles) {
        return permissionEvaluator.hasPermission(userRoles, Action.PRJ_UPDATE) ||
               permissionEvaluator.hasPermission(userRoles, Action.PRJ_DELETE) ||
               permissionEvaluator.hasPermission(userRoles, Action.PRJ_ARCHIVE);
    }
    
    /**
     * Check if user can manage project members
     */
    public boolean canManageProjectMembers(List<String> userRoles) {
        return permissionEvaluator.hasPermission(userRoles, Action.PRJ_MEMBER_ADD) ||
               permissionEvaluator.hasPermission(userRoles, Action.PRJ_MEMBER_REMOVE);
    }
    
    /**
     * Check if user can create tasks
     */
    public boolean canCreateTasks(List<String> userRoles) {
        return permissionEvaluator.hasPermission(userRoles, Action.TASK_CREATE);
    }
    
    /**
     * Check if user can manage task workflow
     */
    public boolean canManageTaskWorkflow(List<String> userRoles) {
        return permissionEvaluator.hasPermission(userRoles, Action.TASK_STATUS_CHANGE) ||
               permissionEvaluator.hasPermission(userRoles, Action.TASK_PRIORITY_CHANGE) ||
               permissionEvaluator.hasPermission(userRoles, Action.TASK_ASSIGN);
    }
    
    /**
     * Check if user can delete any comments (moderator function)
     */
    public boolean canModerateComments(List<String> userRoles) {
        return permissionEvaluator.hasPermission(userRoles, Action.CMT_DELETE_ANY);
    }
    
    /**
     * Check if user has admin privileges (wildcard permissions)
     */
    public boolean isAdmin(List<String> userRoles) {
        return userRoles.contains("ROLE_ADMIN") && 
               permissionEvaluator.roleHasPermission("ROLE_ADMIN", "**");
    }
}
