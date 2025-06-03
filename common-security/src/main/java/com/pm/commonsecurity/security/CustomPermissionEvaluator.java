package com.pm.commonsecurity.security;

import com.pm.commonsecurity.config.RbacConfigurationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;

/**
 * Spring Security PermissionEvaluator that integrates with the RBAC system.
 * This allows using @PreAuthorize annotations with hasPermission() expressions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomPermissionEvaluator implements PermissionEvaluator {
    
    private final com.pm.commonsecurity.security.PermissionEvaluator rbacPermissionEvaluator;
    
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || permission == null) {
            log.debug("Authentication or permission is null");
            return false;
        }
        
        Collection<String> roles = extractRoles(authentication);
        String permissionStr = permission.toString();
        
        boolean hasPermission = rbacPermissionEvaluator.hasPermission(roles, permissionStr);
        
        log.debug("Permission check - User: {}, Roles: {}, Permission: {}, Result: {}", 
                  authentication.getName(), roles, permissionStr, hasPermission);
        
        return hasPermission;
    }
    
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, 
                               String targetType, Object permission) {
        if (authentication == null || permission == null) {
            log.debug("Authentication or permission is null");
            return false;
        }
        
        Collection<String> roles = extractRoles(authentication);
        String permissionStr = permission.toString();
        
        boolean hasPermission = rbacPermissionEvaluator.hasPermission(roles, permissionStr);
        
        log.debug("Permission check - User: {}, Roles: {}, Target: {}#{}, Permission: {}, Result: {}", 
                  authentication.getName(), roles, targetType, targetId, permissionStr, hasPermission);
        
        return hasPermission;
    }
    
    /**
     * Extract role names from Spring Security authentication
     */
    private Collection<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
