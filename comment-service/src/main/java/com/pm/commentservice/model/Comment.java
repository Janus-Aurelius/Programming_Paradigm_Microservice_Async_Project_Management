package com.pm.commentservice.model;


import com.pm.commoncontracts.domain.ParentType;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
@Data
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "comments")
public class Comment {
    @Id
    private String id;

    @Indexed // Index for efficient lookup by parent
    private String parentId; // ID of the Task, Project, etc.
    @Indexed
    private ParentType parentType; // Enum to distinguish parent type

    private String userId; // ID of the user who wrote the comment
    private String username; // Denormalized username (optional, for display)
    private String content; // The actual comment text
    private Instant createdAt;
    private Instant updatedAt;



    // Constructor for creation
    public void Comment(String parentId, ParentType parentType, String userId, String username, String content) {
        this.parentId = parentId;
        this.parentType = parentType;
        this.userId = userId;
        this.username = username; // Store username at creation time
        this.content = content;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
}
