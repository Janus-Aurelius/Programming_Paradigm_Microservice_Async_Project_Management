package com.pm.userservice.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id; // If you implement Auditable
import org.springframework.data.annotation.LastModifiedDate; // If you implement Auditable
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.pm.commoncontracts.domain.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; // Changed from Date

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
        @Indexed(unique = true)        private String email;
        
        @NotBlank // Password hash should not be blank
        @Field("passwordHash") // Map to MongoDB field name
        private String hashedPassword; // Java field name
        
        @Builder.Default
        private UserRole role = UserRole.ROLE_USER; // Single role per user - simplified architecture

        @Builder.Default
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

        private Instant lastLogin; // Optional: for auditing        @Builder.Default
        private boolean emailVerified = false; // Default to false
        
        @Builder.Default
        private boolean locked = false; // Default to false

        @Size(max = 255)
        private String profilePictureUrl; // Optional: for avatars

        // Optional: For auditing who created/modified the user record itself
        // @CreatedBy
        // private String createdByUserId;
        // @LastModifiedBy
        // private String lastModifiedByUserId;
}