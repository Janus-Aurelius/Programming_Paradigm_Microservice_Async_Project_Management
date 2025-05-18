package com.pm.commoncontracts.requestDto;

import com.pm.commoncontracts.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequestDto(
    @NotNull TaskStatus newStatus,
    @NotNull Long expectedVersion
) {}