package com.pm.userservice.dto;

import com.pm.commoncontracts.dto.UserDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private UserDto user;
}
