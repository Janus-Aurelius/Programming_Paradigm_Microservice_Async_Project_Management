package com.pm.commoncontracts.events.task;

import com.pm.commoncontracts.dto.TaskDto;

public record TaskDeletedEventPayload (TaskDto taskDto) {
    public static final String EVENT_TYPE = "TASK_DELETED";
}
