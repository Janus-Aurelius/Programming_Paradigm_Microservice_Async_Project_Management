package com.pm.commoncontracts.events.project;

import com.pm.commoncontracts.dto.ProjectDto;

public record ProjectPriorityChangedEventPayload(ProjectDto projectDto) {

    public static final String EVENT_TYPE = "PROJECT_PRIORITY_CHANGED";
}
