// package com.pm.notificationservice.service;

// import com.pm.commoncontracts.domain.NotificationChannel;
// import com.pm.commoncontracts.domain.ParentType;
// import com.pm.commoncontracts.dto.NotificationDto;
// import com.pm.commoncontracts.events.notification.NotificationEvent;
// import com.pm.notificationservice.model.Notification;
// import com.pm.notificationservice.repository.NotificationRepository;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import reactor.core.publisher.Flux;
// import reactor.core.publisher.Mono;
// import reactor.test.StepVerifier;

// import java.time.Instant;
// import java.time.LocalDateTime;
// import java.util.HashMap;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.when;

// @ExtendWith(MockitoExtension.class)
// class NotificationServiceTest {

//     @Mock
//     private NotificationRepository notificationRepository;

//     @InjectMocks
//     private NotificationService notificationService;

//     private Notification testNotification;

//     @BeforeEach
//     void setUp() {
//         testNotification = Notification.builder()
//                 .id("notification1")
//                 .recipientUserId("user1")
//                 .event(NotificationEvent.TASK_ASSIGNED)
//                 .entityType(ParentType.TASK)
//                 .entityId("task1")
//                 .channel(NotificationChannel.WEBSOCKET)
//                 .payload(new HashMap<>())
//                 .message("You have been assigned to task 'Test Task'")
//                 .timestamp(LocalDateTime.now())
//                 .read(false)
//                 .createdAt(Instant.now())
//                 .version(0L)
//                 .build();
//     }

//     @Test
//     void getNotificationsForUser_ShouldReturnNotificationDtos() {
//         when(notificationRepository.findByRecipientUserIdOrderByTimestampDesc("user1"))
//                 .thenReturn(Flux.just(testNotification));

//         StepVerifier.create(notificationService.getNotificationsForUser("user1"))
//                 .expectNextMatches(dto -> 
//                     dto.getId().equals("notification1") &&
//                     dto.getRecipientUserId().equals("user1") &&
//                     dto.getMessage().equals("You have been assigned to task 'Test Task'") &&
//                     !dto.isRead())
//                 .verifyComplete();
//     }

//     @Test
//     void getNotificationsForUser_ShouldReturnEmptyWhenNoNotifications() {
//         when(notificationRepository.findByRecipientUserIdOrderByTimestampDesc("user2"))
//                 .thenReturn(Flux.empty());

//         StepVerifier.create(notificationService.getNotificationsForUser("user2"))
//                 .verifyComplete();
//     }

//     @Test
//     void markNotificationRead_ShouldUpdateNotificationAndReturnVoid() {
//         Notification updatedNotification = Notification.builder()
//                 .id("notification1")
//                 .recipientUserId("user1")
//                 .event(NotificationEvent.TASK_ASSIGNED)
//                 .entityType(ParentType.TASK)
//                 .entityId("task1")
//                 .channel(NotificationChannel.WEBSOCKET)
//                 .payload(new HashMap<>())
//                 .message("You have been assigned to task 'Test Task'")
//                 .timestamp(LocalDateTime.now())
//                 .read(true)
//                 .readAt(Instant.now())
//                 .createdAt(Instant.now())
//                 .version(1L)
//                 .build();

//         when(notificationRepository.findById("notification1"))
//                 .thenReturn(Mono.just(testNotification));
//         when(notificationRepository.save(any(Notification.class)))
//                 .thenReturn(Mono.just(updatedNotification));

//         StepVerifier.create(notificationService.markNotificationRead("notification1", "user1"))
//                 .verifyComplete();
//     }

//     @Test
//     void markNotificationRead_ShouldReturnErrorWhenNotificationNotFound() {
//         when(notificationRepository.findById("nonexistent"))
//                 .thenReturn(Mono.empty());

//         StepVerifier.create(notificationService.markNotificationRead("nonexistent", "user1"))
//                 .expectError()
//                 .verify();
//     }

//     @Test
//     void markNotificationRead_ShouldReturnErrorWhenUserNotAuthorized() {
//         when(notificationRepository.findById("notification1"))
//                 .thenReturn(Mono.just(testNotification));

//         StepVerifier.create(notificationService.markNotificationRead("notification1", "user2"))
//                 .expectError()
//                 .verify();
//     }

//     @Test
//     void getUnreadNotifications_ShouldReturnOnlyUnreadNotifications() {
//         when(notificationRepository.findByRecipientUserIdAndReadIsFalse("user1"))
//                 .thenReturn(Flux.just(testNotification));

//         StepVerifier.create(notificationService.getUnreadNotifications("user1"))
//                 .expectNextMatches(dto -> 
//                     dto.getId().equals("notification1") &&
//                     !dto.isRead())
//                 .verifyComplete();
//     }
// }
