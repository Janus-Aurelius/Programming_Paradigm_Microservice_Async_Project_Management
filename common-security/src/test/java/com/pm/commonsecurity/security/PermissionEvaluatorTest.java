package com.pm.commonsecurity.security;

import com.pm.commonsecurity.config.RbacConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for PermissionEvaluator functionality
 */
public class PermissionEvaluatorTest {
    
    private PermissionEvaluator permissionEvaluator;
    private RbacConfigurationProperties rbacConfig;
    
    @BeforeEach
    void setUp() {
        // Manual setup for testing
        rbacConfig = new RbacConfigurationProperties();
        rbacConfig.setRoles(Map.of(
            "ROLE_USER", List.of("USER_SELF_READ", "USER_SELF_UPDATE", "CMT_CREATE", "CMT_UPDATE_OWN", "CMT_DELETE_OWN", "NOTI_READ", "NOTI_MARK_READ"),
            "ROLE_DEVELOPER", List.of("USER_SELF_READ", "USER_SELF_UPDATE", "CMT_CREATE", "CMT_UPDATE_OWN", "CMT_DELETE_OWN", "NOTI_READ", "NOTI_MARK_READ", "PRJ_READ", "TASK_CREATE", "TASK_READ", "TASK_UPDATE", "TASK_STATUS_CHANGE"),
            "ROLE_PROJECT_MANAGER", List.of("USER_SELF_READ", "USER_SELF_UPDATE", "CMT_CREATE", "CMT_UPDATE_OWN", "CMT_DELETE_OWN", "NOTI_READ", "NOTI_MARK_READ", "PRJ_READ", "TASK_CREATE", "TASK_READ", "TASK_UPDATE", "TASK_STATUS_CHANGE", "PRJ_CREATE", "PRJ_UPDATE", "PRJ_DELETE", "PRJ_ARCHIVE", "TASK_ASSIGN", "TASK_DELETE", "CMT_DELETE_ANY"),
            "ROLE_ADMIN", List.of("**")
        ));
        
        permissionEvaluator = new PermissionEvaluator(rbacConfig);
    }
    
    @Test
    void testUserRolePermissions() {
        List<String> userRoles = List.of("ROLE_USER");
        
        // User should be able to read/update own profile
        assertThat(permissionEvaluator.hasPermission(userRoles, Action.USER_SELF_READ)).isTrue();
        assertThat(permissionEvaluator.hasPermission(userRoles, Action.USER_SELF_UPDATE)).isTrue();
        
        // User should NOT be able to manage other users
        assertThat(permissionEvaluator.hasPermission(userRoles, Action.USER_READ)).isFalse();
        assertThat(permissionEvaluator.hasPermission(userRoles, Action.USER_CREATE)).isFalse();
        
        // User should NOT be able to create projects
        assertThat(permissionEvaluator.hasPermission(userRoles, Action.PRJ_CREATE)).isFalse();
    }
    
    @Test
    void testDeveloperRolePermissions() {
        List<String> developerRoles = List.of("ROLE_DEVELOPER");
        
        // Developer should have user permissions
        assertThat(permissionEvaluator.hasPermission(developerRoles, Action.USER_SELF_READ)).isTrue();
        
        // Developer should be able to read projects and work with tasks
        assertThat(permissionEvaluator.hasPermission(developerRoles, Action.PRJ_READ)).isTrue();
        assertThat(permissionEvaluator.hasPermission(developerRoles, Action.TASK_CREATE)).isTrue();
        assertThat(permissionEvaluator.hasPermission(developerRoles, Action.TASK_UPDATE)).isTrue();
        
        // Developer should NOT be able to delete projects
        assertThat(permissionEvaluator.hasPermission(developerRoles, Action.PRJ_DELETE)).isFalse();
    }
    
    @Test
    void testProjectManagerRolePermissions() {
        List<String> pmRoles = List.of("ROLE_PROJECT_MANAGER");
        
        // PM should be able to create and manage projects
        assertThat(permissionEvaluator.hasPermission(pmRoles, Action.PRJ_CREATE)).isTrue();
        assertThat(permissionEvaluator.hasPermission(pmRoles, Action.PRJ_UPDATE)).isTrue();
        assertThat(permissionEvaluator.hasPermission(pmRoles, Action.PRJ_DELETE)).isTrue();
        
        // PM should be able to assign and delete tasks
        assertThat(permissionEvaluator.hasPermission(pmRoles, Action.TASK_ASSIGN)).isTrue();
        assertThat(permissionEvaluator.hasPermission(pmRoles, Action.TASK_DELETE)).isTrue();
        
        // PM should be able to moderate comments
        assertThat(permissionEvaluator.hasPermission(pmRoles, Action.CMT_DELETE_ANY)).isTrue();
    }
    
    @Test
    void testAdminRolePermissions() {
        List<String> adminRoles = List.of("ROLE_ADMIN");
        
        // Admin should have wildcard permissions
        assertThat(permissionEvaluator.hasPermission(adminRoles, Action.USER_CREATE)).isTrue();
        assertThat(permissionEvaluator.hasPermission(adminRoles, Action.USER_DELETE)).isTrue();
        assertThat(permissionEvaluator.hasPermission(adminRoles, Action.PRJ_DELETE)).isTrue();
        assertThat(permissionEvaluator.hasPermission(adminRoles, "ANY_ACTION")).isTrue();
    }
    
    @Test
    void testMultipleRoles() {
        List<String> multipleRoles = List.of("ROLE_USER", "ROLE_DEVELOPER");
        
        // Should have permissions from both roles
        assertThat(permissionEvaluator.hasPermission(multipleRoles, Action.USER_SELF_READ)).isTrue();
        assertThat(permissionEvaluator.hasPermission(multipleRoles, Action.PRJ_READ)).isTrue();
        assertThat(permissionEvaluator.hasPermission(multipleRoles, Action.TASK_CREATE)).isTrue();
    }
    
    @Test
    void testNoRoles() {
        List<String> noRoles = List.of();
        
        // Should have no permissions
        assertThat(permissionEvaluator.hasPermission(noRoles, Action.USER_SELF_READ)).isFalse();
        assertThat(permissionEvaluator.hasPermission(noRoles, Action.PRJ_READ)).isFalse();
    }
}
