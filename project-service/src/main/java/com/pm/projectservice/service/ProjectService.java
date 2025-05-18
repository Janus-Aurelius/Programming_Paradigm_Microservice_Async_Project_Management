package com.pm.projectservice.service;

// Shared module imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service; // Import your event payload records
import org.springframework.web.reactive.function.client.WebClient; // Assuming this provides the context key

import com.pm.commoncontracts.domain.ProjectStatus;
import com.pm.commoncontracts.dto.ProjectDto; // Using your existing mapper utility
import com.pm.commoncontracts.dto.TaskDto; // Your internal domain entity
import com.pm.commoncontracts.envelope.EventEnvelope;
import com.pm.commoncontracts.events.project.ProjectCreatedEventPayload;
import com.pm.commoncontracts.events.project.ProjectDeletedEventPayload;
import com.pm.commoncontracts.events.project.ProjectStatusChangedEventPayload;
import com.pm.commoncontracts.events.project.ProjectTaskCreatedEventPayload;
import com.pm.commoncontracts.events.project.ProjectUpdatedEventPayload;
import com.pm.projectservice.config.MdcLoggingFilter;
import com.pm.projectservice.model.Project;
import com.pm.projectservice.repository.ProjectRepository; // Import ContextView
import com.pm.projectservice.utils.ProjectUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private final ProjectRepository projectRepository;
    private final ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final WebClient taskServiceWebClient = WebClient.builder()
            .baseUrl("http://localhost:8081/api/tasks") // Adjust if service discovery/gateway is used
            .build();

    @Value("${spring.application.name}")
    private String serviceName;
    @Value("${kafka.topic.project-events:project-events}")
    private String projectEventsTopic;

    // Constructor injection
    public ProjectService(ProjectRepository projectRepository,
                          ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> kafkaTemplate) {
        this.projectRepository = projectRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // ==============================
    // Read operations (No changes needed)
    // ==============================
    public Flux<ProjectDto> getAllProjects() {
        return projectRepository.findAll()
                .map(ProjectUtils::entityToDto)
                .doOnError(e -> log.error("Error fetching all projects", e))
                .onErrorResume(e -> Flux.error(new RuntimeException("Failed to fetch projects", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getAllProjects, skipping element", throwable));
    }

    public Flux<ProjectDto> getProjectByName(String name) {
        return projectRepository.findByName(name)
                .map(ProjectUtils::entityToDto)
                .doOnError(e -> log.error("Error fetching project by name: {}", name, e))
                .onErrorResume(e -> Flux.error(new RuntimeException("Failed to fetch project by name", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getProjectByName, skipping element", throwable));
    }

    public Mono<ProjectDto> getProjectById(String id) {
        return projectRepository.findById(id)
                .map(ProjectUtils::entityToDto)
                .doOnError(e -> log.error("Error fetching project by ID: {}", id, e))
                .switchIfEmpty(Mono.error(new RuntimeException("Project not found: " + id)))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to fetch project by id", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getProjectById, skipping element", throwable));
    }

    public Flux<ProjectDto> getProjectByCreatedBy(String owner) {
        return projectRepository.findByCreatedBy(owner)
                .map(ProjectUtils::entityToDto)
                .doOnError(e -> log.error("Error fetching projects by owner: {}", owner, e))
                .onErrorResume(e -> Flux.error(new RuntimeException("Failed to fetch projects by owner", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getProjectByCreatedBy, skipping element", throwable));
    }

    public Flux<ProjectDto> getProjectsByStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status)
                .map(ProjectUtils::entityToDto)
                .doOnError(e -> log.error("Error fetching projects by status: {}", status, e))
                .onErrorResume(e -> Flux.error(new RuntimeException("Failed to fetch projects by status", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getProjectsByStatus, skipping element", throwable));
    }

    // ==============================
    // Get all tasks for a project (with event publishing)
    // ==============================
    public Flux<String> getAllTaskIdsByProjectId(String projectId) {
        return projectRepository.findById(projectId)
                .flatMapMany(project -> {
                    if (project.getTaskIds() == null || project.getTaskIds().isEmpty()) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(project.getTaskIds());
                })
                .doOnError(e -> log.error("Error fetching task IDs for project: {}", projectId, e))
                .onErrorResume(e -> Flux.error(new RuntimeException("Failed to fetch task IDs for project", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getAllTaskIdsByProjectId, skipping element", throwable));
    }

    // ==============================
    // Create a task for a specific project
    // ==============================
    public Mono<TaskDto> createTaskForProject(String projectId, TaskDto taskDto) {
        TaskDto taskToCreate = TaskDto.builder()
                .id(taskDto.getId())
                .name(taskDto.getName())
                .status(taskDto.getStatus())
                .priority(taskDto.getPriority())
                .description(taskDto.getDescription())
                .createdBy(taskDto.getCreatedBy())
                .createdAt(taskDto.getCreatedAt())
                .updatedBy(taskDto.getUpdatedBy())
                .updatedAt(taskDto.getUpdatedAt())
                .dueDate(taskDto.getDueDate())
                .assigneeId(taskDto.getAssigneeId())
                .assigneeName(taskDto.getAssigneeName())
                .tags(taskDto.getTags())
                .attachments(taskDto.getAttachments())
                .version(taskDto.getVersion())
                .projectId(projectId)
                .build();

        // 1. Persist the task in the real task microservice
        return taskServiceWebClient.post()
                .uri("")
                .bodyValue(taskToCreate)
                .retrieve()
                .bodyToMono(TaskDto.class)
                .doOnError(e -> log.error("Error creating task for project: {}", projectId, e))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to create task for project", e)))
                .flatMap(createdTask ->
                        // 2. Add the created task's ID to the project
                        projectRepository.findById(projectId)
                                .flatMap(project -> {
                                    project.getTaskIds().add(createdTask.getId());
                                    return projectRepository.save(project)
                                            .thenReturn(createdTask);
                                })
                                .doOnError(e -> log.error("Error updating project with new task ID: {}", projectId, e))
                                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to update project with new task ID", e)))
                                // 3. Publish event after both task creation and project update
                                .doOnSuccess(savedTask -> publishProjectTaskCreatedEvent(savedTask, projectId))
                )
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in createTaskForProject, skipping element", throwable));
    }

    private void publishProjectTaskCreatedEvent(TaskDto createdTask, String projectId) {
        try {
            ProjectTaskCreatedEventPayload payload = new ProjectTaskCreatedEventPayload(createdTask, projectId);
            EventEnvelope<ProjectTaskCreatedEventPayload> envelope = new EventEnvelope<>(
                    "N/A-project-task-create",
                    ProjectTaskCreatedEventPayload.EVENT_TYPE,
                    serviceName,
                    payload
            );
            log.info("Publishing ProjectTaskCreatedEvent envelope for projectId: {} and taskId: {}", projectId, createdTask.getId());
            kafkaTemplate.send(projectEventsTopic, projectId, envelope)
                    .doOnError(e -> log.error("Failed to send ProjectTaskCreatedEvent envelope for projectId: {}", projectId, e))
                    .onErrorResume(e -> {
                        // TODO: Optionally send to dead-letter topic here
                        log.error("Unrecoverable error publishing ProjectTaskCreatedEvent for projectId: {}", projectId, e);
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error preparing or sending ProjectTaskCreatedEvent for projectId: {}", projectId, e);
        }
    }

    // ==============================
    // Write operations (Transformed)
    // ==============================

    public Mono<ProjectDto> createProject(ProjectDto projectDto) {
        // Use deferContextual to access Reactor Context
        return Mono.deferContextual(contextView -> {
            Project projectEntity = ProjectUtils.dtoToEntity(projectDto);
            return projectRepository.save(projectEntity)
                    .doOnError(e -> log.error("Error creating project", e))
                    .onErrorResume(e -> Mono.error(new RuntimeException("Failed to create project", e)))
                    .doOnSuccess(createdProject -> publishProjectCreatedEvent(createdProject, contextView))
                    .map(ProjectUtils::entityToDto)
                    .onErrorContinue((throwable, o) -> log.error("Unhandled error in createProject, skipping element", throwable));
        });
    }

    public Mono<ProjectDto> updateProject(String id, ProjectDto projectDto) {
        // Use deferContextual to access Reactor Context
        return Mono.deferContextual(contextView ->
                projectRepository.findById(id)
                        .flatMap(existingProject -> {
                            existingProject.setName(projectDto.getName());
                            existingProject.setDescription(projectDto.getDescription());
                            existingProject.setCreatedBy(projectDto.getCreatedBy());
                            existingProject.setAssignedTo(projectDto.getAssignedTo());
                            existingProject.setStartDate(projectDto.getStartDate());
                            existingProject.setEndDate(projectDto.getEndDate());
                            existingProject.setMemberIds(projectDto.getMemberIds());
                            existingProject.setPriority(projectDto.getPriority());
                            return projectRepository.save(existingProject)
                                    .doOnError(e -> log.error("Error updating project with ID: {}", id, e))
                                    .onErrorResume(e -> Mono.error(new RuntimeException("Failed to update project", e)))
                                    .doOnSuccess(updatedProject -> publishProjectUpdatedEvent(updatedProject, contextView));
                        })
                        .map(ProjectUtils::entityToDto)
                        .switchIfEmpty(Mono.error(new RuntimeException("Project not found for update: " + id)))
                        .onErrorContinue((throwable, o) -> log.error("Unhandled error in updateProject, skipping element", throwable))
        );
    }

    public Mono<ProjectDto> updateProjectStatus(String projectId, ProjectStatus newStatus, Long expectedVersion) {
        return projectRepository.findById(projectId)
                .flatMap(project -> {
                    if (expectedVersion != null && !expectedVersion.equals(project.getVersion())) {
                        return Mono.error(new RuntimeException("Version mismatch for project " + projectId));
                    }
                    boolean statusChanged = !project.getStatus().equals(newStatus);
                    project.setStatus(newStatus);
                    return projectRepository.save(project)
                            .doOnError(e -> log.error("Error updating project status for projectId: {}", projectId, e))
                            .onErrorResume(e -> Mono.error(new RuntimeException("Failed to update project status", e)))
                            .map(ProjectUtils::entityToDto)
                            .doOnSuccess(updatedDto -> {
                                if (statusChanged) {
                                    publishProjectStatusChangedEvent(updatedDto);
                                }
                            });
                })
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in updateProjectStatus, skipping element", throwable));
    }

    private void publishProjectStatusChangedEvent(ProjectDto projectDto) {
        try {
            ProjectStatusChangedEventPayload payload = new ProjectStatusChangedEventPayload(projectDto);
            EventEnvelope<ProjectStatusChangedEventPayload> envelope = new EventEnvelope<>(
                    "N/A-status-change",
                    ProjectStatusChangedEventPayload.EVENT_TYPE,
                    serviceName,
                    payload
            );
            log.info("Publishing ProjectStatusChangedEvent envelope for projectId: {}", projectDto.getId());
            kafkaTemplate.send(projectEventsTopic, projectDto.getId(), envelope)
                    .doOnError(e -> log.error("Failed to send ProjectStatusChangedEvent envelope for projectId: {}", projectDto.getId(), e))
                    .onErrorResume(e -> {
                        // TODO: Optionally send to dead-letter topic here
                        log.error("Unrecoverable error publishing ProjectStatusChangedEvent for projectId: {}", projectDto.getId(), e);
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error preparing or sending ProjectStatusChangedEvent for projectId: {}", projectDto.getId(), e);
        }
    }

    public Mono<ProjectDto> updateProjectCombined(String id, ProjectDto dto) {
        return projectRepository.findById(id)
                .flatMap(project -> {
                    boolean statusChanged = !project.getStatus().equals(dto.getStatus());
                    project.setName(dto.getName());
                    project.setDescription(dto.getDescription());
                    project.setStatus(dto.getStatus());
                    project.setCreatedBy(dto.getCreatedBy());
                    project.setAssignedTo(dto.getAssignedTo());
                    project.setStartDate(dto.getStartDate());
                    project.setEndDate(dto.getEndDate());
                    return projectRepository.save(project)
                            .doOnError(e -> log.error("Error updating project combined fields for ID: {}", id, e))
                            .onErrorResume(e -> Mono.error(new RuntimeException("Failed to update project combined fields", e)))
                            .map(ProjectUtils::entityToDto)
                            .doOnSuccess(updatedDto -> {
                                if (statusChanged) {
                                    publishProjectStatusChangedEvent(updatedDto);
                                }
                                publishProjectUpdatedEvent(updatedDto);
                            });
                })
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in updateProjectCombined, skipping element", throwable));
    }

    private void publishProjectUpdatedEvent(ProjectDto projectDto) {
        try {
            ProjectUpdatedEventPayload payload = new ProjectUpdatedEventPayload(projectDto);
            EventEnvelope<ProjectUpdatedEventPayload> envelope = new EventEnvelope<>(
                    "N/A-project-update",
                    ProjectUpdatedEventPayload.EVENT_TYPE,
                    serviceName,
                    payload
            );
            log.info("Publishing ProjectUpdatedEvent envelope for projectId: {}", projectDto.getId());
            kafkaTemplate.send(projectEventsTopic, projectDto.getId(), envelope)
                    .doOnError(e -> log.error("Failed to send ProjectUpdatedEvent envelope for projectId: {}", projectDto.getId(), e))
                    .onErrorResume(e -> {
                        // TODO: Optionally send to dead-letter topic here
                        log.error("Unrecoverable error publishing ProjectUpdatedEvent for projectId: {}", projectDto.getId(), e);
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error preparing or sending ProjectUpdatedEvent for projectId: {}", projectDto.getId(), e);
        }
    }

    public Mono<Void> deleteProject(String id) {
        // Use deferContextual to access Reactor Context
        return Mono.deferContextual(contextView ->
                projectRepository.findById(id)
                        .flatMap(projectToDelete ->
                                projectRepository.deleteById(id).thenReturn(projectToDelete)
                        )
                        .doOnError(e -> log.error("Error deleting project with ID: {}", id, e))
                        .onErrorResume(e -> Mono.error(new RuntimeException("Failed to delete project", e)))
                        .doOnSuccess(deletedProject -> publishProjectDeletedEvent(deletedProject, contextView))
                        .then()
                        .switchIfEmpty(Mono.error(new RuntimeException("Project not found for deletion: " + id)))
                        .onErrorContinue((throwable, o) -> log.error("Unhandled error in deleteProject, skipping element", throwable))
        );
    }

    public Mono<Void> removeUserFromAllProjects(String userId) {
        return projectRepository.findAll()
                .filter(project -> project.getMemberIds() != null && project.getMemberIds().contains(userId))
                .flatMap(project -> {
                    project.getMemberIds().removeIf(id -> id.equals(userId));
                    return projectRepository.save(project)
                        .doOnError(e -> log.error("Error removing user {} from project {}", userId, project.getId(), e))
                        .onErrorResume(e -> Mono.error(new RuntimeException("Failed to remove user from project", e)))
                        .doOnSuccess(p -> log.info("Removed user {} from project {}", userId, project.getId()));
                })
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in removeUserFromAllProjects, skipping element", throwable))
                .then();
    }

    // ==============================
    // Event Publishing Helper Methods
    // ==============================

    private void publishProjectCreatedEvent(Project createdProject, ContextView contextView) {
        String correlationId = contextView.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-proj-create");
        try {
            ProjectDto projectDto = ProjectUtils.entityToDto(createdProject);
            ProjectCreatedEventPayload payload = new ProjectCreatedEventPayload(projectDto);
            EventEnvelope<ProjectCreatedEventPayload> envelope = new EventEnvelope<>(
                    correlationId,
                    ProjectCreatedEventPayload.EVENT_TYPE,
                    serviceName,
                    payload
            );
            log.info("Publishing ProjectCreatedEvent envelope. CorrID: {}", correlationId);
            kafkaTemplate.send(projectEventsTopic, createdProject.getId(), envelope)
                    .doOnError(e -> log.error("Failed to send ProjectCreatedEvent envelope. CorrID: {}", correlationId, e))
                    .onErrorResume(e -> {
                        // TODO: Optionally send to dead-letter topic here
                        log.error("Unrecoverable error publishing ProjectCreatedEvent. CorrID: {}", correlationId, e);
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error preparing or sending ProjectCreatedEvent. CorrID: {}", correlationId, e);
        }
    }

    private void publishProjectUpdatedEvent(Project updatedProject, ContextView contextView) {
        String correlationId = contextView.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-proj-update");
        try {
            ProjectDto projectDto = ProjectUtils.entityToDto(updatedProject);
            ProjectUpdatedEventPayload payload = new ProjectUpdatedEventPayload(projectDto);
            EventEnvelope<ProjectUpdatedEventPayload> envelope = new EventEnvelope<>(
                    correlationId,
                    ProjectUpdatedEventPayload.EVENT_TYPE,
                    serviceName,
                    payload
            );
            log.info("Publishing ProjectUpdatedEvent envelope. CorrID: {}", correlationId);
            kafkaTemplate.send(projectEventsTopic, updatedProject.getId(), envelope)
                    .doOnError(e -> log.error("Failed to send ProjectUpdatedEvent envelope. CorrID: {}", correlationId, e))
                    .onErrorResume(e -> {
                        // TODO: Optionally send to dead-letter topic here
                        log.error("Unrecoverable error publishing ProjectUpdatedEvent. CorrID: {}", correlationId, e);
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error preparing or sending ProjectUpdatedEvent. CorrID: {}", correlationId, e);
        }
    }

    private void publishProjectDeletedEvent(Project deletedProject, ContextView contextView) {
        String correlationId = contextView.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-proj-delete");
        try {
            ProjectDto projectDto = ProjectUtils.entityToDto(deletedProject);
            ProjectDeletedEventPayload payload = new ProjectDeletedEventPayload(projectDto);
            EventEnvelope<ProjectDeletedEventPayload> envelope = new EventEnvelope<>(
                    correlationId,
                    ProjectDeletedEventPayload.EVENT_TYPE,
                    serviceName,
                    payload
            );
            log.info("Publishing ProjectDeletedEvent envelope. CorrID: {}", correlationId);
            kafkaTemplate.send(projectEventsTopic, deletedProject.getId(), envelope)
                    .doOnError(e -> log.error("Failed to send ProjectDeletedEvent envelope. CorrID: {}", correlationId, e))
                    .onErrorResume(e -> {
                        // TODO: Optionally send to dead-letter topic here
                        log.error("Unrecoverable error publishing ProjectDeletedEvent. CorrID: {}", correlationId, e);
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("Error preparing or sending ProjectDeletedEvent. CorrID: {}", correlationId, e);
        }
    }

}