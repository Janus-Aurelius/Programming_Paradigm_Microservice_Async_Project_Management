package com.pm.projectservice.controller;

// Import DTO from the SHARED module
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
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

import com.pm.commoncontracts.domain.ProjectStatus;
import com.pm.commoncontracts.dto.ProjectDto;
import com.pm.commoncontracts.dto.TaskDto;
import com.pm.commoncontracts.requestDto.project.UpdateProjectStatusRequestDto;
import com.pm.projectservice.security.ReactiveProjectPermissionEvaluator;
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
    private final ReactiveProjectPermissionEvaluator permissionEvaluator;

    private String extractUserIdFromHeader(ServerHttpRequest request) {
        return request.getHeaders().getFirst("X-User-Id");
    }

    @PostMapping
    public Mono<ResponseEntity<ProjectDto>> createProject(
            @Valid @RequestBody ProjectDto projectDto,
            UriComponentsBuilder uriBuilder,
            ServerHttpRequest request,
            Authentication authentication
    ) {
        log.info("Received request to create project: {}", projectDto);

        return permissionEvaluator.hasGeneralPermission(authentication, "PRJ_CREATE")
                .flatMap(hasAccess -> {
                    if (hasAccess) {
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
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).<ProjectDto>build());
                    }
                });
    }

    @GetMapping
    public Flux<ProjectDto> getAllProjects(Authentication authentication) {
        log.info("Received request to get all projects");

        return permissionEvaluator.hasGeneralPermission(authentication, "PRJ_READ")
                .flatMapMany(hasAccess -> {
                    if (hasAccess) {
                        return projectService.getAllProjects();
                    } else {
                        return Flux.empty(); // Return empty flux if no permission
                    }
                });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ProjectDto>> getProjectById(@PathVariable String id, Authentication authentication) {
        log.info("Received request to get project by ID: {}", id);

        return projectService.getProjectById(id)
                .flatMap(project -> {
                    // Check if user has permission to read this project using reactive permission evaluator
                    return permissionEvaluator.hasPermission(authentication, id, "PRJ_READ")
                            .map(hasAccess -> {
                                if (hasAccess) {
                                    return ResponseEntity.ok(project);
                                } else {
                                    return ResponseEntity.status(HttpStatus.FORBIDDEN).<ProjectDto>build();
                                }
                            });
                })
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping(params = "name")
    public Flux<ProjectDto> getProjectByName(@RequestParam String name, Authentication authentication) {
        return permissionEvaluator.hasGeneralPermission(authentication, "PRJ_READ")
                .flatMapMany(hasAccess -> {
                    if (hasAccess) {
                        return projectService.getProjectByName(name);
                    } else {
                        return Flux.empty();
                    }
                });
    }

    @GetMapping(params = "owner")
    public Flux<ProjectDto> getProjectByOwner(@RequestParam String owner, Authentication authentication) {
        return permissionEvaluator.hasGeneralPermission(authentication, "PRJ_READ")
                .flatMapMany(hasAccess -> {
                    if (hasAccess) {
                        return projectService.getProjectByCreatedBy(owner);
                    } else {
                        return Flux.empty();
                    }
                });
    }

    @GetMapping("/status/{status}")
    public Flux<ProjectDto> getProjectsByStatus(@PathVariable ProjectStatus status, Authentication authentication) {
        log.info("Received request to get projects with status: {}", status);

        return permissionEvaluator.hasGeneralPermission(authentication, "PRJ_READ")
                .flatMapMany(hasAccess -> {
                    if (hasAccess) {
                        return projectService.getProjectsByStatus(status);
                    } else {
                        return Flux.empty();
                    }
                });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ProjectDto>> updateProject(@PathVariable String id, @Valid @RequestBody ProjectDto projectDto, Authentication authentication) {
        log.info("Received request to update project with ID: {}", id);

        return permissionEvaluator.hasPermission(authentication, id, "PRJ_UPDATE")
                .flatMap(hasAccess -> {
                    if (hasAccess) {
                        return projectService.updateProject(id, projectDto)
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).<ProjectDto>build());
                    }
                });
    }

    @GetMapping("/{projectId}/tasks/ids")
    public Flux<String> getAllTaskIdsByProjectId(@PathVariable String projectId, Authentication authentication) {
        log.info("Received request to get all task IDs for project: {}", projectId);

        return permissionEvaluator.hasPermission(authentication, projectId, "PRJ_READ")
                .flatMapMany(hasAccess -> {
                    if (hasAccess) {
                        return projectService.getAllTaskIdsByProjectId(projectId);
                    } else {
                        return Flux.empty(); // Return empty flux if no permission
                    }
                });
    }

    @PostMapping("/{projectId}/tasks")
    public Mono<ResponseEntity<TaskDto>> createTaskForProject(
            @PathVariable String projectId,
            @Valid @RequestBody TaskDto taskDto,
            UriComponentsBuilder uriBuilder,
            Authentication authentication
    ) {
        log.info("Received request to create a task for project {}: {}", projectId, taskDto);

        return permissionEvaluator.hasPermission(authentication, projectId, "TASK_CREATE")
                .flatMap(hasAccess -> {
                    if (hasAccess) {
                        return projectService.createTaskForProject(projectId, taskDto)
                                .map(createdTask -> {
                                    URI location = uriBuilder.path("/projects/{projectId}/tasks/{taskId}")
                                            .buildAndExpand(projectId, createdTask.getId())
                                            .toUri();
                                    log.info("Task created for project {} with ID: {}", projectId, createdTask.getId());
                                    return ResponseEntity.created(location).body(createdTask);
                                });
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).<TaskDto>build());
                    }
                });
    }

    @PutMapping("/{id}/status")
    public Mono<ResponseEntity<ProjectDto>> updateProjectStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateProjectStatusRequestDto request,
            Authentication authentication
    ) {
        log.info("Received request to update status for project {} to {}", id, request.getNewStatus());

        return permissionEvaluator.hasPermission(authentication, id, "PRJ_STATUS_CHANGE")
                .flatMap(hasAccess -> {
                    if (hasAccess) {
                        return projectService.updateProjectStatus(id, request.getNewStatus(), request.getExpectedVersion())
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).<ProjectDto>build());
                    }
                });
    }

    @PutMapping("/{id}/combined")
    public Mono<ResponseEntity<ProjectDto>> updateProjectCombined(
            @PathVariable String id,
            @Valid @RequestBody ProjectDto dto,
            Authentication authentication
    ) {
        log.info("Received request to update project {} (combined fields)", id);

        return permissionEvaluator.hasPermission(authentication, id, "PRJ_UPDATE")
                .flatMap(hasAccess -> {
                    if (hasAccess) {
                        return projectService.updateProjectCombined(id, dto)
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.notFound().build());
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).<ProjectDto>build());
                    }
                });
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<ResponseEntity<Void>> deleteProject(@PathVariable String id, Authentication authentication) {
        log.info("Received request to delete project with ID: {}", id);

        return permissionEvaluator.hasPermission(authentication, id, "PRJ_DELETE")
                .flatMap(hasAccess -> {
                    if (hasAccess) {
                        return projectService.deleteProject(id)
                                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build());
                    }
                });
    }

    @GetMapping("/{id}/permissions/check")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkProjectPermissions(
            @PathVariable String id,
            @RequestParam String action,
            Authentication authentication) {
        log.info("Checking permissions for project ID: {} with action: {}", id, action);

        return permissionEvaluator.hasPermission(authentication, id, action)
                .map(hasAccess -> {
                    Map<String, Boolean> permissions = new HashMap<>();
                    permissions.put("hasAccess", hasAccess);
                    return ResponseEntity.ok(permissions);
                })
                .onErrorResume(e -> {
                    log.error("Error checking permissions for project {}: {}", id, e.getMessage());
                    Map<String, Boolean> permissions = new HashMap<>();
                    permissions.put("hasAccess", false);
                    return Mono.just(ResponseEntity.ok(permissions));
                });
    }

}
