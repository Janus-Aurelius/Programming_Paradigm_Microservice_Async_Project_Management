package com.pm.commoncontracts.requestDto.task;

import com.pm.commoncontracts.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class UpdateTaskStatusRequestDto {
    @NotNull
    private TaskStatus newStatus;

    @NotNull
    private Long expectedVersion;
}
