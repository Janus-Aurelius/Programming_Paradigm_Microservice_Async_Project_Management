package com.pm.commentservice.utils;

import com.pm.commentservice.model.Comment;
import com.pm.commoncontracts.dto.CommentDto;

public class CommentUtils {

    private static String sanitizeObjectIdString(String id) {
        if (id == null) {
            return null;
        }
        if (id.startsWith("ObjectId(\"") && id.endsWith("\")")) {
            return id.substring(10, id.length() - 2);
        }
        return id;
    }

    public static Comment dtoToEntity(CommentDto dto) {
        return Comment.builder()
                .id(sanitizeObjectIdString(dto.getId()))
                .parentId(sanitizeObjectIdString(dto.getParentId()))
                .parentType(dto.getParentType())
                .content(dto.getContent())
                .userId(sanitizeObjectIdString(dto.getAuthorId()))
                .displayName(dto.getUsername() != null ? dto.getUsername() : dto.getDisplayName())
                .createdAt(dto.getCreatedAt() != null ? java.time.Instant.parse(dto.getCreatedAt()) : null)
                .updatedAt(dto.getUpdatedAt() != null ? java.time.Instant.parse(dto.getUpdatedAt()) : null)
                .parentCommentId(sanitizeObjectIdString(dto.getParentCommentId()))
                .version(dto.getVersion())
                .deleted(dto.isDeleted())
                .build();
    }

    public static CommentDto entityToDto(Comment comment) {
        return CommentDto.builder()
                .id(sanitizeObjectIdString(comment.getId()))
                .parentId(sanitizeObjectIdString(comment.getParentId()))
                .parentType(comment.getParentType())
                .content(comment.getContent())
                .authorId(sanitizeObjectIdString(comment.getUserId()))
                .username(comment.getDisplayName())
                .displayName(comment.getDisplayName())
                .createdAt(comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : null)
                .updatedAt(comment.getUpdatedAt() != null ? comment.getUpdatedAt().toString() : null)
                .parentCommentId(sanitizeObjectIdString(comment.getParentCommentId()))
                .version(comment.getVersion())
                .deleted(comment.isDeleted())
                .build();
    }
}
