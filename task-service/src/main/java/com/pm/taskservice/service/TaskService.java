package com.pm.taskservice.service;

// Shared module imports
import java.time.Instant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.pm.commoncontracts.domain.TaskPriority; // Assuming this provides the context key
import com.pm.commoncontracts.domain.TaskStatus;
import com.pm.commoncontracts.dto.TaskDto;
import com.pm.commoncontracts.dto.UserDto;
import com.pm.commoncontracts.envelope.EventEnvelope; // Using your existing mapper utility
import com.pm.commoncontracts.events.task.TaskAssignedEventPayload; // Your internal domain entity
import com.pm.commoncontracts.events.task.TaskCreatedEventPayload;
import com.pm.commoncontracts.events.task.TaskDeletedEventPayload;
import com.pm.commoncontracts.events.task.TaskPriorityChangedEventPayload;
import com.pm.commoncontracts.events.task.TaskStatusChangedEventPayload;
import com.pm.commoncontracts.events.task.TaskUpdatedEventPayload;
import com.pm.taskservice.config.MdcLoggingFilter;
import com.pm.taskservice.exception.ConflictException;
import com.pm.taskservice.exception.ResourceNotFoundException;
import com.pm.taskservice.model.Task; // Import ContextView
import com.pm.taskservice.repository.TaskRepository;
import com.pm.taskservice.utils.TaskUtils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@Service
@Slf4j
public class TaskService {

    private final TaskRepository repository;
    private final ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final WebClient userWebClient;

    @Value("${kafka.topic.task-events}")
    private String taskEventsTopic;

    @Value("${spring.application.name}")
    private String serviceName;

    public TaskService(
            TaskRepository repository,
            ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> kafkaTemplate,
            @Qualifier("userWebClient") WebClient userWebClient
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.userWebClient = userWebClient;
    }

