package com.pm.notificationservice.utils;

import com.pm.commoncontracts.dto.NotificationDto;
import com.pm.notificationservice.model.Notification;

public class NotificationUtils {
    public static NotificationDto entityToDto(Notification notification) {
        return NotificationDto.builder()
            .id(notification.getId())
            .recipientUserId(notification.getRecipientUserId())
            .event(notification.getEvent() != null ? notification.getEvent().name() : null)
            .eventType(notification.getEvent() != null ? notification.getEvent().name() : null)
            .entityType(notification.getEntityType() != null ? notification.getEntityType().name() : null)
            .entityId(notification.getEntityId())
            .channel(notification.getChannel() != null ? notification.getChannel().name() : null)
            .payload(notification.getPayload())
            .message(notification.getMessage())
            .read(notification.isRead())
            .isRead(notification.isRead())
            .readAt(notification.getReadAt())
            .createdAt(notification.getCreatedAt())
            .version(notification.getVersion())
            .timestamp(notification.getCreatedAt() != null ? notification.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
            .build();
    }

    public static Notification dtoToEntity(NotificationDto dto) {
        return Notification.builder()
            .id(dto.getId())
            .recipientUserId(dto.getRecipientUserId())
            .event(dto.getEvent() != null ? com.pm.commoncontracts.events.notification.NotificationEvent.valueOf(dto.getEvent()) : null)
            .entityType(dto.getEntityType() != null ? com.pm.commoncontracts.domain.ParentType.valueOf(dto.getEntityType()) : null)
            .entityId(dto.getEntityId())
            .channel(dto.getChannel() != null ? com.pm.commoncontracts.domain.NotificationChannel.valueOf(dto.getChannel()) : null)
            .payload(dto.getPayload())
            .message(dto.getMessage())
            .read(dto.isRead())
            .readAt(dto.getReadAt())
            .createdAt(dto.getCreatedAt())
            .version(dto.getVersion())
            .build();
    }

    public static com.pm.commoncontracts.events.notification.NotificationToSendEventPayload toNotificationToSendEventPayload(com.pm.notificationservice.model.Notification notification) {
        com.pm.commoncontracts.dto.NotificationDto dto = entityToDto(notification);
        return new com.pm.commoncontracts.events.notification.NotificationToSendEventPayload(dto, notification.getId());
    }
}
