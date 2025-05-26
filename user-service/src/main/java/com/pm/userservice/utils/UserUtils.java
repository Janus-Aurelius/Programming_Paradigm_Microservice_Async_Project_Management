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
        }        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                // Do not map hashed password back to DTO password field
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole()) // Direct single role mapping
                .build();
    }

    // Method for authentication that includes the hashed password
    public static UserDto toDtoWithPassword(User user) {
        if (user == null) {
            return null;
        }        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getHashedPassword()) // Include hashed password for authentication
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole()) // Direct single role mapping
                .build();
    }    // Get user role as string for JWT generation - simplified for single role
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
                .id(userDto.getId())                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName());

        // Set single role
        if (userDto.getRole() != null) {
            builder.role(userDto.getRole());
        }

        if (userDto.getPassword() != null && !userDto.getPassword().trim().isEmpty()) {
            builder.hashedPassword(passwordEncoder.encode(userDto.getPassword().trim()));
        }
        
        // User entity's @Builder.Default handles initial values for 'enabled', 'emailVerified', 'locked', and the 'role'.
        // 'createdAt' and 'updatedAt' are handled by Spring Data auditing.
        // If userDto.getRole() is null, user.role will be null after this. UserService can set a default if needed.

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
        }        if (userDto.getRole() != null) {
            existingUser.setRole(userDto.getRole()); // Updates the single UserRole field
        }

        // Handle password update: only if a new password is provided in DTO
        if (userDto.getPassword() != null && !userDto.getPassword().trim().isEmpty()) {
            existingUser.setHashedPassword(passwordEncoder.encode(userDto.getPassword().trim()));
        }
          // The 'role' field (UserRole) in User entity is not directly managed by this method from UserDto.role.
        // It defaults via @Builder.Default.
        // Fields like 'enabled', 'locked', 'emailVerified' are not in UserDto and thus not updated here.

        return existingUser;
    }

    // Static method to access password encoder, e.g., for password comparison or direct encoding in service.
    public static PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
