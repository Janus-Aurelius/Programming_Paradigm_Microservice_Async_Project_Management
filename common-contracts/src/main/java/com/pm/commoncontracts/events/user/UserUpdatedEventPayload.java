package com.pm.commoncontracts.events.user;

import com.pm.commoncontracts.dto.UserDto;

public record UserUpdatedEventPayload(UserDto userDto) {
    public static final String EVENT_TYPE = "USER_UPDATED";
}