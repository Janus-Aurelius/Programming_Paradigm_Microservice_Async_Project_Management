package com.pm.taskservice.model;

import lombok.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
// import java.util.Date; // Replaced with Instant

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import com.pm.commoncontracts.domain.TaskPriority;
import com.pm.commoncontracts.domain.TaskStatus;

@Document(collection = "tasks")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    @Id
    private String id;

    @NotBlank
    @Indexed
    private String projectId; // Reference to Project

    @NotBlank
    @Size(min = 3, max = 255)
    private String name;

    @NotNull
    private TaskStatus status = TaskStatus.TODO; // Default status

    @NotNull
    private TaskPriority priority = TaskPriority.MEDIUM; // Default priority

    @Size(max = 10000)
    private String description;

    // Auditing fields will be populated by Spring Data Auditing
    @CreatedDate
    private Instant createdAt;

    @CreatedBy
    private String createdBy; // User ID

    @LastModifiedDate
    private Instant updatedAt;

    @LastModifiedBy
    private String updatedBy; // User ID

    private Instant dueDate;

    private String assigneeId; // User ID of current assignee

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    @Version
    private Long version;
}

