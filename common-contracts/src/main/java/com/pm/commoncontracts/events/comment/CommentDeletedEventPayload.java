package com.pm.commoncontracts.events.comment;

import com.pm.commoncontracts.dto.CommentDto;

public record CommentDeletedEventPayload(CommentDto commentDto) {
    public static final String EVENT_TYPE = "COMMENT_DELETED";

}
