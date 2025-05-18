package com.pm.commoncontracts.requestDto.project;

import com.pm.commoncontracts.domain.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProjectStatusRequestDto {
    @NotNull
    private ProjectStatus newStatus;
    private Long expectedVersion;
}
