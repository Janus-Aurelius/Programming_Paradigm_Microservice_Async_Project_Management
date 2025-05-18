package com.pm.commoncontracts.events.comment;

import com.pm.commoncontracts.dto.CommentDto;

public record CommentAddedEventPayload(CommentDto commentDto) {
    public static final String EVENT_TYPE = "COMMENT_ADDED";
}
