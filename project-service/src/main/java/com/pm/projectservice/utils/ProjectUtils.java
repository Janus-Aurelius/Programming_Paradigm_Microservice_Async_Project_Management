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
            .ownerId(project.getOwnerId())
            .managerIds(project.getManagerIds())
            .memberIds(project.getMemberIds())
            .createdAt(project.getCreatedAt())
            .createdBy(project.getCreatedBy())
            .updatedAt(project.getUpdatedAt())
            .lastModifiedBy(project.getLastModifiedBy())
            .version(project.getVersion());
        builder.startDate(project.getStartDate() != null ? project.getStartDate().toString() : null);
        builder.endDate(project.getEndDate() != null ? project.getEndDate().toString() : null);
        // ProjectDto does not have a priority field, so skip mapping it
        // Do not set taskIds here (should be set by service if needed)
        return builder.build();
    }

    public static Project dtoToEntity(ProjectDto dto) {
        Project.ProjectBuilder builder = Project.builder()
            .id(dto.getId())
            .name(dto.getName())
            .description(dto.getDescription())
            .status(dto.getStatus())
            .ownerId(dto.getOwnerId())
            .managerIds(dto.getManagerIds())
            .memberIds(dto.getMemberIds())
            .createdAt(dto.getCreatedAt())
            .createdBy(dto.getCreatedBy())
            .updatedAt(dto.getUpdatedAt())
            .lastModifiedBy(dto.getLastModifiedBy())
            .version(dto.getVersion());
        builder.startDate(dto.getStartDate() != null ? java.time.Instant.parse(dto.getStartDate()) : null);
        builder.endDate(dto.getEndDate() != null ? java.time.Instant.parse(dto.getEndDate()) : null);
        // ProjectDto does not have a priority field, so skip mapping it
        // Ignore taskIds (not persisted in entity)
        return builder.build();
    }
}
