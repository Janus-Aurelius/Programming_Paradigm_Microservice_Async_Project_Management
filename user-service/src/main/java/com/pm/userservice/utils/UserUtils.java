package com.pm.userservice.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder; // Required for UserDto.getRole() and User.getRole() types

import com.pm.commoncontracts.dto.UserDto;       // Required for User type
import com.pm.userservice.model.User;

public class UserUtils {

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail()) // Do not map hashed password back to DTO password field
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole()) // Direct single role mapping
                .enabled(user.isEnabled())
                .active(user.isActive()) // Map separate active field
                .lastLogin(user.getLastLogin() != null ? user.getLastLogin().toString() : null)
                .profilePictureUrl(user.getProfilePictureUrl())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null)
                .build();
    }    // Method for authentication that includes the hashed password

    public static UserDto toDtoWithPassword(User user) {
        if (user == null) {
            return null;
        }
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail()).password(user.getHashedPassword()) // Include hashed password for authentication
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole()) // Direct single role mapping
                .enabled(user.isEnabled())
                .active(user.isActive()) // Map separate active field
                .lastLogin(user.getLastLogin() != null ? user.getLastLogin().toString() : null)
                .profilePictureUrl(user.getProfilePictureUrl())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null)
                .build();
    }// Get user role as string for JWT generation - simplified for single role

    public static String getRoleAsString(User user) {
        if (user == null || user.getRole() == null) {
            return "ROLE_USER"; // Default role
        }
        return user.getRole().name();
    }

    public static User toEntity(UserDto userDto) {
        if (userDto == null) {
            return null;
        }
        User.UserBuilder builder = User.builder()
                .id(userDto.getId()).username(userDto.getUsername())
                .email(userDto.getEmail())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .profilePictureUrl(userDto.getProfilePictureUrl());

        // Set single role
        if (userDto.getRole() != null) {
            builder.role(userDto.getRole());
        }        // Set enabled/active status separately
        if (userDto.getEnabled() != null) {
            builder.enabled(userDto.getEnabled());
        }
        if (userDto.getActive() != null) {
            builder.active(userDto.getActive());
        }

        // Handle dates - parse from ISO strings if provided
        if (userDto.getLastLogin() != null && !userDto.getLastLogin().trim().isEmpty()) {
            try {
                builder.lastLogin(java.time.Instant.parse(userDto.getLastLogin()));
            } catch (Exception e) {
                // Skip invalid date format
            }
        }

        if (userDto.getPassword() != null && !userDto.getPassword().trim().isEmpty()) {
            builder.hashedPassword(passwordEncoder.encode(userDto.getPassword().trim()));
        }

        // Note: createdAt and updatedAt are handled by Spring Data auditing
        return builder.build();
    }

    public static User updateUserFromDto(User existingUser, UserDto userDto) {
        if (userDto == null || existingUser == null) {
            return existingUser;
        }

        if (userDto.getUsername() != null) {
            existingUser.setUsername(userDto.getUsername());
        }
        if (userDto.getEmail() != null) {
            existingUser.setEmail(userDto.getEmail());
        }
        if (userDto.getFirstName() != null) {
            existingUser.setFirstName(userDto.getFirstName());
        }
        if (userDto.getLastName() != null) {
            existingUser.setLastName(userDto.getLastName());
        }
        if (userDto.getRole() != null) {
            existingUser.setRole(userDto.getRole()); // Updates the single UserRole field
        }        // Handle new fields - enabled and active separately
        if (userDto.getEnabled() != null) {
            existingUser.setEnabled(userDto.getEnabled());
        }
        if (userDto.getActive() != null) {
            existingUser.setActive(userDto.getActive());
        }

        if (userDto.getProfilePictureUrl() != null) {
            existingUser.setProfilePictureUrl(userDto.getProfilePictureUrl());
        }

        // Handle date fields
        if (userDto.getLastLogin() != null && !userDto.getLastLogin().trim().isEmpty()) {
            try {
                existingUser.setLastLogin(java.time.Instant.parse(userDto.getLastLogin()));
            } catch (Exception e) {
                // Skip invalid date format
            }
        }

        // Handle password update: only if a new password is provided in DTO
        if (userDto.getPassword() != null && !userDto.getPassword().trim().isEmpty()) {
            existingUser.setHashedPassword(passwordEncoder.encode(userDto.getPassword().trim()));
        }

        return existingUser;
    }

    // Static method to access password encoder, e.g., for password comparison or direct encoding in service.
    public static PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
