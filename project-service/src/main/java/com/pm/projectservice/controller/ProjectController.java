package com.pm.projectservice.controller;


// Import DTO from the SHARED module
import java.net.URI;

import com.pm.commoncontracts.domain.ProjectStatus;
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

import com.pm.commoncontracts.dto.ProjectDto;
import com.pm.commoncontracts.dto.TaskDto;
import com.pm.commoncontracts.requestDto.project.UpdateProjectStatusRequestDto;
import com.pm.projectservice.service.ProjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    private String extractUserIdFromHeader(ServerHttpRequest request) {
        return request.getHeaders().getFirst("X-User-Id");
    }

    @PreAuthorize("hasPermission(null, 'PRJ_CREATE')")
    @PostMapping
    public Mono<ResponseEntity<ProjectDto>> createProject(
            @Valid @RequestBody ProjectDto projectDto,
            UriComponentsBuilder uriBuilder,
            ServerHttpRequest request
    ) {
        log.info("Received request to create project: {}", projectDto);
        String userId = extractUserIdFromHeader(request);
        projectDto.setCreatedBy(userId);
        return projectService.createProject(projectDto)
                .map(createdProject -> {
                    URI location = uriBuilder.path("/projects/{id}")
                            .buildAndExpand(createdProject.getId())
                            .toUri();
                    log.info("Project created successfully with ID: {}", createdProject.getId());
                    return ResponseEntity.created(location).body(createdProject);
                });
    }

    @PreAuthorize("hasPermission(null, 'PRJ_READ')")
    @GetMapping
    public Flux<ProjectDto> getAllProjects()
    {
        log.info("Received request to get all projects");
        return projectService.getAllProjects();
    }

    @PreAuthorize("hasPermission(#id, 'Project', 'PRJ_READ')")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ProjectDto>> getProjectById(@PathVariable String id) {
        log.info("Received request to get project by ID: {}", id);
        return projectService.getProjectById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasPermission(null, 'PRJ_READ')")
    @GetMapping(params = "name")
    public Flux<ProjectDto> getProjectByName(@RequestParam String name)
    {
        return projectService.getProjectByName(name);
    }

    @PreAuthorize("hasPermission(null, 'PRJ_READ')")
    @GetMapping(params = "owner")
    public Flux<ProjectDto> getProjectByOwner(@RequestParam String owner)
    {
        return projectService.getProjectByCreatedBy(owner);
    }

    @PreAuthorize("hasPermission(null, 'PRJ_READ')")
    @GetMapping("/status/{status}")
    public Flux<ProjectDto> getProjectsByStatus(@PathVariable ProjectStatus status) {
        log.info("Received request to get projects with status: {}", status);
        return projectService.getProjectsByStatus(status);
    }

    @PreAuthorize("hasPermission(#id, 'Project', 'PRJ_UPDATE')")
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ProjectDto>> updateProject(@PathVariable String id, @Valid @RequestBody ProjectDto projectDto)
    {
        log.info("Received request to update project with ID: {}", id);
        return projectService.updateProject(id, projectDto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    @PreAuthorize("hasPermission(#projectId, 'Project', 'PRJ_READ')")
    @GetMapping("/{projectId}/tasks/ids")
    public Flux<String> getAllTaskIdsByProjectId(@PathVariable String projectId) {
        log.info("Received request to get all task IDs for project: {}", projectId);
        return projectService.getAllTaskIdsByProjectId(projectId);
    }

    @PreAuthorize("hasPermission(#projectId, 'Project', 'TASK_CREATE')")
    @PostMapping("/{projectId}/tasks")
    public Mono<ResponseEntity<TaskDto>> createTaskForProject(
            @PathVariable String projectId,
            @Valid @RequestBody TaskDto taskDto,
            UriComponentsBuilder uriBuilder
    ) {
        log.info("Received request to create a task for project {}: {}", projectId, taskDto);
        return projectService.createTaskForProject(projectId, taskDto)
                .map(createdTask -> {
                    URI location = uriBuilder.path("/projects/{projectId}/tasks/{taskId}")
                            .buildAndExpand(projectId, createdTask.getId())
                            .toUri();
                    log.info("Task created for project {} with ID: {}", projectId, createdTask.getId());
                    return ResponseEntity.created(location).body(createdTask);
                });
    }

    @PreAuthorize("hasPermission(#id, 'Project', 'PRJ_STATUS_CHANGE')")
    @PutMapping("/{id}/status")
    public Mono<ResponseEntity<ProjectDto>> updateProjectStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateProjectStatusRequestDto request
    ) {
        log.info("Received request to update status for project {} to {}", id, request.getNewStatus());
        return projectService.updateProjectStatus(id, request.getNewStatus(), request.getExpectedVersion())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasPermission(#id, 'Project', 'PRJ_UPDATE')")
    @PutMapping("/{id}/combined")
    public Mono<ResponseEntity<ProjectDto>> updateProjectCombined(
            @PathVariable String id,
            @Valid @RequestBody ProjectDto dto
    ) {
        log.info("Received request to update project {} (combined fields)", id);
        return projectService.updateProjectCombined(id, dto)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasPermission(#id, 'Project', 'PRJ_DELETE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteProject(@PathVariable String id)
    {
        log.info("Received request to delete project with ID: {}", id);
        return projectService.deleteProject(id);
    }

}
