// package com.pm.notificationservice.repository;

// import com.pm.commoncontracts.domain.NotificationChannel;
// import com.pm.commoncontracts.domain.ParentType;
// import com.pm.commoncontracts.events.notification.NotificationEvent;
// import com.pm.notificationservice.model.Notification;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
// import org.springframework.test.context.ActiveProfiles;
// import reactor.core.publisher.Flux;
// import reactor.test.StepVerifier;

// import java.time.LocalDateTime;
// import java.util.HashMap;

// @DataMongoTest
// @ActiveProfiles("test")
// class NotificationRepositoryTest {

//     @Autowired
//     private NotificationRepository notificationRepository;

//     private Notification notification1;
//     private Notification notification2;
//     private Notification notification3;

//     @BeforeEach
//     void setUp() {
//         // Clean up before each test
//         notificationRepository.deleteAll().block();

//         // Create test notifications
//         notification1 = Notification.builder()
//                 .recipientUserId("user1")
//                 .event(NotificationEvent.TASK_ASSIGNED)
//                 .entityType(ParentType.TASK)
//                 .entityId("task1")
//                 .channel(NotificationChannel.WEBSOCKET)
//                 .payload(new HashMap<>())
//                 .message("You have been assigned to task 'Test Task'")
//                 .timestamp(LocalDateTime.now().minusHours(1))
//                 .read(false)
//                 .build();

//         notification2 = Notification.builder()
//                 .recipientUserId("user1")
//                 .event(NotificationEvent.COMMENT_ADDED)
//                 .entityType(ParentType.TASK)
//                 .entityId("task2")
//                 .channel(NotificationChannel.EMAIL)
//                 .payload(new HashMap<>())
//                 .message("New comment added to task")
//                 .timestamp(LocalDateTime.now().minusHours(2))
//                 .read(true)
//                 .build();

//         notification3 = Notification.builder()
//                 .recipientUserId("user2")
//                 .event(NotificationEvent.TASK_STATUS_CHANGED)
//                 .entityType(ParentType.TASK)
//                 .entityId("task3")
//                 .channel(NotificationChannel.WEBSOCKET)
//                 .payload(new HashMap<>())
//                 .message("Task status changed")
//                 .timestamp(LocalDateTime.now().minusHours(3))
//                 .read(false)
//                 .build();

//         // Save test data
//         notificationRepository.saveAll(Flux.just(notification1, notification2, notification3)).blockLast();
//     }

//     @Test
//     void findByRecipientUserIdOrderByTimestampDesc_ShouldReturnNotificationsInDescendingOrder() {
//         StepVerifier.create(notificationRepository.findByRecipientUserIdOrderByTimestampDesc("user1"))
//                 .expectNextMatches(notification -> notification.getMessage().equals("You have been assigned to task 'Test Task'"))
//                 .expectNextMatches(notification -> notification.getMessage().equals("New comment added to task"))
//                 .verifyComplete();
//     }

//     @Test
//     void findByRecipientUserIdAndReadIsFalse_ShouldReturnOnlyUnreadNotifications() {
//         StepVerifier.create(notificationRepository.findByRecipientUserIdAndReadIsFalse("user1"))
//                 .expectNextMatches(notification -> 
//                     notification.getMessage().equals("You have been assigned to task 'Test Task'") && 
//                     !notification.isRead())
//                 .verifyComplete();
//     }

//     @Test
//     void findByrecipientUserId_ShouldReturnAllNotificationsForUser() {
//         StepVerifier.create(notificationRepository.findByrecipientUserId("user1"))
//                 .expectNextCount(2)
//                 .verifyComplete();
//     }

//     @Test
//     void findByrecipientUserId_ShouldReturnEmptyFluxForNonExistentUser() {
//         StepVerifier.create(notificationRepository.findByrecipientUserId("nonexistent"))
//                 .verifyComplete();
//     }

//     @Test
//     void save_ShouldPersistNotification() {
//         Notification newNotification = Notification.builder()
//                 .recipientUserId("user3")
//                 .event(NotificationEvent.PROJECT_MEMBER_ADDED)
//                 .entityType(ParentType.PROJECT)
//                 .entityId("project1")
//                 .channel(NotificationChannel.EMAIL)
//                 .payload(new HashMap<>())
//                 .message("You have been added to project")
//                 .timestamp(LocalDateTime.now())
//                 .read(false)
//                 .build();

//         StepVerifier.create(notificationRepository.save(newNotification))
//                 .expectNextMatches(saved -> saved.getId() != null)
//                 .verifyComplete();

//         StepVerifier.create(notificationRepository.findByrecipientUserId("user3"))
//                 .expectNextCount(1)
//                 .verifyComplete();
//     }
// }
