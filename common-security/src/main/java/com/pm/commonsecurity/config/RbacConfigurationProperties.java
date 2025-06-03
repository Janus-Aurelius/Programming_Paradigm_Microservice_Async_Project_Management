package com.pm.commonsecurity.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope; // Added for @RefreshScope

import lombok.Data;

/**
 * Configuration properties for RBAC permissions loaded from permissions.yml
 */
@Data
@ConfigurationProperties(prefix = "rbac")
@RefreshScope // Added to enable runtime refresh of these properties
public class RbacConfigurationProperties {
    
    /**
     * Map of role names to their allowed actions.
     * Supports YAML anchors and references for role inheritance.
     */
    private Map<String, List<String>> roles;
    
    /**
     * Get permissions for a specific role
     * @param roleName the role name (e.g., "ROLE_USER", "ROLE_ADMIN")
     * @return list of actions the role can perform
     */
    public List<String> getPermissionsForRole(String roleName) {
        return roles.get(roleName);
    }
    
    /**
     * Check if a role exists in the configuration
     * @param roleName the role name to check
     * @return true if the role is defined
     */
    public boolean hasRole(String roleName) {
        return roles != null && roles.containsKey(roleName);
    }
    
    /**
     * Get all defined role names
     * @return set of all role names
     */
    public java.util.Set<String> getAllRoleNames() {
        return roles != null ? roles.keySet() : java.util.Set.of();
    }
}
