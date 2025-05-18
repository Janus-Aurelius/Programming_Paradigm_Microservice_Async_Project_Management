package com.pm.projectservice.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import com.pm.commoncontracts.domain.ProjectStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Document(collection ="project")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Project {
// This class represents a Project entity in the MongoDB database
    @Id
    private String id;
    private String name;
    private String description;
    private ProjectStatus status;
    private String createdBy;
    private String assignedTo;
    private String startDate;
    private String endDate;
    @Builder.Default
    private List<String> taskIds = new ArrayList<>();
    @Builder.Default
    private List<String> memberIds = new ArrayList<>(); // User IDs of project members
    private String priority; // Optional: project-level priority
    @Version
    private Long version; // For optimistic locking

    public List<String> getMemberIds() {
        return memberIds;
    }
    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }
    public String getPriority() {
        return priority;
    }
    public void setPriority(String priority) {
        this.priority = priority;
    }
}
