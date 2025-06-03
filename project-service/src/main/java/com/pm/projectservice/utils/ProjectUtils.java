package com.pm.projectservice.utils;

import com.pm.commoncontracts.dto.ProjectDto;
import com.pm.projectservice.model.Project;

public class ProjectUtils {

    public static ProjectDto entityToDto(Project project) {
        ProjectDto.ProjectDtoBuilder builder = ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .priority(project.getPriority()) // Map priority field
                .ownerId(project.getOwnerId())
                .managerIds(project.getManagerIds())
                .memberIds(project.getMemberIds())
                .assignedTo(project.getAssignedTo()) // Map assignedTo field
                .taskIds(project.getTaskIds()) // Map taskIds field
                .createdBy(project.getCreatedBy())
                .lastModifiedBy(project.getLastModifiedBy())
                .version(project.getVersion());
        // Convert Instant dates to String for DTO
        builder.startDate(project.getStartDate() != null ? project.getStartDate().toString() : null);
        builder.endDate(project.getEndDate() != null ? project.getEndDate().toString() : null);
        builder.createdAt(project.getCreatedAt() != null ? project.getCreatedAt().toString() : null);
        builder.updatedAt(project.getUpdatedAt() != null ? project.getUpdatedAt().toString() : null);
        return builder.build();
    }

    public static Project dtoToEntity(ProjectDto dto) {
        Project.ProjectBuilder builder = Project.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .priority(dto.getPriority()) // Map priority field
                .ownerId(dto.getOwnerId())
                .managerIds(dto.getManagerIds())
                .memberIds(dto.getMemberIds())
                .assignedTo(dto.getAssignedTo()) // Map assignedTo field
                .taskIds(dto.getTaskIds()) // Map taskIds field
                .createdBy(dto.getCreatedBy())
                .lastModifiedBy(dto.getLastModifiedBy())
                .version(dto.getVersion());
        // Convert String dates to Instant for entity
        builder.startDate(dto.getStartDate() != null ? java.time.Instant.parse(dto.getStartDate()) : null);
        builder.endDate(dto.getEndDate() != null ? java.time.Instant.parse(dto.getEndDate()) : null);
        // Note: createdAt and updatedAt are handled by Spring Data auditing, so no need to set them
        return builder.build();
    }
}
