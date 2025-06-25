package com.pm.taskservice.utils;

import com.pm.commoncontracts.dto.TaskDto;
import com.pm.taskservice.model.Task;

public class TaskUtils {

    private static String sanitizeObjectIdString(String id) {
        if (id == null) {
            return null;
        }
        if (id.startsWith("ObjectId(\"") && id.endsWith("\")")) {
            return id.substring(10, id.length() - 2);
        }
        return id;
    }

    public static TaskDto entityToDto(com.pm.taskservice.model.Task task) {
        TaskDto.TaskDtoBuilder builder = TaskDto.builder()
                .id(sanitizeObjectIdString(task.getId()))
                .projectId(sanitizeObjectIdString(task.getProjectId()))
                .name(task.getName())
                .status(task.getStatus())
                .priority(task.getPriority())
                .description(task.getDescription())
                .createdBy(sanitizeObjectIdString(task.getCreatedBy()))
                .updatedBy(sanitizeObjectIdString(task.getUpdatedBy()))
                .assigneeId(sanitizeObjectIdString(task.getAssigneeId()))
                .tags(task.getTags())
                .version(task.getVersion());

        // Convert date fields to String format for DTO
        builder.createdAt(task.getCreatedAt() != null ? task.getCreatedAt().toString() : null);
        builder.updatedAt(task.getUpdatedAt() != null ? task.getUpdatedAt().toString() : null);
        builder.dueDate(task.getDueDate() != null ? task.getDueDate().toString() : null);

        // Map attachment URLs
        if (task.getAttachments() != null) {
            builder.attachmentUrls(task.getAttachments().stream().map(com.pm.taskservice.model.Attachment::getUrl).toList());
        }
        return builder.build();
    }

    public static Task dtoToEntity(TaskDto dto) {
        Task.TaskBuilder builder = Task.builder()
                .id(dto.getId())
                .projectId(dto.getProjectId())
                .name(dto.getName())
                .status(dto.getStatus())
                .priority(dto.getPriority())
                .description(dto.getDescription())
                .createdBy(dto.getCreatedBy())
                .updatedBy(dto.getUpdatedBy())
                .assigneeId(dto.getAssigneeId())
                .tags(dto.getTags())
                .version(dto.getVersion());

        // Parse date fields from String format for entity
        if (dto.getCreatedAt() != null && !dto.getCreatedAt().trim().isEmpty()) {
            try {
                builder.createdAt(java.time.Instant.parse(dto.getCreatedAt()));
            } catch (Exception e) {
                // Skip invalid date format
            }
        }
        if (dto.getUpdatedAt() != null && !dto.getUpdatedAt().trim().isEmpty()) {
            try {
                builder.updatedAt(java.time.Instant.parse(dto.getUpdatedAt()));
            } catch (Exception e) {
                // Skip invalid date format
            }
        }
        if (dto.getDueDate() != null && !dto.getDueDate().trim().isEmpty()) {
            try {
                builder.dueDate(java.time.Instant.parse(dto.getDueDate()));
            } catch (Exception e) {
                // Skip invalid date format
            }
        }

        // Do not map attachmentUrls to attachments - attachments are managed separately
        return builder.build();
    }

}
