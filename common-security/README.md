# Common Security - RBAC System

This module provides a comprehensive Role-Based Access Control (RBAC) system for the project management microservices architecture.

## Features

- **Hierarchical Role System**: Support for USER, DEVELOPER, PROJECT_MANAGER, and ADMIN roles
- **YAML Configuration**: Externalized permission configuration with support for role inheritance
- **Spring Security Integration**: Method-level security with `@PreAuthorize` annotations
- **Auto-Configuration**: Spring Boot auto-configuration for easy integration
- **Comprehensive Permission Set**: Covers all operations across User, Project, Task, Comment, and Notification services

## Quick Start

### 1. Add Dependency

Add this module as a dependency in your microservice:

```xml
<dependency>
    <groupId>com.pm</groupId>
    <artifactId>common-security</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Enable Method Security

Add `@EnableReactiveMethodSecurity` to your Spring configuration:

```java
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {
    // Your security configuration
}
```

### 3. Use in Controllers

```java
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    
    @PreAuthorize("hasPermission(null, 'PRJ_CREATE')")
    @PostMapping
    public Mono<ProjectDto> createProject(@RequestBody ProjectDto project) {
        // Only users with PRJ_CREATE permission can access this
        return projectService.createProject(project);
    }
    
    @PreAuthorize("hasPermission(#id, 'Project', 'PRJ_READ')")
    @GetMapping("/{id}")
    public Mono<ProjectDto> getProject(@PathVariable String id) {
        // Check permission for specific project
        return projectService.getProject(id);
    }
}
```

## Permission System

### Roles Hierarchy

```
ROLE_ADMIN (all permissions)
└── ROLE_PROJECT_MANAGER
    └── ROLE_DEVELOPER  
        └── ROLE_USER (basic permissions)
```

### Permission Categories

#### User Service Permissions
- `USER_SELF_READ` - Read own profile
- `USER_SELF_UPDATE` - Update own profile
- `USER_READ` - Read any user (admin only)
- `USER_CREATE` - Create new users (admin only)
- `USER_DELETE` - Delete users (admin only)
- `USER_ROLE_GRANT` - Assign/revoke roles (admin only)

#### Project Service Permissions
- `PRJ_CREATE` - Create new projects
- `PRJ_READ` - View project details
- `PRJ_UPDATE` - Modify project settings
- `PRJ_DELETE` - Delete projects
- `PRJ_ARCHIVE` - Archive/unarchive projects
- `PRJ_MEMBER_ADD` - Add members to projects
- `PRJ_MEMBER_REMOVE` - Remove members from projects

#### Task Service Permissions
- `TASK_CREATE` - Create new tasks
- `TASK_READ` - View task details
- `TASK_UPDATE` - Edit task fields
- `TASK_STATUS_CHANGE` - Change task status
- `TASK_PRIORITY_CHANGE` - Change task priority
- `TASK_ASSIGN` - Assign/reassign tasks
- `TASK_DELETE` - Delete tasks

#### Comment Service Permissions
- `CMT_CREATE` - Post new comments
- `CMT_UPDATE_OWN` - Edit own comments
- `CMT_DELETE_OWN` - Delete own comments
- `CMT_DELETE_ANY` - Delete any comment (moderator)

#### Notification Service Permissions
- `NOTI_READ` - Read notifications
- `NOTI_MARK_READ` - Mark notifications as read

## Configuration

### Default Configuration (permissions.yml)

```yaml
rbac:
  roles:
    ROLE_USER:
      - USER_SELF_READ
      - USER_SELF_UPDATE
      - CMT_CREATE
      - CMT_UPDATE_OWN
      - CMT_DELETE_OWN
      - NOTI_READ
      - NOTI_MARK_READ
      
    ROLE_DEVELOPER:
      # Inherits all ROLE_USER permissions plus:
      - PRJ_READ
      - TASK_CREATE
      - TASK_READ
      - TASK_UPDATE
      - TASK_STATUS_CHANGE
      - TASK_PRIORITY_CHANGE
      
    ROLE_PROJECT_MANAGER:
      # Inherits all ROLE_DEVELOPER permissions plus:
      - PRJ_CREATE
      - PRJ_UPDATE
      - PRJ_DELETE
      - PRJ_ARCHIVE
      - PRJ_MEMBER_ADD
      - PRJ_MEMBER_REMOVE
      - TASK_ASSIGN
      - TASK_DELETE
      - CMT_DELETE_ANY

    ROLE_ADMIN:
      - "**"  # Wildcard - all permissions
```

### Custom Configuration

Override the default configuration by providing your own `permissions.yml` in your application's `src/main/resources`:

```yaml
rbac:
  roles:
    ROLE_CUSTOM:
      - CUSTOM_PERMISSION_1
      - CUSTOM_PERMISSION_2
```

## Usage Examples

### Method-Level Security

```java
@Service
@RequiredArgsConstructor
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    
    @PreAuthorize("hasPermission(null, 'PRJ_CREATE')")
    public Mono<Project> createProject(ProjectDto projectDto) {
        // Implementation
    }
    
    @PreAuthorize("hasPermission(#projectId, 'Project', 'PRJ_UPDATE')")
    public Mono<Project> updateProject(String projectId, ProjectDto projectDto) {
        // Implementation
    }
    
    @PostAuthorize("hasPermission(returnObject, 'PRJ_READ')")
    public Mono<Project> getProject(String projectId) {
        // Implementation
    }
}
```

### Programmatic Permission Checking

```java
@Service
@RequiredArgsConstructor
public class CustomSecurityService {
    
