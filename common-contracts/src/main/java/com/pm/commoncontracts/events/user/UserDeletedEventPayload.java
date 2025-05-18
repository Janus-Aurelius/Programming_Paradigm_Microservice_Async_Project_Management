package com.pm.commoncontracts.events.user;

import com.pm.commoncontracts.dto.UserDto;

public record UserDeletedEventPayload(UserDto userDto) {
    public static final String EVENT_TYPE = "USER_DELETED";
}