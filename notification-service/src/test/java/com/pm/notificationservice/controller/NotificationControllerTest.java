package com.pm.notificationservice.controller;

import com.pm.commoncontracts.dto.NotificationDto;
import com.pm.notificationservice.security.NotificationPermissionEvaluator;
import com.pm.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@WebFluxTest(NotificationController.class)
@ContextConfiguration(classes = {NotificationController.class, NotificationControllerTest.TestConfig.class})
@ActiveProfiles("test")
@DisplayName("Notification Controller Tests")
class NotificationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationPermissionEvaluator notificationPermissionEvaluator;

    @Configuration
    static class TestConfig {
        @Bean
        public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
                NotificationPermissionEvaluator notificationPermissionEvaluator) {
            DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
            expressionHandler.setPermissionEvaluator(notificationPermissionEvaluator);
            return expressionHandler;
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @WithMockUser(username = "user123", authorities = {"ROLE_USER"})
    @DisplayName("Should get user notifications successfully when user has permission")
    void shouldGetUserNotificationsSuccessfully() {
        // Given
        String userId = "user123";
        NotificationDto notification1 = createNotificationDto("notif1", userId, "Task assigned", false);
        NotificationDto notification2 = createNotificationDto("notif2", userId, "Project updated", true);
        
        when(notificationPermissionEvaluator.hasPermission(any(), eq(userId), eq("NOTI_READ")))
                .thenReturn(true);
        when(notificationService.getNotificationsForUser(userId))
                .thenReturn(Flux.fromIterable(List.of(notification1, notification2)));

        // When & Then
        webTestClient.get()
                .uri("/notifications")
                .header("X-User-Id", userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(NotificationDto.class)
                .hasSize(2)
                .contains(notification1, notification2);

        verify(notificationService).getNotificationsForUser(userId);
    }

    @Test
    @WithMockUser(username = "user123", authorities = {"ROLE_USER"})
    @DisplayName("Should return empty list when user has no notifications")
    void shouldReturnEmptyListWhenNoNotifications() {
        // Given
        String userId = "user123";
        
        when(notificationPermissionEvaluator.hasPermission(any(), eq(userId), eq("NOTI_READ")))
                .thenReturn(true);
        when(notificationService.getNotificationsForUser(userId))
                .thenReturn(Flux.empty());

        // When & Then
        webTestClient.get()
                .uri("/notifications")
                .header("X-User-Id", userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(NotificationDto.class)
                .hasSize(0);

        verify(notificationService).getNotificationsForUser(userId);
    }

    @Test
    @WithMockUser(username = "user123", authorities = {"ROLE_USER"})
    @DisplayName("Should return 403 when user doesn't have permission to read notifications")
    void shouldReturn403WhenUserLacksReadPermission() {
        // Given
        String userId = "user123";
        
        when(notificationPermissionEvaluator.hasPermission(any(), eq(userId), eq("NOTI_READ")))
                .thenReturn(false);

        // When & Then
        webTestClient.get()
                .uri("/notifications")
                .header("X-User-Id", userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(username = "user123", authorities = {"ROLE_USER"})
    @DisplayName("Should mark notification as read successfully when user has permission")
    void shouldMarkNotificationAsReadSuccessfully() {
        // Given
        String userId = "user123";
        String notificationId = "notif123";
        
        when(notificationPermissionEvaluator.hasPermission(any(), eq(notificationId), eq("NOTI_UPDATE")))
                .thenReturn(true);
        when(notificationService.markNotificationRead(notificationId, userId))
                .thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri("/notifications/mark-read/{notificationId}", notificationId)
                .header("X-User-Id", userId)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        verify(notificationService).markNotificationRead(notificationId, userId);
    }

    @Test
    @WithMockUser(username = "user123", authorities = {"ROLE_USER"})
    @DisplayName("Should return 403 when user doesn't have permission to update notification")
    void shouldReturn403WhenUserLacksUpdatePermission() {
        // Given
        String userId = "user123";
        String notificationId = "notif123";
        
        when(notificationPermissionEvaluator.hasPermission(any(), eq(notificationId), eq("NOTI_UPDATE")))
                .thenReturn(false);

        // When & Then
        webTestClient.post()
                .uri("/notifications/mark-read/{notificationId}", notificationId)
                .header("X-User-Id", userId)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(username = "user123", authorities = {"ROLE_ADMIN"})
    @DisplayName("Should allow admin to read any user's notifications")
    void shouldAllowAdminToReadAnyUserNotifications() {
        // Given
        String userId = "user456"; // Different user
        String adminUserId = "user123";
        NotificationDto notification = createNotificationDto("notif1", userId, "Task assigned", false);
        
        when(notificationPermissionEvaluator.hasPermission(any(), eq(userId), eq("NOTI_READ")))
                .thenReturn(true); // Admin should have permission
        when(notificationService.getNotificationsForUser(userId))
                .thenReturn(Flux.just(notification));

        // When & Then
        webTestClient.get()
                .uri("/notifications")
                .header("X-User-Id", userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NotificationDto.class)
                .hasSize(1)
                .contains(notification);

        verify(notificationService).getNotificationsForUser(userId);
    }

    @Test
    @WithMockUser(username = "user123", authorities = {"ROLE_ADMIN"})
    @DisplayName("Should allow admin to mark any notification as read")
    void shouldAllowAdminToMarkAnyNotificationAsRead() {
        // Given
        String userId = "user456"; // Different user
        String notificationId = "notif123";
        
        when(notificationPermissionEvaluator.hasPermission(any(), eq(notificationId), eq("NOTI_UPDATE")))
                .thenReturn(true); // Admin should have permission
        when(notificationService.markNotificationRead(notificationId, userId))
                .thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri("/notifications/mark-read/{notificationId}", notificationId)
                .header("X-User-Id", userId)
                .exchange()
                .expectStatus().isNoContent();

        verify(notificationService).markNotificationRead(notificationId, userId);
    }

    @Test
    @DisplayName("Should return 401 when user is not authenticated")
    void shouldReturn401WhenUserNotAuthenticated() {
        // When & Then - GET notifications without authentication
        webTestClient.get()
                .uri("/notifications")
                .header("X-User-Id", "user123")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();

        // When & Then - POST mark as read without authentication
        webTestClient.post()
                .uri("/notifications/mark-read/{notificationId}", "notif123")
                .header("X-User-Id", "user123")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(username = "user123", authorities = {"ROLE_USER"})
    @DisplayName("Should handle service error gracefully")
    void shouldHandleServiceErrorGracefully() {
        // Given
        String userId = "user123";
        
        when(notificationPermissionEvaluator.hasPermission(any(), eq(userId), eq("NOTI_READ")))
                .thenReturn(true);
        when(notificationService.getNotificationsForUser(userId))
                .thenReturn(Flux.error(new RuntimeException("Database connection error")));

        // When & Then
        webTestClient.get()
                .uri("/notifications")
                .header("X-User-Id", userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @WithMockUser(username = "user123", authorities = {"ROLE_USER"})
    @DisplayName("Should handle mark as read service error gracefully")
    void shouldHandleMarkAsReadServiceErrorGracefully() {
        // Given
        String userId = "user123";
        String notificationId = "notif123";
        
        when(notificationPermissionEvaluator.hasPermission(any(), eq(notificationId), eq("NOTI_UPDATE")))
                .thenReturn(true);
        when(notificationService.markNotificationRead(notificationId, userId))
                .thenReturn(Mono.error(new RuntimeException("Database connection error")));

        // When & Then
        webTestClient.post()
                .uri("/notifications/mark-read/{notificationId}", notificationId)
                .header("X-User-Id", userId)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @WithMockUser(username = "user123", authorities = {"ROLE_USER"})
    @DisplayName("Should extract user ID from header correctly")
    void shouldExtractUserIdFromHeaderCorrectly() {
        // Given
        String userId = "user456";
        NotificationDto notification = createNotificationDto("notif1", userId, "Test message", false);
        
        when(notificationPermissionEvaluator.hasPermission(any(), eq(userId), eq("NOTI_READ")))
                .thenReturn(true);
        when(notificationService.getNotificationsForUser(userId))
                .thenReturn(Flux.just(notification));

        // When & Then
        webTestClient.get()
                .uri("/notifications")
                .header("X-User-Id", userId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(NotificationDto.class)
                .hasSize(1);

        verify(notificationService).getNotificationsForUser(userId);
    }

    private NotificationDto createNotificationDto(String id, String recipientUserId, String message, boolean isRead) {
        return NotificationDto.builder()
                .id(id)
                .recipientUserId(recipientUserId)
                .message(message)
                .eventType("TASK_ASSIGNED")
                .entityType("TASK")
                .entityId("task123")
                .channel("WEBSOCKET")
                .isRead(isRead)
                .read(isRead)
                .createdAt(Instant.now())
                .version(1L)
                .build();
    }
}