    // ==============================
    // Read operations (No changes needed)
    // ==============================
    public Flux<TaskDto> getTasks() {
        return repository.findAll()
                .map(TaskUtils::entityToDto)
                .doOnError(e -> log.error("Error fetching all tasks", e))
                .onErrorResume(e -> Flux.error(new RuntimeException("Failed to fetch tasks", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getTasks, skipping element", throwable));
    }

    public Mono<TaskDto> getTaskById(String id) {
        return repository.findById(id)
                .map(TaskUtils::entityToDto)
                .doOnError(e -> log.error("Error fetching task by ID: {}", id, e))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Task not found: " + id)))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to fetch task by id", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getTaskById, skipping element", throwable));
    }

    public Flux<TaskDto> getTasksByProjectId(String projectId) {
        return repository.findByProjectId(projectId)
                .map(TaskUtils::entityToDto)
                .doOnError(e -> log.error("Error fetching tasks by project ID: {}", projectId, e))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("No tasks found for project ID: " + projectId)))
                .onErrorResume(e -> Flux.error(new RuntimeException("Failed to fetch tasks by projectId", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getTasksByProjectId, skipping element", throwable));
    }

    public Flux<TaskDto> getTasksByAssigneeId(String assigneeId) {
        return repository.findByAssigneeId(assigneeId)
                .flatMap(task -> enrichTaskWithUserInfo(TaskUtils.entityToDto(task)))
                .doOnError(e -> log.error("Error fetching tasks by assigned user: {}", assigneeId, e))
                .switchIfEmpty(Flux.error(new ResourceNotFoundException("No tasks found for user: " + assigneeId)))
                .onErrorResume(e -> Flux.error(new RuntimeException("Failed to fetch tasks by assigneeId", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getTasksByAssigneeId, skipping element", throwable));
    }

    // ==============================
    // Write operations (Transformed)
    // ==============================
    public Mono<TaskDto> createTask(TaskDto taskDto) {
        // Use deferContextual to access Reactor Context for correlationId
        return Mono.deferContextual(contextView -> {
            Task taskEntity = TaskUtils.dtoToEntity(taskDto);
            // You might want to set default status, creation date etc. on taskEntity here
            return repository.insert(taskEntity)
                    .doOnSuccess(createdTask -> publishTaskCreatedEvent(createdTask, contextView)) // Publish event on success
                    .doOnError(e -> log.error("Error creating task", e))
                    .onErrorResume(e -> Mono.error(new RuntimeException("Failed to create task", e)))
                    .map(TaskUtils::entityToDto); // Map final entity to DTO for response
        });
    }

    public Mono<TaskDto> updateTaskStatus(String taskId, TaskStatus newStatus,
            Long expectedVersion, String userId) {
        return Mono.deferContextual(contextView
                -> // For Correlation ID
                repository.findById(taskId)
                        .flatMap(task -> {
                            // --- Optimistic Lock Check ---
                            Long currentVersion = task.getVersion();
                            log.debug("Attempting update for task [{}]. Expected version: {}, Current version: {}", taskId, expectedVersion, currentVersion);
                            if (expectedVersion == null || !expectedVersion.equals(currentVersion)) {
                                log.warn("Optimistic Lock Conflict: Task [{}]. Expected version: {}, Current version: {}", taskId, expectedVersion, currentVersion);
                                // Throw a specific exception for conflict
                                return Mono.error(new ConflictException("Conflict detected for task " + taskId + ". Data may have changed."));
                            }
                            // --- End Optimistic Lock Check ---

                            // Versions match - Proceed with update
                            if (task.getStatus() == newStatus) {
                                log.debug("Task [{}] status already matches [{}]. No update needed.", taskId, newStatus);
                                return Mono.just(task); // No change, return current task
                            }

                            task.setStatus(newStatus);
                            // task.setUpdatedBy(userId);
                            // task.setUpdatedAt(Instant.now());
                            // Version is incremented automatically by Spring Data @Version on save

                            log.debug("Saving updated task [{}] with new status [{}]", taskId, newStatus);
                            // Save using repository. Spring Data's save handles the @Version increment and check.
                            return repository.save(task);
                        })
                        .doOnSuccess(savedTask -> {
                            // Only publish event AFTER successful save
                            if (savedTask != null) { // Check if save actually happened (though save usually returns the object)
                                publishTaskStatusChangedEvent(savedTask, contextView); // Pass saved task (with new version)
                            }
                        })
                        .doOnError(e -> log.error("Error updating task status", e))
                        .onErrorResume(e -> Mono.error(new RuntimeException("Failed to update task status", e)))
                        .map(TaskUtils::entityToDto) // Map the successfully saved task (with incremented version) to DTO
                        .switchIfEmpty(Mono.error(new RuntimeException("Task not found: " + taskId))) // Or specific NotFoundException
        );
        // Note: The actual atomic check happens INSIDE taskRepository.save() if using @Version
    }

    public Mono<TaskDto> updateTaskCombined(String id, TaskDto dto, String userId) {
        return Mono.deferContextual(contextView
                -> repository.findById(id)
                        .flatMap(task -> {
                            // Optimistic‑lock
                            Long currentVersion = task.getVersion();
                            if (dto.getVersion() == null || !dto.getVersion().equals(currentVersion)) {
                                return Mono.error(new ConflictException(
                                        "Version mismatch for task " + id));
                            }                            // Apply all changes
                            task.setName(dto.getName());
                            task.setDescription(dto.getDescription());
                            if (dto.getDueDate() != null && !dto.getDueDate().trim().isEmpty()) {
                                try {
                                    task.setDueDate(Instant.parse(dto.getDueDate()));
                                } catch (Exception e) {
                                    // Skip invalid date format
                                    task.setDueDate(null);
                                }
                            } else {
                                task.setDueDate(null);
                            }
                            task.setPriority(dto.getPriority());
                            task.setAssigneeId(dto.getAssigneeId());
                            task.setStatus(dto.getStatus());
                            task.setUpdatedBy(userId);
                            task.setUpdatedAt(Instant.now());

                            return repository.save(task);
                        })
                        .doOnSuccess(saved -> {
                            publishTaskUpdatedEvent(saved, contextView);
                            if (saved.getStatus() != dto.getStatus()) {
                                publishTaskStatusChangedEvent(saved, contextView);
                            }
                        })
                        .doOnError(e -> log.error("Error updating task combined", e))
                        .onErrorResume(e -> Mono.error(new RuntimeException("Failed to update task combined", e)))
                        .map(TaskUtils::entityToDto)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                                "Task not found: " + id)))
        );
    }

    public Mono<TaskDto> updateTaskPriority(
            String taskId,
            TaskPriority newPriority,
            Long expectedVersion,
            String userId
    ) {
        return Mono.deferContextual(ctx
                -> repository.findById(taskId)
                        .flatMap(task -> {
                            if (expectedVersion == null || !expectedVersion.equals(task.getVersion())) {
                                return Mono.error(new ConflictException("Version mismatch for task " + taskId));
                            }
                            if (task.getPriority() == newPriority) {
                                return Mono.just(task);
                            }
                            task.setPriority(newPriority);
                            return repository.save(task);
                        })
                        .doOnSuccess(saved -> publishTaskPriorityChangedEvent(saved, ctx))
                        .doOnError(e -> log.error("Error updating task priority", e))
                        .onErrorResume(e -> Mono.error(new RuntimeException("Failed to update task priority", e)))
                        .map(TaskUtils::entityToDto)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Task not found: " + taskId)))
        );
    }

    public Mono<TaskDto> assignTask(String taskId, String assigneeId, String actorId) {
        // Validate user existence before assignment
        return userWebClient.get()
                .uri("/api/users/{id}", assigneeId)
                .retrieve()
                .bodyToMono(com.pm.commoncontracts.dto.UserDto.class)
                .flatMap(userDto
                        -> repository.findById(taskId)
                        .flatMap(task -> {
                            task.setAssigneeId(assigneeId);
                            task.setUpdatedBy(actorId);
                            task.setUpdatedAt(Instant.now());
                            return repository.save(task);
                        })
                        .flatMap(savedTask -> {
                            TaskDto taskDto = TaskUtils.entityToDto(savedTask);
                            return enrichTaskWithUserInfo(taskDto)
                                    .doOnNext(enrichedDto -> publishTaskAssignedEvent(savedTask, enrichedDto.getAssigneeName(), reactor.util.context.Context.empty()));
                        })
                        .doOnError(e -> log.error("Error assigning task", e))
                        .onErrorResume(e -> Mono.error(new RuntimeException("Failed to assign task", e)))
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Task not found: " + taskId)))
                )
                .doOnError(e -> log.error("Error validating user existence", e))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to validate user existence", e)))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found: " + assigneeId)));
    }

    public Mono<Void> deleteTask(String taskId) {
        return Mono.deferContextual(ctx
                -> repository.findById(taskId)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Task not found: " + taskId)))
                        .flatMap(task
                                -> repository.delete(task)
                                .doOnSuccess(ignored -> publishTaskDeletedEvent(task, ctx))
                        )
                        .doOnError(e -> log.error("Error deleting task", e))
                        .onErrorResume(e -> Mono.error(new RuntimeException("Failed to delete task", e)))
        );
    }

    /**
     * Removes the given userId from all tasks where they are assigned as
     * assignee.
     *
     * @param userId the user to remove
     * @return Mono<Void> when operation is complete
     */
    public Mono<Void> removeUserFromAllTasks(String userId) {
        return repository.findByAssigneeId(userId)
                .flatMap(task -> {
                    task.setAssigneeId(null);
                    return repository.save(task)
                            .doOnSuccess(t -> log.info("Removed user {} as assignee from task {}", userId, task.getId()));
                })
                .doOnError(e -> log.error("Error removing user from all tasks", e))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to remove user from all tasks", e)))
                .then();
    }

    // ==============================
    // Event Publishing Helper Methods
    // ==============================
    private void publishTaskCreatedEvent(Task createdTask, ContextView contextView) {
        String correlationId = contextView.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-create");
        try {
            TaskDto taskDto = TaskUtils.entityToDto(createdTask);
            TaskCreatedEventPayload payload = new TaskCreatedEventPayload(taskDto);
            EventEnvelope<TaskCreatedEventPayload> envelope = new EventEnvelope<>(
                    correlationId,
                    TaskCreatedEventPayload.EVENT_TYPE,
                    serviceName,
                    payload
            );
            log.info("Publishing TaskCreatedEvent envelope. CorrID: {}", correlationId);
            kafkaTemplate.send(taskEventsTopic, createdTask.getId(), envelope)
                    .doOnError(e -> log.error("Failed to send TaskCreatedEvent envelope. CorrID: {}", correlationId, e))
                    .onErrorResume(e -> {
                        // TODO: Optionally send to dead-letter topic here
                        log.error("Unrecoverable error publishing TaskCreatedEvent. CorrID: {}", correlationId, e);
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error preparing or sending TaskCreatedEvent. CorrID: {}", correlationId, e);
        }
    }

    private void publishTaskStatusChangedEvent(Task savedTask, ContextView contextView) {
        String correlationId = contextView.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-fallback");
        TaskDto taskDto = TaskUtils.entityToDto(savedTask); // Using entityToDto instead of dtoToEntity
        TaskStatusChangedEventPayload payload = new TaskStatusChangedEventPayload(taskDto);
        EventEnvelope<TaskStatusChangedEventPayload> envelope = new EventEnvelope<>(
                correlationId, TaskStatusChangedEventPayload.EVENT_TYPE, serviceName, payload
        );
        log.info("Publishing TaskStatusChangedEvent envelope. New Version: {}. CorrID: {}", savedTask.getVersion(), correlationId);
        kafkaTemplate.send(taskEventsTopic, savedTask.getId(), envelope)
                .doOnError(e -> log.error("Failed to send TaskStatusChangedEvent envelope. CorrID: {}", correlationId, e))
                .onErrorResume(e -> {
                    // TODO: Optionally send to dead-letter topic here
                    log.error("Unrecoverable error publishing TaskStatusChangedEvent. CorrID: {}", correlationId, e);
                    return Mono.empty();
                })
                .subscribe();
    }

    private void publishTaskDeletedEvent(Task deletedTask, ContextView contextView) {
        String correlationId = contextView.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-delete");
        try {
            TaskDto taskDto = TaskUtils.entityToDto(deletedTask);
            TaskDeletedEventPayload payload = new TaskDeletedEventPayload(taskDto);
            EventEnvelope<TaskDeletedEventPayload> envelope = new EventEnvelope<>(
                    correlationId,
                    TaskDeletedEventPayload.EVENT_TYPE,
                    serviceName,
                    payload
            );
            log.info("Publishing TaskDeletedEvent envelope. CorrID: {}", correlationId);
            kafkaTemplate.send(taskEventsTopic, deletedTask.getId(), envelope)
                    .doOnError(e -> log.error("Failed to send TaskDeletedEvent envelope. CorrID: {}", correlationId, e))
                    .onErrorResume(e -> {
                        // TODO: Optionally send to dead-letter topic here
                        log.error("Unrecoverable error publishing TaskDeletedEvent. CorrID: {}", correlationId, e);
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error preparing or sending TaskDeletedEvent. CorrID: {}", correlationId, e);
        }
    }

    private void publishTaskUpdatedEvent(Task updatedTask, ContextView contextView) {
        String correlationId = contextView.getOrDefault(
                MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY,
                "N/A-update"
        );
        TaskDto taskDto = TaskUtils.entityToDto(updatedTask);
        TaskUpdatedEventPayload payload = new TaskUpdatedEventPayload(taskDto);
        EventEnvelope<TaskUpdatedEventPayload> envelope = new EventEnvelope<>(
                correlationId,
                TaskUpdatedEventPayload.EVENT_TYPE,
                serviceName,
                payload
        );
        log.info("Publishing TaskUpdatedEvent envelope. CorrID: {}", correlationId);
        kafkaTemplate
                .send(taskEventsTopic, updatedTask.getId(), envelope)
                .doOnError(e
                        -> log.error("Failed to send TaskUpdatedEvent envelope. CorrID: {}", correlationId, e)
                )
                .onErrorResume(e -> {
                    // TODO: Optionally send to dead-letter topic here
                    log.error("Unrecoverable error publishing TaskUpdatedEvent. CorrID: {}", correlationId, e);
                    return Mono.empty();
                })
                .subscribe();
    }

    private void publishTaskPriorityChangedEvent(Task savedTask, ContextView ctx) {
        String correlationId = ctx.getOrDefault(
                MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY,
                "N/A-priority"
        );
        TaskDto dto = TaskUtils.entityToDto(savedTask);
        TaskPriorityChangedEventPayload payload = new TaskPriorityChangedEventPayload(dto);
        EventEnvelope<TaskPriorityChangedEventPayload> envelope = new EventEnvelope<>(
                correlationId,
                TaskPriorityChangedEventPayload.EVENT_TYPE,
                serviceName,
                payload
        );
        kafkaTemplate
                .send(taskEventsTopic, savedTask.getId(), envelope)
                .doOnError(e -> log.error("Failed to send TaskPriorityChangedEvent. CorrID: {}", correlationId, e))
                .onErrorResume(e -> {
                    // TODO: Optionally send to dead-letter topic here
                    log.error("Unrecoverable error publishing TaskPriorityChangedEvent. CorrID: {}", correlationId, e);
                    return Mono.empty();
                })
                .subscribe();
    }

    private void publishTaskAssignedEvent(Task task, String assigneeName, ContextView contextView) {
        String correlationId = contextView.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-assign");
        try {
            TaskDto taskDto = TaskUtils.entityToDto(task);
            taskDto.setAssigneeName(assigneeName);

            TaskAssignedEventPayload payload = new TaskAssignedEventPayload(taskDto);
            EventEnvelope<TaskAssignedEventPayload> envelope = new EventEnvelope<>(
                    correlationId,
                    TaskAssignedEventPayload.EVENT_TYPE,
                    serviceName,
                    payload
            );

            kafkaTemplate.send(taskEventsTopic, task.getId(), envelope)
                    .doOnError(e -> log.error("Failed to send TaskAssignedEvent. CorrID: {}", correlationId, e))
                    .onErrorResume(e -> {
                        // TODO: Optionally send to dead-letter topic here
                        log.error("Unrecoverable error publishing TaskAssignedEvent. CorrID: {}", correlationId, e);
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error preparing or sending TaskAssignedEvent. CorrID: {}", correlationId, e);
        }
    }

    private Mono<TaskDto> enrichTaskWithUserInfo(TaskDto taskDto) {
        if (taskDto.getAssigneeId() == null) {
            return Mono.just(taskDto);
        }

        return userWebClient.get()
                .uri("/api/users/{id}", taskDto.getAssigneeId())
                .retrieve()
                .bodyToMono(UserDto.class)
                .map(userDto -> {
                    taskDto.setAssigneeName(userDto.getFirstName() + " " + userDto.getLastName());
                    return taskDto;
                })
                .doOnError(e -> log.error("Error enriching task with user info", e))
                .onErrorResume(e -> {
                    log.warn("Could not fetch user info for task {}: {}", taskDto.getId(), e.getMessage());
                    return Mono.just(taskDto);
                });
    }
}
