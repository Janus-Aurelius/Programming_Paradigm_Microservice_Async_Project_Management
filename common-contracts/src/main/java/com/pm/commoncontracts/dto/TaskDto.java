package com.pm.commoncontracts.dto;

import com.pm.commoncontracts.domain.TaskPriority;
import com.pm.commoncontracts.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.Date;

@Setter
@Getter
@AllArgsConstructor
@Builder
@Data

public class TaskDto {
    private String id;
    
    @NotBlank(message = "Project ID is required")
    private String projectId;
    
    @NotBlank(message = "Task name is required")
    private String name;
    
    @NotNull(message = "Task status is required")
    private TaskStatus status;
    
    private TaskPriority priority;
    private String description;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private Instant updatedAt;
    private Date dueDate;
    private String assigneeId;
    private String assigneeName;
    private String tags;
    private String attachments;
    private Long version;

    public TaskDto() {}

}
