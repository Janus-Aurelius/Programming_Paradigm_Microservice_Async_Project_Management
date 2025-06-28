package com.pm.commonsecurity.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.pm.commonsecurity.config.RbacConfigurationProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Central permission evaluator for RBAC-based authorization. This component
 * evaluates whether a user with specific roles can perform a given action.
 */
@Slf4j
@Component
public class PermissionEvaluator {

    private final RbacConfigurationProperties rbacConfig;

    public PermissionEvaluator(RbacConfigurationProperties rbacConfig) {
        this.rbacConfig = rbacConfig;
    }

    /**
     * Check if any of the user's roles allow the specified action
     *
     * @param userRoles collection of role names the user has
     * @param action the action to check (from Action enum)
     * @return true if the user has permission to perform the action
     */
    public boolean hasPermission(Collection<String> userRoles, Action action) {
        return hasPermission(userRoles, action.name());
    }

    /**
     * Check if any of the user's roles allow the specified action
     *
     * @param userRoles collection of role names the user has
     * @param actionName the action name to check
     * @return true if the user has permission to perform the action
     */
    public boolean hasPermission(Collection<String> userRoles, String actionName) {
        if (userRoles == null || userRoles.isEmpty()) {
            log.debug("No roles provided for permission check");
            return false;
        }

        for (String role : userRoles) {
            if (roleHasPermission(role, actionName)) {
                log.debug("Permission granted: role '{}' allows action '{}'", role, actionName);
                return true;
            }
        }

        log.debug("Permission denied: no role in {} allows action '{}'", userRoles, actionName);
        return false;
    }

    /**
     * Check if a specific role allows the specified action
     *
     * @param roleName the role name to check
     * @param actionName the action name to check
     * @return true if the role has permission for the action
     */
    public boolean roleHasPermission(String roleName, String actionName) {
        log.debug("Checking permission: role='{}', action='{}'", roleName, actionName);

        List<String> rolePermissions = rbacConfig.getPermissionsForRole(roleName);

        log.debug("Permissions for role '{}': {}", roleName, rolePermissions);

        if (rolePermissions == null) {
            log.debug("Role '{}' not found in configuration", roleName);
            // Let's also debug what roles ARE available
            log.debug("Available roles in config: {}", rbacConfig.getAllRoleNames());
            return false;
        }

        // Check for wildcard permissions (admin role)
        if (rolePermissions.contains("**")) {
            log.debug("Role '{}' has wildcard permissions", roleName);
            return true;
        }

        // Check for exact action match
        boolean hasPermission = rolePermissions.contains(actionName);
        log.debug("Role '{}' contains action '{}': {}", roleName, actionName, hasPermission);
        return hasPermission;
    }

    /**
     * Get all permissions for a specific role
     *
     * @param roleName the role name
     * @return list of actions the role can perform
     */
    public List<String> getPermissionsForRole(String roleName) {
        return rbacConfig.getPermissionsForRole(roleName);
    }

    /**
     * Check if a role exists in the configuration
     *
     * @param roleName the role name to check
     * @return true if the role is defined
     */
    public boolean roleExists(String roleName) {
        return rbacConfig.hasRole(roleName);
    }

    /**
     * Get all defined role names
     *
     * @return set of all role names
     */
    public Set<String> getAllRoleNames() {
        return rbacConfig.getAllRoleNames();
    }

    /**
     * Utility method to check if current authenticated user has permission This
     * can be used in method-level security annotations
     *
     * @param actionName the action to check
     * @return true if current user has permission
     */
    public boolean currentUserHasPermission(String actionName) {
        // This would integrate with Spring Security's SecurityContext
        // For now, returning false - to be implemented based on your auth mechanism
        log.warn("currentUserHasPermission() not fully implemented - requires SecurityContext integration");
        return false;
    }
}
