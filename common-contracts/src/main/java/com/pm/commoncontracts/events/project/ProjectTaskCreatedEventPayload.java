package com.pm.commoncontracts.events.project;

import com.pm.commoncontracts.dto.TaskDto;

public record ProjectTaskCreatedEventPayload(TaskDto taskDto, String projectId) {
    public static final String EVENT_TYPE = "PROJECT_TASK_CREATED";
}