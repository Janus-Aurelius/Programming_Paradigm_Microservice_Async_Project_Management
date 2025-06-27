package com.pm.taskservice.controller;

// Import DTO from the SHARED module
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.pm.commoncontracts.dto.TaskDto;
import com.pm.commoncontracts.requestDto.task.UpdateTaskPriorityRequestDto;
import com.pm.commoncontracts.requestDto.task.UpdateTaskStatusRequestDto;
import com.pm.taskservice.exception.ConflictException;
import com.pm.taskservice.exception.ResourceNotFoundException;
import com.pm.taskservice.security.TaskPermissionEvaluator;
import com.pm.taskservice.service.TaskService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tasks") // Consistent API prefix is good practice
@RequiredArgsConstructor // Handles constructor injection for final fields
@Slf4j
public class TaskController {

    // No @Autowired needed here due to @RequiredArgsConstructor
    private final TaskService taskService;
    private final TaskPermissionEvaluator taskPermissionEvaluator;

    // REMOVED: KafkaProducerTemplate - Service layer handles events
    private String extractUserIdFromHeader(ServerHttpRequest request) {
        return request.getHeaders().getFirst("X-User-Id");
    }

    @PostMapping
    public Mono<ResponseEntity<TaskDto>> createTask(
            @Valid @RequestBody TaskDto taskDto, // Add @Valid for validation
            UriComponentsBuilder uriBuilder,
            ServerHttpRequest request,
            Authentication authentication
    ) {
        log.info("Received request to create task: {}", taskDto);
        String userId = extractUserIdFromHeader(request);
        taskDto.setCreatedBy(userId);

        // Check permission reactively
        return taskPermissionEvaluator.hasGeneralPermission(authentication, "TASK_CREATE")
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return Mono.error(new AccessDeniedException("Access denied: insufficient permissions to create task"));
                    }

