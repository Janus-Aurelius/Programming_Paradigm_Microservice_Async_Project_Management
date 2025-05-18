package com.pm.commoncontracts.dto;


import java.util.ArrayList;
import java.util.List;

import com.pm.commoncontracts.domain.ProjectStatus;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data

public class ProjectDto {
    private String id;
    private String name;
    private String description;
    private ProjectStatus status;
    private String createdBy;
    private String OwnerId;
    private String assignedTo;
    private String startDate;
    private String endDate;
    private Long version;
    @Builder.Default
    private List<String> taskIds = new ArrayList<>();
    @Setter
    @Getter
    @Builder.Default
    private List<String> memberIds = new ArrayList<>(); // User IDs of project members
    @Setter
    @Getter
    private String priority; // Optional: project-level priority

}
