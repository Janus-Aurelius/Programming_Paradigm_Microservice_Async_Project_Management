package com.pm.commoncontracts.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.pm.commoncontracts.domain.ProjectStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data

public class ProjectDto {
    private String id;
    private String name;
    private String description;
    private ProjectStatus status;
    private String ownerId;
    @Builder.Default
    private List<String> managerIds = new ArrayList<>();
    private String startDate;
    private String endDate;
    @Builder.Default
    private List<String> memberIds = new ArrayList<>(); // User IDs of project members
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String lastModifiedBy;
    private Long version;
    @Builder.Default
    private List<String> taskIds = new ArrayList<>();
    private String assignedTo;
    private String priority;

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
