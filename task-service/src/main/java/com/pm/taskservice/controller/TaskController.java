package com.pm.taskservice.controller;

// Import DTO from the SHARED module
import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    // REMOVED: KafkaProducerTemplate - Service layer handles events

    private String extractUserIdFromHeader(ServerHttpRequest request) {
        return request.getHeaders().getFirst("X-User-Id");
    }    @PreAuthorize("hasPermission(null, 'TASK_CREATE')")
    @PostMapping
    public Mono<ResponseEntity<TaskDto>> createTask(
            @Valid @RequestBody TaskDto taskDto, // Add @Valid for validation
            UriComponentsBuilder uriBuilder,
            ServerHttpRequest request
    ) {
        log.info("Received request to create task: {}", taskDto); // Log includes CorrID via MDC filter if configured
        String userId = extractUserIdFromHeader(request);
        taskDto.setCreatedBy(userId);
        // Service handles saving AND publishing event
        return taskService.createTask(taskDto)
                .map(createdTask -> {
                    URI location = uriBuilder.path("/api/tasks/{id}")
                            .buildAndExpand(createdTask.getId()) // Assuming TaskDto has id()
                            .toUri();
                    log.info("Task created successfully with ID: {}", createdTask.getId());
                                        return ResponseEntity.created(location).body(createdTask); // Return 201 Created
                });
    }    @PreAuthorize("hasPermission(null, 'TASK_READ')")
    @GetMapping
    public Flux<TaskDto> getAllTasks() {
        log.info("Received request to get all tasks");
        return taskService.getTasks(); // Return Flux directly
    }    @PreAuthorize("hasPermission(#id, 'Task', 'TASK_READ')")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<TaskDto>> getTaskById(@PathVariable String id) {
        log.info("Received request to get task by ID: {}", id);
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok) // If found, wrap in 200 OK
                .defaultIfEmpty(ResponseEntity.notFound().build()); // If service returns empty Mono, return 404
        // Note: This .defaultIfEmpty can be removed if the service throws ResourceNotFoundException handled globally
    }    @PreAuthorize("hasPermission(#projectId, 'Project', 'PRJ_READ')")
    @GetMapping(params = "projectId")
    public Flux<TaskDto> getTasksByProjectId(@RequestParam String projectId) {
        log.info("Received request to get tasks for project ID: {}", projectId);
        return taskService.getTasksByProjectId(projectId);
    }    @PreAuthorize("hasPermission(null, 'TASK_READ')")
    @GetMapping(params = "assignedTo")
    public Flux<TaskDto> getTasksByAssigneeId(@RequestParam String assignedTo) {
        log.info("Received request to get tasks assigned to: {}", assignedTo);
        return taskService.getTasksByAssigneeId(assignedTo);
    }    @PreAuthorize("hasPermission(#id, 'Task', 'TASK_STATUS_CHANGE')")
    @PutMapping("/{id}/status")
    public Mono<ResponseEntity<TaskDto>> updateTaskStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateTaskStatusRequestDto request,
            ServerHttpRequest httpRequest
    ) {
        String userId = extractUserIdFromHeader(httpRequest);
        return taskService.updateTaskStatus(id, request.getNewStatus(),
                        request.getExpectedVersion(), userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(ConflictException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
                );
    }    @PreAuthorize("hasPermission(#id, 'Task', 'TASK_UPDATE')")
    @PutMapping("/{id}")
    public Mono<ResponseEntity<TaskDto>> updateTaskCombined(
            @PathVariable String id,
            @Valid @RequestBody TaskDto taskDto,
            ServerHttpRequest httpRequest
    ) {
        String userId = extractUserIdFromHeader(httpRequest);
        return taskService.updateTaskCombined(id, taskDto, userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(ConflictException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
                );
    }    @PreAuthorize("hasPermission(#id, 'Task', 'TASK_PRIORITY_CHANGE')")
    @PutMapping("/{id}/priority")
    public Mono<ResponseEntity<TaskDto>> updateTaskPriority(
            @PathVariable String id,
            @Valid @RequestBody UpdateTaskPriorityRequestDto request,
            ServerHttpRequest httpRequest
    ) {
        String userId = extractUserIdFromHeader(httpRequest);
        return taskService.updateTaskPriority(
                        id,
                        request.getNewPriority(),
                        request.getExpectedVersion(),
                        userId
                )
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(ConflictException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
                );
    }    @PreAuthorize("hasPermission(#id, 'Task', 'TASK_DELETE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteTask(@PathVariable String id) {
        log.info("Received request to delete task ID: {}", id);
        return taskService.deleteTask(id);
    }

}