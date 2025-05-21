package com.pm.userservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy; // If you implement Auditable
import org.springframework.data.annotation.LastModifiedBy; // If you implement Auditable
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant; // Changed from Date
import java.util.List;
// import java.util.UUID; // Not used for id, MongoDB ObjectId is typical

@Document(collection = "users")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
        @Id
        private String id; // MongoDB ObjectId will be stored as String

        @NotBlank
        @Size(min = 3, max = 50)
        @Indexed(unique = true)
        private String username;

        @NotBlank
        @Email
        @Size(max = 100)
        @Indexed(unique = true)
        private String email;

        @NotBlank // Password hash should not be blank
        private String passwordHash; // Store ONLY the hash (e.g., BCrypt)

        @Builder.Default
        private List<String> roles = List.of("ROLE_USER"); // e.g., ["ROLE_USER", "ROLE_ADMIN"], provide a default

        private boolean enabled = true; // Default to enabled

        @Size(max = 50)
        private String firstName;

        @Size(max = 50)
        private String lastName;

        // fullName can be derived, consider if storing it is necessary
        // If stored, ensure it's updated when firstName/lastName change.
        // Alternatively, derive in DTOs or service layer.
        // private String fullName;

        @CreatedDate
        private Instant createdAt;

        @LastModifiedDate
        private Instant updatedAt;

        private Instant lastLogin; // Optional: for auditing

        private boolean emailVerified = false; // Default to false
        private boolean locked = false;        // Default to false

        @Size(max = 255)
        private String profilePictureUrl; // Optional: for avatars

        // Optional: For auditing who created/modified the user record itself
        // @CreatedBy
        // private String createdByUserId;
        // @LastModifiedBy
        // private String lastModifiedByUserId;
}