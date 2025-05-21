package com.pm.commentservice.model;

import com.pm.commoncontracts.domain.ParentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy; // For userId if it's the creator
// import org.springframework.data.annotation.LastModifiedBy; // Comments usually aren't "modified by" another user
import org.springframework.data.annotation.Version;
// import org.springframework.data.mongodb.config.EnableMongoAuditing; // This goes in @Configuration
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "comments")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
// @EnableMongoAuditing // Remove: This annotation should be on a @Configuration class, not an entity.
public class Comment {

    @Id
    private String id;

    /* ------- contextual linkage ------- */
    @NotBlank
    @Indexed
    private String parentId; // e.g. Task ID or Project ID

    @NotNull
    @Indexed
    private ParentType parentType; // TASK, PROJECT, etc.

    private String parentCommentId; // null for top-level; supports threaded replies

    /* ------- author ------- */
    @NotBlank
    // If userId is always the creator, you can use @CreatedBy
    // @CreatedBy
    private String userId; // author’s user id

    @Size(max = 100)
    private String displayName; // capture at post time (optional, denormalized)

    /* ------- content ------- */
    @NotBlank
    @TextIndexed // enables Mongo full-text search
    @Size(min = 1, max = 4096) // guard against abuse
    private String content;

    /* ------- timestamps & versioning ------- */
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate // For edits to the comment
    private Instant updatedAt;

    @Version
    private Long version; // optimistic locking

    /* ------- soft delete & moderation (optional) ------- */
    private boolean deleted = false; // Default to not deleted
}