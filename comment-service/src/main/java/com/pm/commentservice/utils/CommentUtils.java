package com.pm.commentservice.utils;

import com.pm.commentservice.model.Comment;
import com.pm.commoncontracts.dto.CommentDto;

public class CommentUtils {

    public static Comment dtoToEntity(CommentDto dto) {
        return Comment.builder()
            .id(dto.getId())
            .parentId(dto.getParentId())
            .parentType(dto.getParentType())
            .content(dto.getContent())
            .userId(dto.getAuthorId())
            .displayName(dto.getUsername() != null ? dto.getUsername() : dto.getDisplayName())
            .createdAt(dto.getCreatedAt() != null ? java.time.Instant.parse(dto.getCreatedAt()) : null)
            .updatedAt(dto.getUpdatedAt() != null ? java.time.Instant.parse(dto.getUpdatedAt()) : null)
            .parentCommentId(dto.getParentCommentId())
            .version(dto.getVersion())
            .deleted(dto.isDeleted())
            .build();
    }

    public static CommentDto entityToDto(Comment comment) {
        return CommentDto.builder()
            .id(comment.getId())
            .parentId(comment.getParentId())
            .parentType(comment.getParentType())
            .content(comment.getContent())
            .authorId(comment.getUserId())
            .username(comment.getDisplayName())
            .displayName(comment.getDisplayName())
            .createdAt(comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : null)
            .updatedAt(comment.getUpdatedAt() != null ? comment.getUpdatedAt().toString() : null)
            .parentCommentId(comment.getParentCommentId())
            .version(comment.getVersion())
            .deleted(comment.isDeleted())
            .build();
    }
}
