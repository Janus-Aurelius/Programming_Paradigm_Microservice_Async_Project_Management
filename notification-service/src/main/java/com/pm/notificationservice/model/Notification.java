package com.pm.notificationservice.model;

import com.pm.commoncontracts.domain.NotificationChannel;
import com.pm.commoncontracts.domain.ParentType; // Assuming ParentType is suitable here
// Or define a specific NotificationEntityType if ParentType is too broad/not fitting
import com.pm.commoncontracts.events.notification.*; // Assuming this is the location

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "notifications")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    public static Object NotificationChannel;
    @Id
    private String id;

    /* ---------- routing & context ---------- */
    @NotBlank
    @Indexed
    private String recipientUserId;

    @NotNull
    private NotificationEvent event; // enum, e.g. TASK_ASSIGNED, COMMENT_ADDED

    @NotNull
    private ParentType entityType; // PROJECT, TASK, COMMENT … (ensure ParentType enum covers all cases)
    // Or create a more specific NotificationEntityType enum

    @NotBlank
    private String entityId; // ID of the entity that triggered the event

    /* ---------- delivery ---------- */
    @NotNull
    private NotificationChannel channel; // WEBSOCKET, EMAIL, PUSH …

    @Builder.Default
    private Map<String, Object> payload = new HashMap<>(); // channel-specific extras (optional)

    /* ---------- content ---------- */
    @NotBlank
    @Size(max = 1024)
    private String message; // short, user-visible text

    /* ---------- state ---------- */
    private boolean read = false; // false = unread

    private Instant readAt; // nullable, set when user opens it

    /* ---------- auditing ---------- */
    @CreatedDate
    private Instant createdAt;

    // @LastModifiedDate // Notifications are typically immutable once created,
    // except for 'read' status. If 'read' status changes
    // trigger @LastModifiedDate, it's fine.
    // private Instant updatedAt;

    @Version
    private Long version; // optimistic locking
}

// Enums like NotificationChannel and NotificationEvent should ideally be in common-contracts
// if they are shared between services (e.g., a service producing an event that leads to a notification,
// and the notification service consuming it).
// The image shows events/project/NotificationToSendEventPayload, so NotificationEvent is likely there.
//
// Example (ensure these are in your common-contracts if not already):
// package com.pm.commoncontracts.events;
// public enum NotificationChannel { WEBSOCKET, EMAIL, PUSH }

// package com.pm.commoncontracts.events;
// public enum NotificationEvent {
//     TASK_ASSIGNED,
//     TASK_STATUS_CHANGED,
//     TASK_PRIORITY_CHANGED,
//     TASK_DUE_DATE_CHANGED,
//     TASK_COMPLETED,
//     COMMENT_ADDED,
//     PROJECT_MEMBER_ADDED,
//     PROJECT_STATUS_CHANGED,
//     PROJECT_COMPLETED
//     // etc.
// }