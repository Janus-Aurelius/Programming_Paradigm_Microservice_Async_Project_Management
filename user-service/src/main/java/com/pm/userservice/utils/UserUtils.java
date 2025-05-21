package com.pm.userservice.utils;

import com.pm.commoncontracts.dto.UserDto;

public class UserUtils {
    public static UserDto entityToDto(com.pm.userservice.model.User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setFirstName(user.getFirstName());
        userDto.setLastName(user.getLastName());
        userDto.setPasswordHash(user.getPasswordHash());
        userDto.setRoles(user.getRoles());
        userDto.setEnabled(user.isEnabled());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setUpdatedAt(user.getUpdatedAt());
        userDto.setLastLogin(user.getLastLogin());
        userDto.setEmailVerified(user.isEmailVerified());
        userDto.setLocked(user.isLocked());
        userDto.setProfilePictureUrl(user.getProfilePictureUrl());
        // Map single role for backward compatibility if needed
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            userDto.setRole(user.getRoles().get(0));
        }
        // Password and name are not mapped from entity for security/consistency
        return userDto;
    }

    public static com.pm.userservice.model.User dtoToEntity(UserDto userDto) {
        com.pm.userservice.model.User userEntity = new com.pm.userservice.model.User();
        userEntity.setId(userDto.getId());
        userEntity.setUsername(userDto.getUsername());
        userEntity.setEmail(userDto.getEmail());
        userEntity.setFirstName(userDto.getFirstName());
        userEntity.setLastName(userDto.getLastName());
        userEntity.setPasswordHash(userDto.getPasswordHash());
        userEntity.setRoles(userDto.getRoles() != null ? userDto.getRoles() : java.util.List.of(userDto.getRole() != null ? userDto.getRole() : "ROLE_USER"));
        userEntity.setEnabled(userDto.isEnabled());
        userEntity.setCreatedAt(userDto.getCreatedAt());
        userEntity.setUpdatedAt(userDto.getUpdatedAt());
        userEntity.setLastLogin(userDto.getLastLogin());
        userEntity.setEmailVerified(userDto.isEmailVerified());
        userEntity.setLocked(userDto.isLocked());
        userEntity.setProfilePictureUrl(userDto.getProfilePictureUrl());
        // Password and name are not mapped to entity for security/consistency
        return userEntity;
    }
}
