package com.pm.projectservice.utils;

import com.pm.commoncontracts.dto.ProjectDto;
import com.pm.projectservice.model.Project;

public class ProjectUtils {

    private static String sanitizeObjectIdString(String id) {
        if (id == null) {
            return null;
        }
        if (id.startsWith("ObjectId(\"") && id.endsWith("\")")) {
            return id.substring(10, id.length() - 2);
        }
        return id;
    }

    private static java.util.List<String> sanitizeObjectIdList(java.util.List<String> ids) {
        if (ids == null) {
            return null;
        }
        return ids.stream().map(ProjectUtils::sanitizeObjectIdString).collect(java.util.stream.Collectors.toList());
    }

    public static ProjectDto entityToDto(Project project) {
        ProjectDto.ProjectDtoBuilder builder = ProjectDto.builder()
                .id(sanitizeObjectIdString(project.getId()))
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .priority(project.getPriority())
                .ownerId(sanitizeObjectIdString(project.getOwnerId()))
                .managerIds(sanitizeObjectIdList(project.getManagerIds()))
                .memberIds(sanitizeObjectIdList(project.getMemberIds()))
                .assignedTo(sanitizeObjectIdString(project.getAssignedTo()))
                .taskIds(sanitizeObjectIdList(project.getTaskIds()))
                .createdBy(sanitizeObjectIdString(project.getCreatedBy()))
                .lastModifiedBy(sanitizeObjectIdString(project.getLastModifiedBy()))
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
