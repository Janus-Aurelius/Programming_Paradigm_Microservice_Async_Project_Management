package com.pm.commoncontracts.requestDto.project;

import com.pm.commoncontracts.domain.ProjectPriority;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectPriorityRequestDto {

    @NotNull
    private ProjectPriority newPriority;
    private Long expectedVersion;
}
