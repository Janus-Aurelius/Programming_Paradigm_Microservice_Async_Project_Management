package com.pm.notificationservice.integration;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.pm.commoncontracts.domain.NotificationChannel;
import com.pm.commoncontracts.domain.ParentType;
import com.pm.commoncontracts.dto.NotificationDto;
import com.pm.commoncontracts.events.notification.NotificationEvent;
import com.pm.notificationservice.model.Notification;
import com.pm.notificationservice.repository.NotificationRepository;
import com.pm.notificationservice.service.NotificationService;

import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Notification Integration Tests")
class NotificationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // Clean up the database before each test
        notificationRepository.deleteAll().block();
    }

    @Test
    @WithMockUser(username = "user123", authorities = {"ROLE_USER"})
    @DisplayName("Should create, read, and mark notification as read - full flow")
    void shouldCompleteFullNotificationFlow() {
        // Given - Create a notification through the service
        String recipientUserId = "user123";
        String message = "Test notification message";
        String eventType = "TASK_ASSIGNED";
        String entityType = "TASK";
        String entityId = "task123";
          Notification notification = Notification.builder()
                .recipientUserId(recipientUserId)
                .message(message)
                .event(NotificationEvent.TASK_ASSIGNED)
                .entityType(ParentType.valueOf(entityType))
                .entityId(entityId)
                .channel(NotificationChannel.WEBSOCKET)
                .read(false)
                .createdAt(Instant.now())
                .version(1L)
                .build();

        // When - Save notification to database
        Notification savedNotification = notificationRepository.save(notification).block();
        assertThat(savedNotification).isNotNull();
        assertThat(savedNotification.getId()).isNotNull();

        // Then - Verify we can retrieve the notification via API
        webTestClient.get()
                .uri("/notifications")
                .header("X-User-Id", recipientUserId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NotificationDto.class)
                .hasSize(1)
                .value(notifications -> {
                    NotificationDto notif = notifications.get(0);
                    assertThat(notif.getRecipientUserId()).isEqualTo(recipientUserId);
                    assertThat(notif.getMessage()).isEqualTo(message);
                    assertThat(notif.isRead()).isFalse();
                });

        // And - Mark notification as read via API
        webTestClient.post()
                .uri("/notifications/mark-read/{notificationId}", savedNotification.getId())
                .header("X-User-Id", recipientUserId)
                .exchange()
                .expectStatus().isNoContent();

        // Finally - Verify notification is marked as read
        webTestClient.get()
                .uri("/notifications")
                .header("X-User-Id", recipientUserId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NotificationDto.class)
                .hasSize(1)
                .value(notifications -> {
                    NotificationDto notif = notifications.get(0);
                    assertThat(notif.isRead()).isTrue();
                    assertThat(notif.getReadAt()).isNotNull();
                });
    }

    @Test
    @WithMockUser(username = "user123", authorities = {"ROLE_USER"})
    @DisplayName("Should filter notifications by recipient user")
    void shouldFilterNotificationsByRecipientUser() {
        // Given - Create notifications for different users
        String user1 = "user123";
        String user2 = "user456";
        
        Notification notification1 = createTestNotification(user1, "Message for user1");
        Notification notification2 = createTestNotification(user2, "Message for user2");
        Notification notification3 = createTestNotification(user1, "Another message for user1");

        notificationRepository.saveAll(java.util.List.of(notification1, notification2, notification3))
                .collectList().block();

        // When & Then - User1 should only see their notifications
        webTestClient.get()
                .uri("/notifications")
                .header("X-User-Id", user1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NotificationDto.class)
                .hasSize(2)
                .value(notifications -> {
                    assertThat(notifications).allMatch(notif -> notif.getRecipientUserId().equals(user1));
                });
    }

    @Test
    @WithMockUser(username = "user456", authorities = {"ROLE_USER"})
    @DisplayName("Should return empty list when user has no notifications")
    void shouldReturnEmptyListWhenUserHasNoNotifications() {
        // Given - Create notifications for different user
        String otherUser = "user123";
        String currentUser = "user456";
        
        Notification notification = createTestNotification(otherUser, "Message for other user");
        notificationRepository.save(notification).block();

        // When & Then - Current user should see no notifications
        webTestClient.get()
                .uri("/notifications")
                .header("X-User-Id", currentUser)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NotificationDto.class)
                .hasSize(0);
    }

    @Test
    @WithMockUser(username = "user123", authorities = {"ROLE_USER"})
    @DisplayName("Should handle marking non-existent notification as read")
    void shouldHandleMarkingNonExistentNotificationAsRead() {
        // Given
        String userId = "user123";
        String nonExistentNotificationId = "non-existent-id";

        // When & Then - Should handle gracefully (depending on service implementation)
        webTestClient.post()
                .uri("/notifications/mark-read/{notificationId}", nonExistentNotificationId)
                .header("X-User-Id", userId)
                .exchange()
                .expectStatus().isNoContent(); // Assuming service handles this gracefully
    }

    @Test
    @DisplayName("Should test notification service reactive streams")
    void shouldTestNotificationServiceReactiveStreams() {
        // Given
        String userId = "user123";
        Notification notification1 = createTestNotification(userId, "Message 1");
        Notification notification2 = createTestNotification(userId, "Message 2");
        
        // When - Save notifications
        notificationRepository.saveAll(java.util.List.of(notification1, notification2))
                .collectList().block();

        // Then - Test service reactive streams
        StepVerifier.create(notificationService.getNotificationsForUser(userId))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should test mark notification as read service behavior")
    void shouldTestMarkNotificationAsReadServiceBehavior() {
        // Given
        String userId = "user123";
        Notification notification = createTestNotification(userId, "Test message");
        Notification savedNotification = notificationRepository.save(notification).block();
        
        assertThat(savedNotification).isNotNull();
        assertThat(savedNotification.isRead()).isFalse();

        // When - Mark as read through service
        StepVerifier.create(notificationService.markNotificationRead(savedNotification.getId(), userId))
                .verifyComplete();

        // Then - Verify it's marked as read
        Notification updatedNotification = notificationRepository.findById(savedNotification.getId()).block();
        assertThat(updatedNotification).isNotNull();
        assertThat(updatedNotification.isRead()).isTrue();
        assertThat(updatedNotification.getReadAt()).isNotNull();
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    @DisplayName("Should allow admin to access any user's notifications")
    void shouldAllowAdminToAccessAnyUserNotifications() {
        // Given
        String userId = "user123";
        Notification notification = createTestNotification(userId, "Message for user");
        notificationRepository.save(notification).block();

        // When & Then - Admin should be able to access any user's notifications
        webTestClient.get()
                .uri("/notifications")
                .header("X-User-Id", userId) // Admin accessing another user's notifications
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NotificationDto.class)
                .hasSize(1);
    }

    @Test
    @DisplayName("Should test repository operations")
    void shouldTestRepositoryOperations() {
        // Given
        String userId = "user123";
        Notification notification = createTestNotification(userId, "Test message");

        // When - Test save
        StepVerifier.create(notificationRepository.save(notification))
                .assertNext(saved -> {
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getRecipientUserId()).isEqualTo(userId);
                    assertThat(saved.isRead()).isFalse();
                })
                .verifyComplete();        // Then - Test find by recipient
        StepVerifier.create(notificationRepository.findByrecipientUserId(userId))
                .expectNextCount(1)
                .verifyComplete();

        // And - Test find by recipient and read status
        StepVerifier.create(notificationRepository.findByRecipientUserIdAndReadIsFalse(userId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should test notification ordering by creation date")
    void shouldTestNotificationOrderingByCreationDate() {
        // Given
        String userId = "user123";
        Instant now = Instant.now();
        
        Notification oldNotification = createTestNotification(userId, "Old message");
        oldNotification.setCreatedAt(now.minus(Duration.ofHours(2)));
        
        Notification newNotification = createTestNotification(userId, "New message");
        newNotification.setCreatedAt(now);

        // When - Save in random order
        notificationRepository.saveAll(java.util.List.of(newNotification, oldNotification))
                .collectList().block();

        // Then - Should be returned in desc order (newest first)
        StepVerifier.create(notificationService.getNotificationsForUser(userId))
                .assertNext(notif -> assertThat(notif.getMessage()).isEqualTo("New message"))
                .assertNext(notif -> assertThat(notif.getMessage()).isEqualTo("Old message"))
                .verifyComplete();
    }    private Notification createTestNotification(String recipientUserId, String message) {
        return Notification.builder()
                .recipientUserId(recipientUserId)
                .message(message)
                .event(NotificationEvent.TASK_ASSIGNED)
                .entityType(ParentType.TASK)
                .entityId("task123")
                .channel(NotificationChannel.WEBSOCKET)
                .read(false)
                .createdAt(Instant.now())
                .version(1L)
                .build();
    }
}
