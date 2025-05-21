package com.pm.notificationservice.service;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.pm.commoncontracts.domain.TaskStatus;
import com.pm.commoncontracts.dto.CommentDto;
import com.pm.commoncontracts.dto.NotificationDto;
import com.pm.commoncontracts.dto.TaskDto;
import com.pm.commoncontracts.dto.UserDto;
import com.pm.commoncontracts.envelope.EventEnvelope;
import com.pm.commoncontracts.events.comment.CommentAddedEventPayload;
import com.pm.commoncontracts.domain.NotificationChannel;
import com.pm.commoncontracts.events.notification.NotificationEvent;
import com.pm.commoncontracts.events.notification.NotificationToSendEventPayload;
import com.pm.commoncontracts.events.project.ProjectCreatedEventPayload;
import com.pm.commoncontracts.events.project.ProjectTaskCreatedEventPayload;
import com.pm.commoncontracts.events.task.TaskAssignedEventPayload;
import com.pm.commoncontracts.events.task.TaskCreatedEventPayload;
import com.pm.commoncontracts.events.task.TaskPriorityChangedEventPayload;
import com.pm.commoncontracts.events.task.TaskStatusChangedEventPayload;
import com.pm.commoncontracts.events.task.TaskUpdatedEventPayload;
import com.pm.notificationservice.config.MdcLoggingFilter;
import com.pm.notificationservice.model.Notification;
import com.pm.notificationservice.repository.NotificationRepository;
import com.pm.notificationservice.utils.MentionUtils;
import com.pm.notificationservice.utils.NotificationUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final NotificationRepository notificationRepository;
    private final ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final WebClient taskWebClient;
    private final WebClient projectWebClient;

    @Value("${spring.application.name}") 
    private String serviceName;
    
    @Value("${kafka.topic.notification-dispatch}") 
    private String dispatchTopic;

    public NotificationService(
            NotificationRepository notificationRepository,
            ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> kafkaTemplate,
            @Qualifier("taskWebClient") WebClient taskWebClient,
            @Qualifier("projectWebClient") WebClient projectWebClient) {
        this.notificationRepository = notificationRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.taskWebClient = taskWebClient;
        this.projectWebClient = projectWebClient;
    }

    // Central method to process incoming business events
    public Mono<Void> processIncomingEvent(EventEnvelope<?> incomingEnvelope) {
        return Mono.deferContextual(contextView -> {
            String correlationId = contextView.getOrDefault(MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY, "N/A-event");
            logger.debug("Processing incoming event. Type: {}, CorrID: {}", incomingEnvelope.eventType(), correlationId);
            return generateNotificationsFromEvent(incomingEnvelope, correlationId)
                    .flatMap(notification -> saveAndTriggerDispatch(notification, correlationId)
                        .doOnError(e -> logger.error("Error saving/dispatching notification. CorrID: {}", correlationId, e))
                        .onErrorResume(e -> {
                            // TODO: Optionally send to dead-letter topic here
                            logger.error("Unrecoverable error saving/dispatching notification. CorrID: {}", correlationId, e);
                            return Mono.empty();
                        })
                    )
                    .onErrorContinue((throwable, o) -> logger.error("Unhandled error in notification processing, skipping element", throwable))
                    .then();
        });
    }

    private Flux<Notification> generateNotificationsFromEvent(EventEnvelope<?> envelope, String correlationId) {
        Object payload = envelope.payload();

        try {
            if (payload instanceof TaskAssignedEventPayload taskAssignedEventPayload) {
                return handleTaskAssignedEvent(taskAssignedEventPayload, envelope.eventType());
            }
            if (payload instanceof TaskStatusChangedEventPayload taskStatusChangedEventPayload) {
                return handleTaskStatusChangedEvent(taskStatusChangedEventPayload, envelope.eventType());
            }
            if (payload instanceof CommentAddedEventPayload commentAddedEventPayload) {
                return handleCommentAddedEvent(commentAddedEventPayload, envelope.eventType(), correlationId);
            }
            if (payload instanceof ProjectCreatedEventPayload projectCreatedEventPayload) {
                return handleProjectCreatedEvent(projectCreatedEventPayload, envelope.eventType());
            }
            if (payload instanceof ProjectTaskCreatedEventPayload projectTaskCreatedEventPayload) {
                return handleProjectTaskCreatedEvent(projectTaskCreatedEventPayload, envelope.eventType());
            }
            if (payload instanceof TaskCreatedEventPayload taskCreatedEventPayload) {
                return handleTaskCreatedEvent(taskCreatedEventPayload, envelope.eventType());
            }
            if (payload instanceof TaskUpdatedEventPayload taskUpdatedEventPayload) {
                return handleTaskUpdatedEvent(taskUpdatedEventPayload, envelope.eventType());
            }
            if (payload instanceof TaskPriorityChangedEventPayload taskPriorityChangedEventPayload) {
                return handleTaskPriorityChangedEvent(taskPriorityChangedEventPayload, envelope.eventType());
            }
            // ProjectUpdatedEventPayload, ProjectStatusChangedEventPayload: no notification per your rule
            // ProjectDeletedEventPayload, UserCreatedEventPayload, UserUpdatedEventPayload: ignored

            logger.warn("Unhandled event type for notification generation: {}. CorrID: {}", 
                envelope.eventType(), correlationId);
            return Flux.empty();
        } catch (Exception e) {
            logger.error("Error generating notifications for event type: {}. CorrID: {}", 
                envelope.eventType(), correlationId, e);
            return Flux.empty();
        }
    }

    // --- Event Handlers ---
    private Flux<Notification> handleTaskAssignedEvent(TaskAssignedEventPayload taskAssigned, String eventType) {
        TaskDto taskDto = taskAssigned.taskDto();
        String assigneeId = taskDto.getAssigneeId();
        if (assigneeId == null) {
            logger.warn("Task {} was assigned but has no assignee ID", taskDto.getId());
            return Flux.empty();
        }
        String taskName = taskDto.getName();
        String message = String.format("Task '%s' was assigned to you", taskName);
        return Flux.just(Notification.builder()
            .recipientUserId(assigneeId)
            .event(NotificationEvent.TASK_ASSIGNED)
            .entityType(com.pm.commoncontracts.domain.ParentType.TASK)
            .entityId(taskDto.getId())
            .channel(NotificationChannel.WEBSOCKET)
            .message(message)
            .read(false)
            .createdAt(java.time.Instant.now())
            .build());
    }

    private Flux<Notification> handleTaskStatusChangedEvent(TaskStatusChangedEventPayload taskStatusChanged, String eventType) {
        TaskDto updatedTask = taskStatusChanged.taskDto();
        
        if (updatedTask.getStatus() == TaskStatus.BLOCKED) {
            String assigneeId = updatedTask.getAssigneeId();
            if (assigneeId == null) {
                logger.warn("Task {} status changed to BLOCKED but has no assignee ID", updatedTask.getId());
                return Flux.empty();
            }

            String taskName = updatedTask.getName();
            String message = String.format("Task '%s' you are assigned to was marked as BLOCKED", taskName);
            
            return Flux.just(Notification.builder()
                .recipientUserId(assigneeId)
                .event(NotificationEvent.TASK_STATUS_CHANGED)
                .entityType(com.pm.commoncontracts.domain.ParentType.TASK)
                .entityId(updatedTask.getId())
                .channel(NotificationChannel.WEBSOCKET)
                .message(message)
                .read(false)
                .createdAt(java.time.Instant.now())
                .build());
        }
        
        return Flux.empty();
    }

    private Flux<Notification> handleCommentAddedEvent(CommentAddedEventPayload commentAdded, 
            String eventType, String correlationId) {
        CommentDto commentDto = commentAdded.commentDto();
        String parentId = commentDto.getParentId();
        String parentType = commentDto.getParentType().name();
        String authorUsername = commentDto.getUsername();
        String commentId = commentDto.getId();
        String commentText = commentDto.getContent();
        Set<String> mentionedUsernames = MentionUtils.extractMentions(commentText);

        logger.info("CommentAddedEvent received for {} {}. Author: {}. CorrID: {}", 
            parentType, parentId, authorUsername, correlationId);

        if ("TASK".equals(parentType)) {
            return getTaskCommentNotifications(parentId, authorUsername, commentId, eventType, mentionedUsernames);
        } else if ("PROJECT".equals(parentType)) {
            return getProjectCommentNotifications(parentId, authorUsername, commentId, eventType, mentionedUsernames);
        }

        return Flux.empty();
    }

    private Flux<Notification> handleProjectCreatedEvent(ProjectCreatedEventPayload payload, String eventType) {
        var projectDto = payload.projectDto();
        if (projectDto.getOwnerId() != null) {
            String message = String.format("You have been assigned as owner of the new project '%s'", projectDto.getName());
            return Flux.just(Notification.builder()
                .recipientUserId(projectDto.getOwnerId())
                .event(NotificationEvent.PROJECT_CREATED)
                .entityType(com.pm.commoncontracts.domain.ParentType.PROJECT)
                .entityId(projectDto.getId())
                .channel(NotificationChannel.WEBSOCKET)
                .message(message)
                .read(false)
                .createdAt(java.time.Instant.now())
                .build());
        }
        return Flux.empty();
    }

    private Flux<Notification> handleProjectTaskCreatedEvent(ProjectTaskCreatedEventPayload payload, String eventType) {
        var taskDto = payload.taskDto();
        String assigneeId = taskDto.getAssigneeId();
        if (assigneeId != null) {
            String message = String.format("A new task '%s' has been assigned to you in project", taskDto.getName());
            return Flux.just(Notification.builder()
                .recipientUserId(assigneeId)
                .event(NotificationEvent.PROJECT_TASK_CREATED)
                .entityType(com.pm.commoncontracts.domain.ParentType.TASK)
                .entityId(taskDto.getId())
                .channel(NotificationChannel.WEBSOCKET)
                .message(message)
                .read(false)
                .createdAt(java.time.Instant.now())
                .build());
        }
        return Flux.empty();
    }

    private Flux<Notification> handleTaskCreatedEvent(TaskCreatedEventPayload payload, String eventType) {
        var taskDto = payload.taskDto();
        return notifyAssigneeOnly(taskDto, eventType, String.format("A new task '%s' has been assigned to you", taskDto.getName()), com.pm.commoncontracts.domain.ParentType.TASK.name());
    }

    private Flux<Notification> handleTaskUpdatedEvent(TaskUpdatedEventPayload payload, String eventType) {
        var taskDto = payload.taskDto();
        return notifyAssigneeOnly(taskDto, eventType, String.format("Task '%s' was updated", taskDto.getName()), com.pm.commoncontracts.domain.ParentType.TASK.name());
    }

    private Flux<Notification> handleTaskPriorityChangedEvent(TaskPriorityChangedEventPayload payload, String eventType) {
        var taskDto = payload.dto();
        return notifyAssigneeOnly(taskDto, eventType, String.format("Priority of task '%s' was changed", taskDto.getName()), com.pm.commoncontracts.domain.ParentType.TASK.name());
    }

    // Helper: Notify only the assignee
    private Flux<Notification> notifyAssigneeOnly(TaskDto taskDto, String eventType, String message, String type) {
        String assigneeId = taskDto.getAssigneeId();
        if (assigneeId != null) {
            return Flux.just(Notification.builder()
                .recipientUserId(assigneeId)
                .event(NotificationEvent.valueOf(eventType))
                .entityType(com.pm.commoncontracts.domain.ParentType.valueOf(type))
                .entityId(taskDto.getId())
                .channel(NotificationChannel.WEBSOCKET)
                .message(message)
                .read(false)
                .createdAt(java.time.Instant.now())
                .build());
        }
        return Flux.empty();
    }

    private Flux<Notification> getTaskCommentNotifications(String taskId, String authorUsername, String commentId,
            String eventType, Set<String> mentionedUsernames) {
        return taskWebClient.get()
            .uri("/tasks/{id}/assignee", taskId)
            .retrieve()
            .bodyToMono(UserDto.class)
            .flatMapMany(assignee -> {
                String assigneeId = assignee.getId();
                if (assigneeId.equals(authorUsername)) {
                    logger.debug("Comment author is the assignee, no notification needed for task {}", taskId);
                    return Flux.empty();
                }

                Notification assigneeNotification = Notification.builder()
                    .recipientUserId(assigneeId)
                    .event(NotificationEvent.valueOf(eventType))
                    .entityType(com.pm.commoncontracts.domain.ParentType.TASK)
                    .entityId(taskId)
                    .channel(NotificationChannel.WEBSOCKET)
                    .message("A new comment was added to task you are assigned to")
                    .read(false)
                    .createdAt(java.time.Instant.now())
                    .build();

                Flux<Notification> mentionNotifications = Flux.fromIterable(mentionedUsernames)
                    .map(username -> Notification.builder()
                        .recipientUserId(username)
                        .event(NotificationEvent.valueOf(eventType))
                        .entityType(com.pm.commoncontracts.domain.ParentType.TASK)
                        .entityId(taskId)
                        .channel(NotificationChannel.WEBSOCKET)
                        .message("You were mentioned in a comment on task")
                        .read(false)
                        .createdAt(java.time.Instant.now())
                        .build());

                return Flux.concat(Flux.just(assigneeNotification), mentionNotifications);
            });
    }

    private Flux<Notification> getProjectCommentNotifications(String projectId, String authorUsername, String commentId,
            String eventType, Set<String> mentionedUsernames) {
        return projectWebClient.get()
            .uri("/projects/{id}/owner", projectId)
            .retrieve()
            .bodyToMono(UserDto.class)
            .flatMapMany(owner -> {
                String ownerId = owner.getId();
                if (ownerId.equals(authorUsername)) {
                    logger.debug("Comment author is the owner, no notification needed for project {}", projectId);
                    return Flux.empty();
                }

                Notification ownerNotification = Notification.builder()
                    .recipientUserId(ownerId)
                    .event(NotificationEvent.valueOf(eventType))
                    .entityType(com.pm.commoncontracts.domain.ParentType.PROJECT)
                    .entityId(projectId)
                    .channel(NotificationChannel.WEBSOCKET)
                    .message("A new comment was added to project you own")
                    .read(false)
                    .createdAt(java.time.Instant.now())
                    .build();

                Flux<Notification> mentionNotifications = Flux.fromIterable(mentionedUsernames)
                    .map(username -> Notification.builder()
                        .recipientUserId(username)
                        .event(NotificationEvent.valueOf(eventType))
                        .entityType(com.pm.commoncontracts.domain.ParentType.PROJECT)
                        .entityId(projectId)
                        .channel(NotificationChannel.WEBSOCKET)
                        .message("You were mentioned in a comment on project")
                        .read(false)
                        .createdAt(java.time.Instant.now())
                        .build());

                return Flux.concat(Flux.just(ownerNotification), mentionNotifications);
            });
    }

    private Mono<Void> saveAndTriggerDispatch(Notification notification, String correlationId) {
        return notificationRepository.save(notification)
            .doOnSuccess(savedNotification -> logger.info("Notification saved. CorrID: {}", correlationId))
            .flatMap(savedNotification -> {
                NotificationToSendEventPayload payload = NotificationUtils.toNotificationToSendEventPayload(savedNotification);
                EventEnvelope<NotificationToSendEventPayload> envelope = new EventEnvelope<>(
                    serviceName,
                    dispatchTopic,
                    correlationId,
                    payload
                );
                return kafkaTemplate.send(dispatchTopic, envelope).then();
            });
    }

    public Flux<NotificationDto> getNotificationsForUser(String recipientUserId) {
        return notificationRepository.findByrecipientUserId(recipientUserId)
                .map(NotificationUtils::entityToDto)
                .doOnError(e -> logger.error("Error fetching notifications for user: {}", recipientUserId, e))
                .onErrorResume(e -> Flux.error(new RuntimeException("Failed to fetch notifications for user", e)))
                .onErrorContinue((throwable, o) -> logger.error("Unhandled error in getNotificationsForUser, skipping element", throwable));
    }

    public Mono<Void> markNotificationRead(String notificationId, String userId) {
        return notificationRepository.findById(notificationId)
                .flatMap(notification -> {
                    if (!notification.getRecipientUserId().equals(userId)) {
                        return Mono.error(new RuntimeException("User not authorized to mark this notification as read"));
                    }
                    notification.setRead(true);
                    notification.setReadAt(java.time.Instant.now());
                    return notificationRepository.save(notification).then();
                });
    }
}