    private final PermissionEvaluator permissionEvaluator;
    
    public boolean canUserAccessProject(Collection<String> userRoles, String projectId) {
        return permissionEvaluator.hasPermission(userRoles, Action.PRJ_READ);
    }
    
    public boolean canUserPerformAction(Collection<String> userRoles, Action action) {
        return permissionEvaluator.hasPermission(userRoles, action);
    }
}
```

### Custom Permission Evaluators

For domain-specific permission logic, implement your own evaluators:

```java
@Component
public class ProjectPermissionEvaluator implements PermissionEvaluator {
    
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (targetDomainObject instanceof Project project) {
            String userId = authentication.getName();
            return switch (permission.toString()) {
                case "EDIT" -> project.getOwnerId().equals(userId) || project.getMemberIds().contains(userId);
                case "VIEW" -> project.isPublic() || project.getMemberIds().contains(userId);
                default -> false;
            };
        }
        return false;
    }
    
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, 
                               String targetType, Object permission) {
        // Implementation for ID-based checks
        return false;
    }
}
```

## Integration with Microservices

### 1. User Service Integration

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @PreAuthorize("hasPermission(null, 'USER_CREATE')")
    @PostMapping
    public Mono<UserDto> createUser(@RequestBody UserDto user) {
        return userService.createUser(user);
    }
    
    @PreAuthorize("hasPermission(#id, 'User', 'USER_READ') or authentication.name == #id")
    @GetMapping("/{id}")
    public Mono<UserDto> getUser(@PathVariable String id) {
        return userService.getUser(id);
    }
}
```

### 2. Project Service Integration

```java
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    
    @PreAuthorize("hasPermission(null, 'PRJ_CREATE')")
    @PostMapping
    public Mono<ProjectDto> createProject(@RequestBody ProjectDto project) {
        return projectService.createProject(project);
    }
    
    @PreAuthorize("hasPermission(#id, 'Project', 'PRJ_DELETE')")
    @DeleteMapping("/{id}")
    public Mono<Void> deleteProject(@PathVariable String id) {
        return projectService.deleteProject(id);
    }
}
```

### 3. Task Service Integration

```java
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    @PreAuthorize("hasPermission(null, 'TASK_CREATE')")
    @PostMapping
    public Mono<TaskDto> createTask(@RequestBody TaskDto task) {
        return taskService.createTask(task);
    }
    
    @PreAuthorize("hasPermission(#id, 'Task', 'TASK_ASSIGN')")
    @PutMapping("/{id}/assign")
    public Mono<TaskDto> assignTask(@PathVariable String id, @RequestBody AssignTaskDto assignTask) {
        return taskService.assignTask(id, assignTask);
    }
}
```

## Testing

The module includes comprehensive tests demonstrating all functionality:

```java
@Test
void testUserRolePermissions() {
    List<String> userRoles = List.of("ROLE_USER");
    
    // User should be able to read/update own profile
    assertThat(permissionEvaluator.hasPermission(userRoles, Action.USER_SELF_READ)).isTrue();
    assertThat(permissionEvaluator.hasPermission(userRoles, Action.USER_SELF_UPDATE)).isTrue();
    
    // User should NOT be able to create projects
    assertThat(permissionEvaluator.hasPermission(userRoles, Action.PRJ_CREATE)).isFalse();
}
```

## Architecture

### Components

1. **PermissionEvaluator**: Core RBAC logic for permission checking
2. **CustomPermissionEvaluator**: Spring Security integration
3. **RbacConfigurationProperties**: YAML configuration binding
4. **YamlPropertySourceFactory**: YAML file loading support
5. **RbacAutoConfiguration**: Spring Boot auto-configuration
6. **Action**: Enum defining all available permissions

### Design Principles

- **Principle of Least Privilege**: Users get minimum necessary permissions
- **Role Hierarchy**: Higher roles inherit lower role permissions
- **Separation of Concerns**: Domain logic separate from security logic
- **Configuration Externalization**: Permissions defined in YAML files
- **Spring Integration**: Seamless integration with Spring Security

## Best Practices

1. **Use Method-Level Security**: Prefer `@PreAuthorize` over programmatic checks
2. **Granular Permissions**: Define specific permissions rather than broad access
3. **Test Security**: Write comprehensive tests for permission scenarios
4. **Audit Access**: Log permission checks for security auditing
5. **Custom Evaluators**: Implement domain-specific permission logic where needed

## Troubleshooting

### Common Issues

1. **Permission Denied**: Check if user has required role and permission is defined in YAML
2. **YAML Loading Issues**: Ensure YAML syntax is correct and file is in classpath
3. **Auto-Configuration Not Working**: Verify Spring Boot version compatibility
4. **Method Security Not Applied**: Ensure `@EnableReactiveMethodSecurity` is configured

### Debug Logging

Enable debug logging to troubleshoot permission issues:

```properties
logging.level.com.pm.commonsecurity=DEBUG
```

This will log all permission checks with detailed information about user roles, requested permissions, and evaluation results.
