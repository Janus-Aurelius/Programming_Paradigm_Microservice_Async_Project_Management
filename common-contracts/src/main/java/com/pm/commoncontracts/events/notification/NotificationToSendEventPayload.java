package com.pm.commoncontracts.events.notification;

import com.pm.commoncontracts.dto.NotificationDto;

public record NotificationToSendEventPayload(
    NotificationDto notification,
    String correlationId
) {
    public static final String EVENT_TYPE = "NOTIFICATION_TO_SEND";
}
