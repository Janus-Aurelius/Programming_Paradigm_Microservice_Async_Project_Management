package com.pm.taskservice.utils;

import com.pm.commoncontracts.dto.TaskDto;
import com.pm.taskservice.model.Task;


public class TaskUtils {
    public static TaskDto entityToDto(com.pm.taskservice.model.Task task) {
        TaskDto.TaskDtoBuilder builder = TaskDto.builder()
            .id(task.getId())
            .projectId(task.getProjectId())
            .name(task.getName())
            .status(task.getStatus())
            .priority(task.getPriority())
            .description(task.getDescription())
            .createdBy(task.getCreatedBy())
            .createdAt(task.getCreatedAt())
            .updatedBy(task.getUpdatedBy())
            .updatedAt(task.getUpdatedAt())
            .dueDate(task.getDueDate() != null ? java.util.Date.from(task.getDueDate()) : null)
            .assigneeId(task.getAssigneeId())
            .tags(task.getTags())
            .version(task.getVersion());
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
            .createdAt(dto.getCreatedAt())
            .updatedBy(dto.getUpdatedBy())
            .updatedAt(dto.getUpdatedAt())
            .dueDate(dto.getDueDate() != null ? dto.getDueDate().toInstant() : null)
            .assigneeId(dto.getAssigneeId())
            .tags(dto.getTags())
            .version(dto.getVersion());
        // Do not map attachmentUrls to attachments
        return builder.build();
    }

}
