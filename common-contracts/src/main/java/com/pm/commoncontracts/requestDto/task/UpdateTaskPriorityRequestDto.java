package com.pm.commoncontracts.requestDto.task;

import com.pm.commoncontracts.domain.TaskPriority;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class UpdateTaskPriorityRequestDto {
    @NotNull
    private TaskPriority newPriority;
    @NotNull
    private Long expectedVersion;
}
