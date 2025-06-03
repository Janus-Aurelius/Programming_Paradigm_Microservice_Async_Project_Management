package com.pm.commoncontracts.dto;

import com.pm.commoncontracts.domain.UserRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private String id;
    private String username;
    private String email;
    private String password; // For input only, will not be stored as is
    private String firstName;
    private String lastName;
    private UserRole role; // Added UserRole
    private Boolean enabled;
    private Boolean active;
    private String lastLogin;
    private String profilePictureUrl;
    private String createdAt;
    private String updatedAt;
}
