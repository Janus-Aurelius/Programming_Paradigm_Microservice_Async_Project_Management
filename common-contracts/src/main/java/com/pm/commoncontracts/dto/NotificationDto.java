package com.pm.commoncontracts.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class NotificationDto{
        String id;
        String recipientUserId;
        String eventType;
        String message;
        String entityType;
        String entityId;
        String channel;
        Instant createdAt;
        boolean isRead;
        private LocalDateTime timestamp;
        String event;
        Map<String, Object> payload;
        boolean read;
        Instant readAt;
        Long version;
}
