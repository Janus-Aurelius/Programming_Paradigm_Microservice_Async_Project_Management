package com.pm.projectservice.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.pm.commoncontracts.domain.ProjectPriority;
import com.pm.commoncontracts.domain.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "projects") // Consistent plural naming
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Project {

    @Id
    private String id;

    @NotBlank
    @Size(min = 3, max = 100)
    @Indexed(unique = true) // If project names must be unique
    private String name;

    @Size(max = 5000)
    private String description;

    @NotNull
    private ProjectStatus status = ProjectStatus.PLANNING; // Provide a default

    /** User who created / owns the project */
    @NotBlank
    @Indexed
    private String ownerId; // Renamed from createdBy in V1 for clarity if it means owner

    /** Optional list of additional project managers / leads */
    @Builder.Default
    private List<String> managerIds = new ArrayList<>();

    private Instant startDate; // Consider if this should be @NotNull
    private Instant endDate;

    @NotNull
    private ProjectPriority priority = ProjectPriority.MEDIUM; // Provide a default

    /** Direct project members (for ACLs, mentions, etc.) */
    @Builder.Default
    private List<String> memberIds = new ArrayList<>();

    @Builder.Default
    private List<String> taskIds = new ArrayList<>();

    // Add assignedTo field for project assignment
    private String assignedTo;

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public List<String> getTaskIds() {
        return taskIds;
    }

    public void setTaskIds(List<String> taskIds) {
        this.taskIds = taskIds;
    }

    // Add setPriority/getPriority for ProjectPriority
    public ProjectPriority getPriority() {
        return priority;
    }

    public void setPriority(ProjectPriority priority) {
        this.priority = priority;
    }

    // Add setStartDate/getStartDate for Instant
    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    /* ---------- auditing ---------- */
    @CreatedDate
    private Instant createdAt;

    @CreatedBy // User ID from SecurityContext via AuditorAware
    private String createdBy;

    @LastModifiedDate
    private Instant updatedAt;

    @LastModifiedBy // User ID from SecurityContext via AuditorAware
    private String lastModifiedBy;

    /* ---------- optimistic locking ---------- */
    @Version
    private Long version;
}
