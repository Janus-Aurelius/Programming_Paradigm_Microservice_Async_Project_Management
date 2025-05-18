package com.pm.commoncontracts.events.project;

import com.pm.commoncontracts.dto.ProjectDto;

public record ProjectTasksFetchedEventPayload (ProjectDto projectDto) {
    public static final String EVENT_TYPE = "PROJECT_TASKS_FETCHED";
}