                    // Service handles saving AND publishing event
                    return taskService.createTask(taskDto)
                            .map(createdTask -> {
                                URI location = uriBuilder.path("/api/tasks/{id}")
                                        .buildAndExpand(createdTask.getId()) // Assuming TaskDto has id()
                                        .toUri();
                                log.info("Task created successfully with ID: {}", createdTask.getId());
                                return ResponseEntity.created(location).body(createdTask); // Return 201 Created
                            });
                });
    }

    @GetMapping
    public Flux<TaskDto> getAllTasks(Authentication authentication) {
        log.info("Received request to get all tasks");

        // Check permission reactively
        return taskPermissionEvaluator.hasGeneralPermission(authentication, "TASK_READ")
                .flatMapMany(hasPermission -> {
                    if (!hasPermission) {
                        return Flux.error(new AccessDeniedException("Access denied: insufficient permissions to read tasks"));
                    }

                    return taskService.getTasks(); // Return Flux directly
                });
    }

    @GetMapping("/my-tasks")
    public Flux<TaskDto> getMyTasks(
            ServerHttpRequest request,
            Authentication authentication
    ) {
        log.info("Received request to get tasks for current user");
        String userId = extractUserIdFromHeader(request);

        if (userId == null || userId.trim().isEmpty()) {
            log.warn("No user ID found in X-User-Id header");
            return Flux.error(new AccessDeniedException("User ID not found in request headers"));
        }

        log.info("Getting tasks for user ID: {}", userId);

        // Check permission reactively
        return taskPermissionEvaluator.hasGeneralPermission(authentication, "TASK_READ")
                .flatMapMany(hasPermission -> {
                    if (!hasPermission) {
                        log.warn("Access denied for user {}: insufficient permissions to read tasks", userId);
                        return Flux.error(new AccessDeniedException("Access denied: insufficient permissions to read tasks"));
                    }

                    log.info("Permission granted - fetching tasks for user: {}", userId);
                    return taskService.getTasksByAssigneeId(userId);
                });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<TaskDto>> getTaskById(@PathVariable String id, Authentication authentication) {
        log.info("Received request to get task by ID: {}", id);

        // Check permission reactively using taskPermissionEvaluator
        return taskPermissionEvaluator.hasPermission(authentication, id, "TASK_READ")
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return Mono.error(new AccessDeniedException("Access denied: insufficient permissions to read this task"));
                    }
                    return taskService.getTaskById(id)
                            .map(ResponseEntity::ok);
                })
                .onErrorResume(e -> {
                    if (e instanceof AccessDeniedException) {
                        return Mono.error(e);
                    }
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @GetMapping(params = "projectId")
    public Flux<TaskDto> getTasksByProjectId(@RequestParam String projectId, Authentication authentication) {
        log.info("Received request to get tasks for project ID: {}", projectId);
        log.info("Authentication object: {}", authentication);
        log.info("Is authenticated: {}", authentication != null ? authentication.isAuthenticated() : "null");

        if (authentication != null) {
            log.info("Authentication principal: {}", authentication.getPrincipal());
            log.info("Authentication authorities: {}", authentication.getAuthorities());
            log.info("Authentication name: {}", authentication.getName());
        }

        // For project-specific queries, use general TASK_READ permission
        // If unauthenticated (e.g., during local development), allow read-only access
        if (authentication == null || !authentication.isAuthenticated()) {
            log.info("No authentication or not authenticated - allowing read access");
            return taskService.getTasksByProjectId(projectId);
        }

        log.info("Checking TASK_READ permission for authenticated user");
        return taskPermissionEvaluator.hasGeneralPermission(authentication, "TASK_READ")
                .doOnNext(hasPermission -> log.info("Permission check result: {}", hasPermission))
                .flatMapMany(hasPermission -> {
                    if (!hasPermission) {
                        log.warn("Access denied for user: insufficient permissions to read tasks");
                        return Flux.error(new AccessDeniedException("Access denied: insufficient permissions to read tasks"));
                    }

                    log.info("Permission granted - fetching tasks for project: {}", projectId);
                    return taskService.getTasksByProjectId(projectId);
                });
    }

    @GetMapping(params = "assignedTo")
    public Flux<TaskDto> getTasksByAssigneeId(@RequestParam String assignedTo, Authentication authentication) {
        log.info("Received request to get tasks assigned to: {}", assignedTo);

        // Check permission reactively
        return taskPermissionEvaluator.hasGeneralPermission(authentication, "TASK_READ")
                .flatMapMany(hasPermission -> {
                    if (!hasPermission) {
                        return Flux.error(new AccessDeniedException("Access denied: insufficient permissions to read tasks"));
                    }

                    return taskService.getTasksByAssigneeId(assignedTo);
                });
    }

    @PatchMapping("/{id}/status")
    public Mono<ResponseEntity<TaskDto>> updateTaskStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateTaskStatusRequestDto request,
            ServerHttpRequest httpRequest,
            Authentication authentication
    ) {
        String userId = extractUserIdFromHeader(httpRequest);

        // Check permission reactively using taskPermissionEvaluator
        return taskPermissionEvaluator.hasPermission(authentication, id, "TASK_STATUS_CHANGE")
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return Mono.error(new AccessDeniedException("Access denied: insufficient permissions to change task status"));
                    }
                    return taskService.updateTaskStatus(id, request.getNewStatus(),
                            request.getExpectedVersion(), userId)
                            .map(ResponseEntity::ok); // Let ConflictException bubble up
                })
                .onErrorResume(e -> {
                    if (e instanceof AccessDeniedException) {
                        return Mono.error(e);
                    }
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<TaskDto>> updateTaskCombined(
            @PathVariable String id,
            @Valid @RequestBody TaskDto taskDto,
            ServerHttpRequest httpRequest,
            Authentication authentication
    ) {
        String userId = extractUserIdFromHeader(httpRequest);

        // Check permission reactively using taskPermissionEvaluator
        return taskPermissionEvaluator.hasPermission(authentication, id, "TASK_UPDATE")
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return Mono.error(new AccessDeniedException("Access denied: insufficient permissions to update task"));
                    }
                    return taskService.updateTaskCombined(id, taskDto, userId)
                            .map(ResponseEntity::ok); // Let ConflictException bubble up
                })
                .onErrorResume(e -> {
                    if (e instanceof AccessDeniedException) {
                        return Mono.error(e);
                    }
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @PutMapping("/{id}/priority")
    public Mono<ResponseEntity<TaskDto>> updateTaskPriority(
            @PathVariable String id,
            @Valid @RequestBody UpdateTaskPriorityRequestDto request,
            ServerHttpRequest httpRequest,
            Authentication authentication
    ) {
        String userId = extractUserIdFromHeader(httpRequest);

        // Check permission reactively using taskPermissionEvaluator
        return taskPermissionEvaluator.hasPermission(authentication, id, "TASK_PRIORITY_CHANGE")
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return Mono.error(new AccessDeniedException("Access denied: insufficient permissions to change task priority"));
                    }
                    return taskService.updateTaskPriority(
                            id,
                            request.getNewPriority(),
                            request.getExpectedVersion(),
                            userId
                    )
                            .map(ResponseEntity::ok); // Let ConflictException bubble up
                })
                .onErrorResume(e -> {
                    if (e instanceof AccessDeniedException) {
                        return Mono.error(e);
                    }
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTask(@PathVariable String id, Authentication authentication) {
        log.info("Received request to delete task ID: {}", id);

        // Check permission reactively using taskPermissionEvaluator
        return taskPermissionEvaluator.hasPermission(authentication, id, "TASK_DELETE")
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return Mono.error(new AccessDeniedException("Access denied: insufficient permissions to delete task"));
                    }

                    return taskService.deleteTask(id);
                })
                .onErrorResume(e -> {
                    if (e instanceof AccessDeniedException) {
                        return Mono.error(e);
                    }
                    return Mono.error(new ResourceNotFoundException("Task not found with id: " + id));
                });
    }

    @GetMapping("/{id}/permissions/check")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkTaskPermissions(
            @PathVariable String id,
            @RequestParam String action,
            Authentication authentication) {
        log.info("Checking permissions for task ID: {} with action: {}", id, action);

        return taskPermissionEvaluator.hasPermission(authentication, id, action)
                .map(hasAccess -> {
                    Map<String, Boolean> permissions = new HashMap<>();
                    permissions.put("hasAccess", hasAccess);
                    return ResponseEntity.ok(permissions);
                })
                .onErrorResume(e -> {
                    log.error("Error checking permissions for task {}: {}", id, e.getMessage());
                    Map<String, Boolean> permissions = new HashMap<>();
                    permissions.put("hasAccess", false);
                    return Mono.just(ResponseEntity.ok(permissions));
                })
                .switchIfEmpty(Mono.fromCallable(() -> {
                    Map<String, Boolean> permissions = new HashMap<>();
                    permissions.put("hasAccess", false);
                    return ResponseEntity.ok(permissions);
                }));
    }

}
