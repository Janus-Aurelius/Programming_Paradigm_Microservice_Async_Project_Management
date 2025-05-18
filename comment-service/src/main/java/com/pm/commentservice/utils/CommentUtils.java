package com.pm.commentservice.utils;
import com.pm.commoncontracts.dto.CommentDto;
import com.pm.commentservice.model.Comment;
import org.springframework.beans.BeanUtils;
public class CommentUtils {
    public static Comment dtoToEntity(CommentDto commentDto) {
        Comment commentEntity = new Comment();
        BeanUtils.copyProperties(commentDto, commentEntity);
        return commentEntity;
    }

    public static CommentDto entityToDto(Comment comment) {
        CommentDto commentDto = new CommentDto();
        BeanUtils.copyProperties(comment, commentDto);
        return commentDto;
    }
}
