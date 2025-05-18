package com.pm.taskservice.model;

import lombok.*;

import java.time.Instant;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import com.pm.commoncontracts.domain.TaskPriority;
import com.pm.commoncontracts.domain.TaskStatus;
//Client → TaskController → TaskService → TaskRepository → MongoDB

@Document(collection="tasks")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    @Id
    private String id;
    private String projectId;
    private String name;
    private TaskStatus status;
    private TaskPriority priority;
    private String description;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private Instant updatedAt;
    private Date dueDate;
    private String assigneeId; // ID of the assigned user
    private String tags;
    private String attachments;
    @Version
    private Long version;
}
