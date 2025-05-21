package com.pm.commoncontracts.domain;

public enum NotificationChannel {
    WEBSOCKET,  // For real-time in-app updates
    EMAIL,      // For email notifications
    PUSH,       // For mobile/desktop push notifications
    IN_APP_FEED // A dedicated feed within the application
}
