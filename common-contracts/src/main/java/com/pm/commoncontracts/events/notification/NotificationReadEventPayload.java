package com.pm.commoncontracts.events.notification;

import com.pm.commoncontracts.dto.NotificationDto;

/**
 * Event payload published when a user marks a notification as read.
 */
public record NotificationReadEventPayload(NotificationDto notificationDto) {

    public static final String EVENT_TYPE = "NOTIFICATION_READ";
}
