package com.pm.commoncontracts.requestDto.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignTaskRequestDto {
    @NotBlank
    private String userId;
}