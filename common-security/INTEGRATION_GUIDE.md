# RBAC Integration Guide for Microservices

This guide provides step-by-step instructions for integrating the common-security RBAC system into each microservice.

## Table of Contents
1. [Common Integration Steps](#common-integration-steps)
2. [User Service Integration](#user-service-integration)
3. [Project Service Integration](#project-service-integration)
4. [Task Service Integration](#task-service-integration)
5. [Comment Service Integration](#comment-service-integration)
6. [Notification Service Integration](#notification-service-integration)
7. [API Gateway Integration](#api-gateway-integration)

## Common Integration Steps

### 1. Add Dependency

Add the common-security dependency to each microservice's `pom.xml`:

```xml
<dependency>
    <groupId>com.pm</groupId>
    <artifactId>common-security</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Update Security Configuration

Modify the existing `SecurityConfig` class to enable method security:

```java
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity  // Add this annotation
public class SecurityConfig {
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**").permitAll()
                .anyExchange().authenticated()
            )
            .build();
    }
    
    // Add this bean to enable custom permission evaluation
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
```

### 3. User Context Setup

Create a user context filter to extract user information from headers:

```java
@Component
public class UserContextWebFilter implements WebFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String userRoles = exchange.getRequest().getHeaders().getFirst("X-User-Roles");
        
        if (userId != null && userRoles != null) {
            List<SimpleGrantedAuthority> authorities = Arrays.stream(userRoles.split(","))
                .map(String::trim)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
                
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
                
            return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
        }
        
        return chain.filter(exchange);
    }
}
```

---

## User Service Integration

### Required Permissions
- `USER_SELF_READ`, `USER_SELF_UPDATE` - For profile management
- `USER_READ`, `USER_CREATE`, `USER_DELETE`, `USER_ROLE_GRANT` - For admin operations

### Controller Updates

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    // Self-service operations (available to all authenticated users)
    @PreAuthorize("hasPermission(null, 'USER_SELF_READ')")
    @GetMapping("/me")
    public Mono<UserDto> getCurrentUser(Authentication auth) {
        return userService.getUserById(auth.getName());
    }
    
    @PreAuthorize("hasPermission(null, 'USER_SELF_UPDATE')")
    @PutMapping("/me")
    public Mono<UserDto> updateCurrentUser(@RequestBody UserDto userDto, Authentication auth) {
        return userService.updateUser(auth.getName(), userDto);
    }
    
    @PreAuthorize("hasPermission(null, 'USER_SELF_UPDATE')")
    @PutMapping("/me/password")
    public Mono<Void> changePassword(@RequestBody ChangePasswordDto dto, Authentication auth) {
        return userService.changePassword(auth.getName(), dto);
    }
    
    // Admin operations
    @PreAuthorize("hasPermission(null, 'USER_READ')")
    @GetMapping
    public Flux<UserDto> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return userService.getAllUsers(page, size);
    }
    
    @PreAuthorize("hasPermission(null, 'USER_READ') or authentication.name == #userId")
    @GetMapping("/{userId}")
    public Mono<UserDto> getUserById(@PathVariable String userId) {
        return userService.getUserById(userId);
    }
    
    @PreAuthorize("hasPermission(null, 'USER_CREATE')")
    @PostMapping
    public Mono<UserDto> createUser(@Valid @RequestBody CreateUserDto createUserDto) {
        return userService.createUser(createUserDto);
    }
    
    @PreAuthorize("hasPermission(null, 'USER_DELETE')")
    @DeleteMapping("/{userId}")
    public Mono<Void> deleteUser(@PathVariable String userId) {
        return userService.deleteUser(userId);
    }
    
    @PreAuthorize("hasPermission(null, 'USER_ROLE_GRANT')")
    @PostMapping("/{userId}/roles")
    public Mono<UserDto> grantRole(@PathVariable String userId, @RequestBody GrantRoleDto dto) {
        return userService.grantRole(userId, dto.getRole());
    }
    
    @PreAuthorize("hasPermission(null, 'USER_ROLE_GRANT')")
    @DeleteMapping("/{userId}/roles/{role}")
    public Mono<UserDto> revokeRole(@PathVariable String userId, @PathVariable String role) {
        return userService.revokeRole(userId, role);
    }
}
```

---

## Project Service Integration

### Required Permissions
- `PRJ_READ` - View projects
- `PRJ_CREATE`, `PRJ_UPDATE`, `PRJ_DELETE`, `PRJ_ARCHIVE` - Project management
- `PRJ_MEMBER_ADD`, `PRJ_MEMBER_REMOVE` - Member management

### Custom Permission Evaluator

```java
@Component
@RequiredArgsConstructor
public class ProjectPermissionEvaluator implements PermissionEvaluator {
    
    private final ProjectRepository projectRepository;
    
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (targetDomainObject instanceof Project project) {
            String userId = authentication.getName();
            return evaluateProjectPermission(project, userId, permission.toString());
        }
        return false;
    }
    
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, 
                               String targetType, Object permission) {
        if ("Project".equals(targetType)) {
            String userId = authentication.getName();
            return projectRepository.findById(targetId.toString())
                .map(project -> evaluateProjectPermission(project, userId, permission.toString()))
                .defaultIfEmpty(false)
                .block(); // Consider making this reactive
        }
        return false;
    }
    
    private boolean evaluateProjectPermission(Project project, String userId, String permission) {
        return switch (permission) {
            case "VIEW" -> project.isPublic() || 
                          project.getOwnerId().equals(userId) || 
                          project.getMemberIds().contains(userId);
            case "EDIT", "DELETE" -> project.getOwnerId().equals(userId);
            case "MANAGE_MEMBERS" -> project.getOwnerId().equals(userId);
            default -> false;
        };
    }
}
```

### Controller Updates

```java
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    
    private final ProjectService projectService;
    
    @PreAuthorize("hasPermission(null, 'PRJ_CREATE')")
    @PostMapping
    public Mono<ProjectDto> createProject(@Valid @RequestBody ProjectDto projectDto, 
                                        Authentication auth) {
        return projectService.createProject(projectDto, auth.getName());
    }
    
    @PreAuthorize("hasPermission(null, 'PRJ_READ')")
    @GetMapping
    public Flux<ProjectDto> getAllProjects() {
        return projectService.getAllProjects();
    }
    
    @PreAuthorize("hasPermission(#projectId, 'Project', 'VIEW')")
    @GetMapping("/{projectId}")
    public Mono<ProjectDto> getProject(@PathVariable String projectId) {
        return projectService.getProjectById(projectId);
    }
    
    @PreAuthorize("hasPermission(#projectId, 'Project', 'EDIT') or hasPermission(null, 'PRJ_UPDATE')")
    @PutMapping("/{projectId}")
    public Mono<ProjectDto> updateProject(@PathVariable String projectId, 
                                        @Valid @RequestBody ProjectDto projectDto) {
        return projectService.updateProject(projectId, projectDto);
    }
    
    @PreAuthorize("hasPermission(#projectId, 'Project', 'DELETE') or hasPermission(null, 'PRJ_DELETE')")
    @DeleteMapping("/{projectId}")
    public Mono<Void> deleteProject(@PathVariable String projectId) {
        return projectService.deleteProject(projectId);
    }
    
    @PreAuthorize("hasPermission(#projectId, 'Project', 'MANAGE_MEMBERS') or hasPermission(null, 'PRJ_MEMBER_ADD')")
    @PostMapping("/{projectId}/members")
    public Mono<ProjectDto> addMember(@PathVariable String projectId, 
                                    @RequestBody AddMemberDto dto) {
        return projectService.addMember(projectId, dto.getUserId(), dto.getRole());
    }
    
    @PreAuthorize("hasPermission(#projectId, 'Project', 'MANAGE_MEMBERS') or hasPermission(null, 'PRJ_MEMBER_REMOVE')")
    @DeleteMapping("/{projectId}/members/{userId}")
    public Mono<ProjectDto> removeMember(@PathVariable String projectId, 
                                       @PathVariable String userId) {
        return projectService.removeMember(projectId, userId);
    }
}
```

---

## Task Service Integration

### Required Permissions
- `TASK_CREATE`, `TASK_READ`, `TASK_UPDATE` - Basic task operations
- `TASK_STATUS_CHANGE`, `TASK_PRIORITY_CHANGE` - Status management
- `TASK_ASSIGN`, `TASK_DELETE` - Advanced operations

### Custom Permission Evaluator

```java
@Component
@RequiredArgsConstructor
public class TaskPermissionEvaluator implements PermissionEvaluator {
    
    private final TaskRepository taskRepository;
    private final ProjectService projectService; // To check project membership
    
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (targetDomainObject instanceof Task task) {
            String userId = authentication.getName();
            return evaluateTaskPermission(task, userId, permission.toString());
        }
        return false;
    }
    
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, 
                               String targetType, Object permission) {
        if ("Task".equals(targetType)) {
            String userId = authentication.getName();
            return taskRepository.findById(targetId.toString())
                .map(task -> evaluateTaskPermission(task, userId, permission.toString()))
                .defaultIfEmpty(false)
                .block();
        }
        return false;
    }
    
    private boolean evaluateTaskPermission(Task task, String userId, String permission) {
        boolean isAssignee = task.getAssigneeId().equals(userId);
        boolean isCreator = task.getCreatedBy().equals(userId);
        boolean isProjectMember = projectService.isProjectMember(task.getProjectId(), userId).block();
        
        return switch (permission) {
            case "VIEW" -> isAssignee || isCreator || isProjectMember;
            case "EDIT" -> isAssignee || isCreator;
            case "STATUS_CHANGE" -> isAssignee || isCreator || isProjectMember;
            case "DELETE" -> isCreator;
            default -> false;
        };
    }
}
```

### Controller Updates

```java
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    
    private final TaskService taskService;
    
    @PreAuthorize("hasPermission(null, 'TASK_CREATE')")
    @PostMapping
    public Mono<TaskDto> createTask(@Valid @RequestBody TaskDto taskDto, Authentication auth) {
        return taskService.createTask(taskDto, auth.getName());
    }
    
    @PreAuthorize("hasPermission(null, 'TASK_READ')")
    @GetMapping
    public Flux<TaskDto> getTasks(@RequestParam(required = false) String projectId,
                                @RequestParam(required = false) String assigneeId) {
        if (projectId != null) {
            return taskService.getTasksByProject(projectId);
        } else if (assigneeId != null) {
            return taskService.getTasksByAssignee(assigneeId);
        }
        return taskService.getAllTasks();
    }
    
    @PreAuthorize("hasPermission(#taskId, 'Task', 'VIEW')")
    @GetMapping("/{taskId}")
    public Mono<TaskDto> getTask(@PathVariable String taskId) {
        return taskService.getTaskById(taskId);
    }
    
    @PreAuthorize("hasPermission(#taskId, 'Task', 'EDIT') or hasPermission(null, 'TASK_UPDATE')")
    @PutMapping("/{taskId}")
    public Mono<TaskDto> updateTask(@PathVariable String taskId, 
                                  @Valid @RequestBody TaskDto taskDto) {
        return taskService.updateTask(taskId, taskDto);
    }
    
    @PreAuthorize("hasPermission(#taskId, 'Task', 'STATUS_CHANGE') or hasPermission(null, 'TASK_STATUS_CHANGE')")
    @PutMapping("/{taskId}/status")
    public Mono<TaskDto> updateTaskStatus(@PathVariable String taskId, 
                                        @RequestBody UpdateStatusDto dto) {
        return taskService.updateTaskStatus(taskId, dto.getStatus());
    }
    
    @PreAuthorize("hasPermission(null, 'TASK_ASSIGN')")
    @PutMapping("/{taskId}/assign")
    public Mono<TaskDto> assignTask(@PathVariable String taskId, 
                                  @RequestBody AssignTaskDto dto) {
        return taskService.assignTask(taskId, dto.getAssigneeId());
    }
    
    @PreAuthorize("hasPermission(#taskId, 'Task', 'DELETE') or hasPermission(null, 'TASK_DELETE')")
    @DeleteMapping("/{taskId}")
    public Mono<Void> deleteTask(@PathVariable String taskId) {
        return taskService.deleteTask(taskId);
    }
}
```

---

## Comment Service Integration

### Required Permissions
- `CMT_CREATE` - Post comments
- `CMT_UPDATE_OWN`, `CMT_DELETE_OWN` - Manage own comments
- `CMT_DELETE_ANY` - Moderator actions

### Controller Updates

```java
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    
    @PreAuthorize("hasPermission(null, 'CMT_CREATE')")
    @PostMapping
    public Mono<CommentDto> createComment(@Valid @RequestBody CommentDto commentDto, 
                                        Authentication auth) {
        return commentService.createComment(commentDto, auth.getName());
    }
    
    @GetMapping
    public Flux<CommentDto> getComments(@RequestParam(required = false) String entityId,
                                      @RequestParam(required = false) String entityType) {
        if (entityId != null && entityType != null) {
            return commentService.getCommentsByEntity(entityType, entityId);
        }
        return commentService.getAllComments();
    }
    
    @PreAuthorize("hasPermission(#commentId, 'Comment', 'UPDATE_OWN') or hasPermission(null, 'CMT_UPDATE_OWN')")
    @PutMapping("/{commentId}")
    public Mono<CommentDto> updateComment(@PathVariable String commentId, 
                                        @Valid @RequestBody CommentDto commentDto,
                                        Authentication auth) {
        return commentService.updateComment(commentId, commentDto, auth.getName());
    }
    
    @PreAuthorize("hasPermission(#commentId, 'Comment', 'DELETE_OWN') or hasPermission(null, 'CMT_DELETE_OWN')")
    @DeleteMapping("/{commentId}")
    public Mono<Void> deleteOwnComment(@PathVariable String commentId, Authentication auth) {
        return commentService.deleteComment(commentId, auth.getName(), false);
    }
    
    @PreAuthorize("hasPermission(null, 'CMT_DELETE_ANY')")
    @DeleteMapping("/admin/{commentId}")
    public Mono<Void> deleteAnyComment(@PathVariable String commentId) {
        return commentService.deleteComment(commentId, null, true);
    }
}
```

---

## Notification Service Integration

### Required Permissions
- `NOTI_READ` - Read notifications
- `NOTI_MARK_READ` - Mark as read

### Controller Updates

```java
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @PreAuthorize("hasPermission(null, 'NOTI_READ')")
    @GetMapping
    public Flux<NotificationDto> getNotifications(Authentication auth,
                                                @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return notificationService.getNotificationsForUser(auth.getName(), unreadOnly);
    }
    
    @PreAuthorize("hasPermission(null, 'NOTI_READ')")
    @GetMapping("/{notificationId}")
    public Mono<NotificationDto> getNotification(@PathVariable String notificationId,
                                               Authentication auth) {
        return notificationService.getNotification(notificationId, auth.getName());
    }
    
    @PreAuthorize("hasPermission(null, 'NOTI_MARK_READ')")
    @PutMapping("/{notificationId}/read")
    public Mono<NotificationDto> markAsRead(@PathVariable String notificationId,
                                          Authentication auth) {
        return notificationService.markAsRead(notificationId, auth.getName());
    }
    
    @PreAuthorize("hasPermission(null, 'NOTI_MARK_READ')")
    @PutMapping("/read-all")
    public Mono<Void> markAllAsRead(Authentication auth) {
        return notificationService.markAllAsRead(auth.getName());
    }
}
```

---

## API Gateway Integration

### JWT Token Processing

Update the API Gateway to extract user information from JWT tokens and forward as headers:

```java
@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String token = extractToken(exchange.getRequest());
            
            if (token != null) {
                try {
                    Claims claims = Jwts.parser()
                        .setSigningKey(config.getJwtSecret())
                        .parseClaimsJws(token)
                        .getBody();
                        
                    String userId = claims.getSubject();
                    String roles = claims.get("roles", String.class);
                    
                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Roles", roles)
                        .build();
                        
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                } catch (Exception e) {
                    return onError(exchange, HttpStatus.UNAUTHORIZED);
                }
            }
            
            return chain.filter(exchange);
        };
    }
    
    // Helper methods...
}
```

### Configuration

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://user-service:8081
          predicates:
            - Path=/api/users/**
          filters:
            - name: Authentication
              args:
                jwt-secret: ${JWT_SECRET:default-secret}
```

---

## Testing Integration

### Integration Test Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "rbac.roles.ROLE_TEST_USER[0]=USER_SELF_READ",
    "rbac.roles.ROLE_TEST_USER[1]=USER_SELF_UPDATE"
})
class RbacIntegrationTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Test
    void testUserCanAccessOwnProfile() {
        webTestClient.get()
            .uri("/api/users/me")
            .header("X-User-Id", "user123")
            .header("X-User-Roles", "ROLE_TEST_USER")
            .exchange()
            .expectStatus().isOk();
    }
    
    @Test
    void testUserCannotAccessAdminEndpoint() {
        webTestClient.get()
            .uri("/api/users")
            .header("X-User-Id", "user123")
            .header("X-User-Roles", "ROLE_TEST_USER")
            .exchange()
            .expectStatus().isForbidden();
    }
}
```

This integration guide provides a comprehensive approach to implementing RBAC across all microservices while maintaining security best practices and proper separation of concerns.
