package com.pm.commoncontracts.events.task;

import com.pm.commoncontracts.dto.TaskDto;

public record TaskPriorityChangedEventPayload(TaskDto dto) {
    public static final String EVENT_TYPE = "TASK_PRIORITY_CHANGED";
}
