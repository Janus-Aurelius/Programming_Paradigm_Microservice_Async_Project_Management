package com.pm.commoncontracts.dto;

import java.util.Map;

import com.pm.commoncontracts.domain.NotificationChannel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class NotificationDto {

    String id;
    String recipientUserId;
    String eventType;
    String message;
    String entityType;
    String entityId;
    NotificationChannel channel;
    String createdAt;
    boolean isRead;
    String timestamp;
    String event;
    Map<String, Object> payload;
    boolean read;
    String readAt;
    Long version;
}
