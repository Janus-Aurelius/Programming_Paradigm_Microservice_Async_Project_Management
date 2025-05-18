package com.pm.commoncontracts.dto;

import com.pm.commoncontracts.domain.ParentType;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data

public class CommentDto
{    String id;
     String parentId;  // ID of the task or project being commented on
     ParentType parentType; // "TASK" or "PROJECT"
     String content;
     String authorId;
     String username;   // Display name of the author
     String createdAt;
 }
