package com.pm.notificationservice.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.pm.commoncontracts.domain.NotificationChannel;
import com.pm.commoncontracts.domain.TaskPriority;
import com.pm.commoncontracts.domain.TaskStatus;
import com.pm.commoncontracts.dto.CommentDto;
import com.pm.commoncontracts.dto.NotificationDto;
import com.pm.commoncontracts.dto.TaskDto;
import com.pm.commoncontracts.envelope.EventEnvelope;
import com.pm.commoncontracts.events.comment.CommentAddedEventPayload;
import com.pm.commoncontracts.events.comment.CommentDeletedEventPayload;
import com.pm.commoncontracts.events.comment.CommentEditedEventPayload;
import com.pm.commoncontracts.events.notification.NotificationEvent;
import com.pm.commoncontracts.events.notification.NotificationReadEventPayload;
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
import reactor.util.context.ContextView;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final WebClient taskWebClient;
    private final WebClient projectWebClient;
    private final WebClient commentWebClient;

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${kafka.topic.notification-dispatch}")
    private String dispatchTopic;

    @Value("${kafka.topic.notification-events}")
    private String notificationEventsTopic;

    @Value("${kafka.topic.websocket-dispatch:websocket-dispatch}")
    private String websocketDispatchTopic;

    public NotificationService(
            NotificationRepository notificationRepository,
            @Qualifier("reactiveKafkaProducerTemplate") ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> kafkaTemplate,
            @Qualifier("taskWebClient") WebClient taskWebClient,
            @Qualifier("projectWebClient") WebClient projectWebClient,
            @Qualifier("commentWebClient") WebClient commentWebClient) {
        this.notificationRepository = notificationRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.taskWebClient = taskWebClient;
        this.projectWebClient = projectWebClient;
        this.commentWebClient = commentWebClient;
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

        // Debug logging to understand payload deserialization
        logger.debug("Payload type: {}, Event type: {}, CorrID: {}",
                payload != null ? payload.getClass().getName() : "null",
                envelope.eventType(), correlationId);

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
            if (payload instanceof CommentDeletedEventPayload commentDeletedEventPayload) {
                return handleCommentDeletedEvent(commentDeletedEventPayload, envelope.eventType(), correlationId);
            }
            if (payload instanceof CommentEditedEventPayload commentEditedEventPayload) {
                return handleCommentEditedEvent(commentEditedEventPayload, envelope.eventType(), correlationId);
            }
            if (payload instanceof ProjectCreatedEventPayload projectCreatedEventPayload) {
                return handleProjectCreatedEvent(projectCreatedEventPayload, envelope.eventType());
            }

            // Handle PROJECT_CREATED event type even if payload deserialization failed
            if ("PROJECT_CREATED".equals(envelope.eventType())) {
                logger.info("Handling PROJECT_CREATED event with fallback deserialization. CorrID: {}", correlationId);
                return handleProjectCreatedEventFallback(payload, envelope.eventType(), correlationId);
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
        String assigneeId = updatedTask.getAssigneeId();

        // Only notify if there's an assignee
        if (assigneeId == null) {
            logger.debug("Task {} status changed but has no assignee ID", updatedTask.getId());
            return Flux.empty();
        }

        String taskName = updatedTask.getName();
        String message = getTaskStatusChangeMessage(updatedTask.getStatus(), taskName);

        // Notify assignee for significant status changes
        if (isSignificantStatusChange(updatedTask.getStatus())) {
            logger.info("Creating status change notification for task '{}' (status: {}) for assignee: {}",
                    taskName, updatedTask.getStatus(), assigneeId);

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

        logger.debug("Status change for task '{}' to {} is not significant enough for notification",
                taskName, updatedTask.getStatus());
        return Flux.empty();
    }

    /**
     * Determines if a status change is significant enough to warrant a
     * notification
     */
    private boolean isSignificantStatusChange(TaskStatus status) {
        return switch (status) {
            case BLOCKED ->
                true;      // Task is blocked - urgent
            case DONE ->
                true;         // Task completed - important milestone
            case IN_PROGRESS ->
                true;  // Task started - progress update
            case ARCHIVED ->
                true;     // Task archived - final state
            case TODO ->
                false;        // Back to TODO - less critical
        };
    }

    /**
     * Generates appropriate message based on the task status
     */
    private String getTaskStatusChangeMessage(TaskStatus status, String taskName) {
        return switch (status) {
            case BLOCKED ->
                String.format("Task '%s' you are assigned to was marked as BLOCKED", taskName);
            case DONE ->
                String.format("Great! Task '%s' you were working on has been marked as DONE", taskName);
            case IN_PROGRESS ->
                String.format("Task '%s' you are assigned to is now IN PROGRESS", taskName);
            case ARCHIVED ->
                String.format("Task '%s' you were assigned to has been ARCHIVED", taskName);
            case TODO ->
                String.format("Task '%s' you are assigned to was moved back to TODO", taskName);
        };
    }

    private Flux<Notification> handleCommentAddedEvent(CommentAddedEventPayload commentAdded,
            String eventType, String correlationId) {
        CommentDto commentDto = commentAdded.commentDto();
        String parentId = commentDto.getParentId();
        String parentType = commentDto.getParentType().name();
        String authorId = commentDto.getAuthorId();
        String commentId = commentDto.getId();
        String commentText = commentDto.getContent();
        String parentCommentId = commentDto.getParentCommentId();
        Set<String> mentionedUsernames = MentionUtils.extractMentions(commentText);

        logger.info("CommentAddedEvent received for {} {}. AuthorId: {}. IsReply: {}. CorrID: {}",
                parentType, parentId, authorId, parentCommentId != null, correlationId);

        // Publish domain event to WebSocket for frontend real-time updates
        publishCommentDomainEventToWebSocket(commentAdded, parentType, parentId, correlationId)
                .subscribe(
                        v -> logger.debug("Successfully published comment domain event to WebSocket topic. CorrID: {}", correlationId),
                        e -> logger.error("Failed to publish comment domain event to WebSocket topic. CorrID: {}", correlationId, e)
                );

        if ("TASK".equals(parentType)) {
            return getTaskCommentNotifications(parentId, authorId, commentId, eventType, mentionedUsernames, parentCommentId);
        } else if ("PROJECT".equals(parentType)) {
            return getProjectCommentNotifications(parentId, authorId, commentId, eventType, mentionedUsernames, parentCommentId);
        } else if ("COMMENT".equals(parentType)) {
            // This is for backward compatibility - if parentType is COMMENT, we need to find the original parent
            logger.warn("Received comment with parentType=COMMENT, this should not happen with new replies. ParentId: {}, CommentId: {}", parentId, commentId);

            // For backward compatibility, try to find the original parent through the comment service
            // In this case, parentId is actually the parent comment ID
            return commentWebClient.get()
                    .uri("/{id}", parentId)
                    .retrieve()
                    .bodyToMono(CommentDto.class)
                    .flatMapMany(originalComment -> {
                        logger.info("Found original comment with parentType: {} and parentId: {}",
                                originalComment.getParentType().name(), originalComment.getParentId());

                        if ("TASK".equals(originalComment.getParentType().name())) {
                            return getTaskCommentNotifications(originalComment.getParentId(), authorId,
                                    commentId, eventType, mentionedUsernames, parentId);
                        } else if ("PROJECT".equals(originalComment.getParentType().name())) {
                            return getProjectCommentNotifications(originalComment.getParentId(), authorId,
                                    commentId, eventType, mentionedUsernames, parentId);
                        }
                        return Flux.empty();
                    })
                    .onErrorResume(error -> {
                        logger.error("Error fetching original comment for backward compatibility: {}", error.getMessage());
                        return Flux.empty();
                    });
        }

        return Flux.empty();
    }

    private Flux<Notification> handleCommentDeletedEvent(CommentDeletedEventPayload commentDeleted,
            String eventType, String correlationId) {
        CommentDto commentDto = commentDeleted.commentDto();
        String parentId = commentDto.getParentId();
        String parentType = commentDto.getParentType().name();
        String authorId = commentDto.getAuthorId();
        String commentId = commentDto.getId();
        String parentCommentId = commentDto.getParentCommentId();

        logger.info("CommentDeletedEvent received for {} {}. AuthorId: {}. IsReply: {}. CorrID: {}",
                parentType, parentId, authorId, parentCommentId != null, correlationId);

        // Publish domain event to WebSocket for frontend real-time updates
        publishCommentDomainEventToWebSocket(commentDeleted, parentType, parentId, correlationId)
                .subscribe(
                        v -> logger.debug("Successfully published comment deletion domain event to WebSocket topic. CorrID: {}", correlationId),
                        e -> logger.error("Failed to publish comment deletion domain event to WebSocket topic. CorrID: {}", correlationId, e)
                );

        // For comment deletion, notify relevant users that a comment was removed
        if ("TASK".equals(parentType)) {
            return getTaskCommentNotifications(parentId, authorId, commentId, eventType, Set.of(), parentCommentId);
        } else if ("PROJECT".equals(parentType)) {
            return getProjectCommentNotifications(parentId, authorId, commentId, eventType, Set.of(), parentCommentId);
        } else if ("COMMENT".equals(parentType)) {
            // Backward compatibility handling
            logger.warn("Received comment deletion with parentType=COMMENT, handling for backward compatibility. ParentId: {}, CommentId: {}", parentId, commentId);

            return commentWebClient.get()
                    .uri("/{id}", parentId)
                    .retrieve()
                    .bodyToMono(CommentDto.class)
                    .flatMapMany(originalComment -> {
                        if ("TASK".equals(originalComment.getParentType().name())) {
                            return getTaskCommentNotifications(originalComment.getParentId(), authorId,
                                    commentId, eventType, Set.of(), parentId);
                        } else if ("PROJECT".equals(originalComment.getParentType().name())) {
                            return getProjectCommentNotifications(originalComment.getParentId(), authorId,
                                    commentId, eventType, Set.of(), parentId);
                        }
                        return Flux.empty();
                    })
                    .onErrorResume(error -> {
                        logger.error("Error fetching original comment for deletion backward compatibility: {}", error.getMessage());
                        return Flux.empty();
                    });
        }

        return Flux.empty();
    }

    private Flux<Notification> handleCommentEditedEvent(CommentEditedEventPayload commentEdited,
            String eventType, String correlationId) {
        CommentDto commentDto = commentEdited.commentDto();
        String parentId = commentDto.getParentId();
        String parentType = commentDto.getParentType().name();
        String authorId = commentDto.getAuthorId();
        String commentId = commentDto.getId();
        String commentText = commentDto.getContent();
        String parentCommentId = commentDto.getParentCommentId();
        Set<String> mentionedUsernames = MentionUtils.extractMentions(commentText);

        logger.info("CommentEditedEvent received for {} {}. AuthorId: {}. IsReply: {}. CorrID: {}",
                parentType, parentId, authorId, parentCommentId != null, correlationId);

        // Publish domain event to WebSocket for frontend real-time updates
        publishCommentDomainEventToWebSocket(commentEdited, parentType, parentId, correlationId)
                .subscribe(
                        v -> logger.debug("Successfully published comment edit domain event to WebSocket topic. CorrID: {}", correlationId),
                        e -> logger.error("Failed to publish comment edit domain event to WebSocket topic. CorrID: {}", correlationId, e)
                );

        // For comment editing, notify users about the edit and any new mentions
        if ("TASK".equals(parentType)) {
            return getTaskCommentNotifications(parentId, authorId, commentId, eventType, mentionedUsernames, parentCommentId);
        } else if ("PROJECT".equals(parentType)) {
            return getProjectCommentNotifications(parentId, authorId, commentId, eventType, mentionedUsernames, parentCommentId);
        } else if ("COMMENT".equals(parentType)) {
            // Backward compatibility handling
            logger.warn("Received comment edit with parentType=COMMENT, handling for backward compatibility. ParentId: {}, CommentId: {}", parentId, commentId);

            return commentWebClient.get()
                    .uri("/{id}", parentId)
                    .retrieve()
                    .bodyToMono(CommentDto.class)
                    .flatMapMany(originalComment -> {
                        if ("TASK".equals(originalComment.getParentType().name())) {
                            return getTaskCommentNotifications(originalComment.getParentId(), authorId,
                                    commentId, eventType, mentionedUsernames, parentId);
                        } else if ("PROJECT".equals(originalComment.getParentType().name())) {
                            return getProjectCommentNotifications(originalComment.getParentId(), authorId,
                                    commentId, eventType, mentionedUsernames, parentId);
                        }
                        return Flux.empty();
                    })
                    .onErrorResume(error -> {
                        logger.error("Error fetching original comment for edit backward compatibility: {}", error.getMessage());
                        return Flux.empty();
                    });
        }

        return Flux.empty();
    }

    private Flux<Notification> handleProjectCreatedEvent(ProjectCreatedEventPayload payload, String eventType) {
        var projectDto = payload.projectDto();
        List<Notification> notifications = new ArrayList<>();

        // Notify the project owner
        if (projectDto.getOwnerId() != null) {
            String ownerMessage = String.format("You have been assigned as owner of the new project '%s'", projectDto.getName());
            notifications.add(Notification.builder()
                    .recipientUserId(projectDto.getOwnerId())
                    .event(NotificationEvent.PROJECT_CREATED)
                    .entityType(com.pm.commoncontracts.domain.ParentType.PROJECT)
                    .entityId(projectDto.getId())
                    .channel(NotificationChannel.WEBSOCKET)
                    .message(ownerMessage)
                    .read(false)
                    .createdAt(java.time.Instant.now())
                    .build());

            logger.info("Created notification for project owner: {} for project: {}",
                    projectDto.getOwnerId(), projectDto.getName());
        }

        // Notify all team members (excluding the owner to avoid duplicate notifications)
        if (projectDto.getMemberIds() != null && !projectDto.getMemberIds().isEmpty()) {
            String memberMessage = String.format("You have been added to the new project '%s'", projectDto.getName());

            for (String memberId : projectDto.getMemberIds()) {
                // Skip the owner as they already received an owner-specific notification
                if (!memberId.equals(projectDto.getOwnerId())) {
                    notifications.add(Notification.builder()
                            .recipientUserId(memberId)
                            .event(NotificationEvent.PROJECT_CREATED)
                            .entityType(com.pm.commoncontracts.domain.ParentType.PROJECT)
                            .entityId(projectDto.getId())
                            .channel(NotificationChannel.WEBSOCKET)
                            .message(memberMessage)
                            .read(false)
                            .createdAt(java.time.Instant.now())
                            .build());

                    logger.info("Created notification for team member: {} for project: {}",
                            memberId, projectDto.getName());
                }
            }
        }

        logger.info("Total notifications created for project '{}': {}",
                projectDto.getName(), notifications.size());

        return Flux.fromIterable(notifications);
    }

    private Flux<Notification> handleProjectTaskCreatedEvent(ProjectTaskCreatedEventPayload payload, String eventType) {
        var taskDto = payload.taskDto();
        String assigneeId = taskDto.getAssigneeId();
        if (assigneeId != null) {
            String message = String.format("A new task '%s' has been assigned to you in project", taskDto.getName());

            logger.info("Creating project task creation notification for task '{}' (ID: {}) for assignee: {}",
                    taskDto.getName(), taskDto.getId(), assigneeId);

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

        logger.debug("Project task '{}' created but has no assignee, skipping notification", taskDto.getName());
        return Flux.empty();
    }

    private Flux<Notification> handleTaskCreatedEvent(TaskCreatedEventPayload payload, String eventType) {
        var taskDto = payload.taskDto();
        String assigneeId = taskDto.getAssigneeId();

        if (assigneeId != null) {
            logger.info("Creating task creation notification for task '{}' (ID: {}) for assignee: {}",
                    taskDto.getName(), taskDto.getId(), assigneeId);
        } else {
            logger.debug("Task '{}' created but has no assignee, skipping notification", taskDto.getName());
        }

        return notifyAssigneeOnly(taskDto, eventType, String.format("A new task '%s' has been assigned to you", taskDto.getName()), com.pm.commoncontracts.domain.ParentType.TASK.name());
    }

    private Flux<Notification> handleTaskUpdatedEvent(TaskUpdatedEventPayload payload, String eventType) {
        var taskDto = payload.taskDto();
        String assigneeId = taskDto.getAssigneeId();

        if (assigneeId != null) {
            logger.info("Creating task update notification for task '{}' (ID: {}) for assignee: {}",
                    taskDto.getName(), taskDto.getId(), assigneeId);
        } else {
            logger.debug("Task '{}' updated but has no assignee, skipping notification", taskDto.getName());
        }

        return notifyAssigneeOnly(taskDto, eventType, String.format("Task '%s' was updated", taskDto.getName()), com.pm.commoncontracts.domain.ParentType.TASK.name());
    }

    private Flux<Notification> handleTaskPriorityChangedEvent(TaskPriorityChangedEventPayload payload, String eventType) {
        var taskDto = payload.dto();
        String assigneeId = taskDto.getAssigneeId();

        // Only notify if there's an assignee
        if (assigneeId == null) {
            logger.debug("Task {} priority changed but has no assignee ID", taskDto.getId());
            return Flux.empty();
        }

        String taskName = taskDto.getName();
        String message = getTaskPriorityChangeMessage(taskDto.getPriority(), taskName);

        // Notify assignee for significant priority changes
        if (isSignificantPriorityChange(taskDto.getPriority())) {
            logger.info("Creating priority change notification for task '{}' (priority: {}) for assignee: {}",
                    taskName, taskDto.getPriority(), assigneeId);

            return Flux.just(Notification.builder()
                    .recipientUserId(assigneeId)
                    .event(NotificationEvent.TASK_PRIORITY_CHANGED)
                    .entityType(com.pm.commoncontracts.domain.ParentType.TASK)
                    .entityId(taskDto.getId())
                    .channel(NotificationChannel.WEBSOCKET)
                    .message(message)
                    .read(false)
                    .createdAt(java.time.Instant.now())
                    .build());
        }

        logger.debug("Priority change for task '{}' to {} is not significant enough for notification",
                taskName, taskDto.getPriority());
        return Flux.empty();
    }

    /**
     * Determines if a priority change is significant enough to warrant a
     * notification
     */
    private boolean isSignificantPriorityChange(TaskPriority priority) {
        return switch (priority) {
            case URGENT ->
                true;    // Urgent priority - immediate attention needed
            case HIGH ->
                true;      // High priority - important
            case MEDIUM ->
                false;   // Medium priority - normal workflow
            case LOW ->
                false;      // Low priority - less critical
        };
    }

    /**
     * Generates appropriate message based on the task priority
     */
    private String getTaskPriorityChangeMessage(TaskPriority priority, String taskName) {
        return switch (priority) {
            case URGENT ->
                String.format("🚨 URGENT: Task '%s' priority has been escalated to URGENT!", taskName);
            case HIGH ->
                String.format("⚠️ Task '%s' priority has been set to HIGH", taskName);
            case MEDIUM ->
                String.format("Task '%s' priority has been changed to MEDIUM", taskName);
            case LOW ->
                String.format("Task '%s' priority has been lowered to LOW", taskName);
        };
    }

    // Helper: Notify only the assignee
    private Flux<Notification> notifyAssigneeOnly(TaskDto taskDto, String eventType, String message, String type) {
        String assigneeId = taskDto.getAssigneeId();
        if (assigneeId != null) {
            logger.debug("Creating notification of type '{}' for assignee: {} for task: {}",
                    eventType, assigneeId, taskDto.getName());

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

        logger.debug("No assignee found for task '{}', skipping {} notification", taskDto.getName(), eventType);
        return Flux.empty();
    }

    private Flux<Notification> getTaskCommentNotifications(String taskId, String authorId, String commentId,
            String eventType, Set<String> mentionedUsernames, String parentCommentId) {
        logger.info("Processing task comment notification for task ID: {}, authorId: {}, isReply: {}", taskId, authorId, parentCommentId != null);

        // Get task details to get project information
        Mono<TaskDto> taskMono = taskWebClient.get()
                .uri("/{id}", taskId)
                .retrieve()
                .bodyToMono(TaskDto.class)
                .doOnError(error -> logger.error("Error calling task service for task {}: {}", taskId, error.getMessage()))
                .onErrorResume(error -> {
                    logger.warn("Failed to get task details for task {}, continuing without task info", taskId, error);
                    return Mono.empty();
                });

        // Get all participants based on whether this is a reply or not
        Mono<Set<String>> participantsMono;
        if (parentCommentId != null) {
            // This is a reply - get all participants in the comment thread
            participantsMono = getCommentThreadParticipants(parentCommentId, authorId);
        } else {
            // This is a top-level comment - get comment participants for the task
            participantsMono = getTaskCommentParticipants(taskId, authorId);
        }

        return Mono.zip(taskMono.defaultIfEmpty(null), participantsMono)
                .flatMapMany(tuple -> {
                    TaskDto task = tuple.getT1();
                    Set<String> commentParticipants = tuple.getT2();

                    List<Notification> notifications = new ArrayList<>();
                    Set<String> notifiedUsers = new HashSet<>();

                    // Add author to avoid self-notification
                    notifiedUsers.add(authorId);

                    // Get project members for this task
                    if (task != null && task.getProjectId() != null) {
                        return projectWebClient.get()
                                .uri("/{id}", task.getProjectId())
                                .retrieve()
                                .bodyToMono(com.pm.commoncontracts.dto.ProjectDto.class)
                                .doOnError(error -> logger.error("Error calling project service for project {}: {}", task.getProjectId(), error.getMessage()))
                                .onErrorResume(error -> {
                                    logger.warn("Failed to get project details for project {}, notifying limited participants", task.getProjectId(), error);
                                    return Mono.empty();
                                })
                                .map(project -> {
                                    // Notify project owner (if exists and not the author)
                                    if (project.getOwnerId() != null && !notifiedUsers.contains(project.getOwnerId())) {
                                        String message = getCommentNotificationMessage(eventType,
                                                parentCommentId != null ? "task comment thread you are involved in" : "task in your project");
                                        notifications.add(createNotification(project.getOwnerId(), eventType, taskId,
                                                com.pm.commoncontracts.domain.ParentType.TASK, message));
                                        notifiedUsers.add(project.getOwnerId());
                                        logger.info("Added notification for project owner: {} for task: {}", project.getOwnerId(), taskId);
                                    }

                                    // Notify all project members (excluding author and those already notified)
                                    if (project.getMemberIds() != null) {
                                        for (String memberId : project.getMemberIds()) {
                                            if (!notifiedUsers.contains(memberId)) {
                                                String message = getCommentNotificationMessage(eventType,
                                                        parentCommentId != null ? "task comment thread in your project" : "task in your project");
                                                notifications.add(createNotification(memberId, eventType, taskId,
                                                        com.pm.commoncontracts.domain.ParentType.TASK, message));
                                                notifiedUsers.add(memberId);
                                                logger.info("Added notification for project member: {} for task: {}", memberId, taskId);
                                            }
                                        }
                                    }

                                    // Notify task assignee specifically (if exists and not already notified)
                                    if (task.getAssigneeId() != null && !notifiedUsers.contains(task.getAssigneeId())) {
                                        String message = getCommentNotificationMessage(eventType,
                                                parentCommentId != null ? "task comment thread you are assigned to" : "task you are assigned to");
                                        notifications.add(createNotification(task.getAssigneeId(), eventType, taskId,
                                                com.pm.commoncontracts.domain.ParentType.TASK, message));
                                        notifiedUsers.add(task.getAssigneeId());
                                        logger.info("Added notification for task assignee: {} for task: {}", task.getAssigneeId(), taskId);
                                    }

                                    // Notify all previous comment participants (excluding those already notified)
                                    for (String participantId : commentParticipants) {
                                        if (!notifiedUsers.contains(participantId)) {
                                            String message = getCommentNotificationMessage(eventType,
                                                    parentCommentId != null ? "comment thread you participated in" : "task you commented on");
                                            notifications.add(createNotification(participantId, eventType, taskId,
                                                    com.pm.commoncontracts.domain.ParentType.TASK, message));
                                            notifiedUsers.add(participantId);
                                            logger.info("Added notification for task comment participant: {} for task: {}", participantId, taskId);
                                        }
                                    }

                                    // Notify mentioned users (excluding those already notified)
                                    for (String mentionedUser : mentionedUsernames) {
                                        if (!notifiedUsers.contains(mentionedUser)) {
                                            String mentionMessage = getCommentNotificationMessage(eventType, "task");
                                            String message = "You were mentioned in a comment on " + mentionMessage.toLowerCase()
                                                    .replace("a comment was", "a").replace("A comment was", "a");
                                            notifications.add(createNotification(mentionedUser, eventType, taskId,
                                                    com.pm.commoncontracts.domain.ParentType.TASK, message));
                                            notifiedUsers.add(mentionedUser);
                                            logger.info("Added notification for mentioned user: {} in task: {}", mentionedUser, taskId);
                                        }
                                    }

                                    logger.info("Total task comment notifications created for task {}: {}", taskId, notifications.size());
                                    return notifications;
                                })
                                .flatMapMany(notificationList -> Flux.fromIterable(notificationList));
                    } else {
                        // Fallback if no task/project info - just notify comment participants and mentions
                        logger.warn("No task or project information available, notifying limited participants for task {}", taskId);

                        // Notify comment participants
                        for (String participantId : commentParticipants) {
                            if (!notifiedUsers.contains(participantId)) {
                                String message = getCommentNotificationMessage(eventType,
                                        parentCommentId != null ? "comment thread you participated in" : "task you commented on");
                                notifications.add(createNotification(participantId, eventType, taskId,
                                        com.pm.commoncontracts.domain.ParentType.TASK, message));
                                notifiedUsers.add(participantId);
                            }
                        }

                        // Notify mentioned users
                        for (String mentionedUser : mentionedUsernames) {
                            if (!notifiedUsers.contains(mentionedUser)) {
                                String mentionMessage = getCommentNotificationMessage(eventType, "task");
                                String message = "You were mentioned in a comment on " + mentionMessage.toLowerCase()
                                        .replace("a comment was", "a").replace("A comment was", "a");
                                notifications.add(createNotification(mentionedUser, eventType, taskId,
                                        com.pm.commoncontracts.domain.ParentType.TASK, message));
                                notifiedUsers.add(mentionedUser);
                            }
                        }

                        logger.info("Total fallback task comment notifications created for task {}: {}", taskId, notifications.size());
                        return Flux.fromIterable(notifications);
                    }
                })
                .onErrorResume(error -> {
                    logger.error("Error creating task comment notifications for task {}", taskId, error);
                    return Flux.empty();
                });
    }

    private Flux<Notification> getProjectCommentNotifications(String projectId, String authorId, String commentId,
            String eventType, Set<String> mentionedUsernames, String parentCommentId) {
        logger.info("Processing project comment notification for project ID: {}, authorId: {}, isReply: {}", projectId, authorId, parentCommentId != null);

        // Get project details to get all members
        Mono<com.pm.commoncontracts.dto.ProjectDto> projectMono = projectWebClient.get()
                .uri("/{id}", projectId)
                .retrieve()
                .bodyToMono(com.pm.commoncontracts.dto.ProjectDto.class)
                .doOnError(error -> logger.error("Error calling project service for project {}: {}", projectId, error.getMessage()))
                .onErrorResume(error -> {
                    logger.error("Failed to get project details for project {}, skipping notifications", projectId, error);
                    return Mono.empty();
                });

        // Get all participants based on whether this is a reply or not
        Mono<Set<String>> participantsMono;
        if (parentCommentId != null) {
            // This is a reply - get all participants in the comment thread
            participantsMono = getCommentThreadParticipants(parentCommentId, authorId);
        } else {
            // This is a top-level comment - get comment participants for the project
            participantsMono = getProjectCommentParticipants(projectId, authorId);
        }

        return Mono.zip(projectMono, participantsMono)
                .flatMapMany(tuple -> {
                    com.pm.commoncontracts.dto.ProjectDto project = tuple.getT1();
                    Set<String> commentParticipants = tuple.getT2();

                    List<Notification> notifications = new ArrayList<>();
                    Set<String> notifiedUsers = new HashSet<>();

                    // Add author to avoid self-notification
                    notifiedUsers.add(authorId);

                    // Notify project owner (if exists and not the author)
                    if (project.getOwnerId() != null && !notifiedUsers.contains(project.getOwnerId())) {
                        String message = getCommentNotificationMessage(eventType,
                                parentCommentId != null ? "project comment thread you own" : "project you own");
                        notifications.add(createNotification(project.getOwnerId(), eventType, projectId,
                                com.pm.commoncontracts.domain.ParentType.PROJECT, message));
                        notifiedUsers.add(project.getOwnerId());
                        logger.info("Added notification for project owner: {} for project: {}", project.getOwnerId(), projectId);
                    }

                    // Notify all project members (excluding author and those already notified)
                    if (project.getMemberIds() != null) {
                        for (String memberId : project.getMemberIds()) {
                            if (!notifiedUsers.contains(memberId)) {
                                String message = getCommentNotificationMessage(eventType,
                                        parentCommentId != null ? "project comment thread you are a member of" : "project you are a member of");
                                notifications.add(createNotification(memberId, eventType, projectId,
                                        com.pm.commoncontracts.domain.ParentType.PROJECT, message));
                                notifiedUsers.add(memberId);
                                logger.info("Added notification for project member: {} for project: {}", memberId, projectId);
                            }
                        }
                    }

                    // Notify all previous comment participants (excluding those already notified)
                    for (String participantId : commentParticipants) {
                        if (!notifiedUsers.contains(participantId)) {
                            String message = getCommentNotificationMessage(eventType,
                                    parentCommentId != null ? "comment thread you participated in" : "project you commented on");
                            notifications.add(createNotification(participantId, eventType, projectId,
                                    com.pm.commoncontracts.domain.ParentType.PROJECT, message));
                            notifiedUsers.add(participantId);
                            logger.info("Added notification for project comment participant: {} for project: {}", participantId, projectId);
                        }
                    }

                    // Notify mentioned users (excluding those already notified)
                    for (String mentionedUser : mentionedUsernames) {
                        if (!notifiedUsers.contains(mentionedUser)) {
                            String mentionMessage = getCommentNotificationMessage(eventType, "project");
                            String message = "You were mentioned in a comment on " + mentionMessage.toLowerCase()
                                    .replace("a comment was", "a").replace("A comment was", "a");
                            notifications.add(createNotification(mentionedUser, eventType, projectId,
                                    com.pm.commoncontracts.domain.ParentType.PROJECT, message));
                            notifiedUsers.add(mentionedUser);
                            logger.info("Added notification for mentioned user: {} in project: {}", mentionedUser, projectId);
                        }
                    }

                    logger.info("Total project comment notifications created for project {}: {}", projectId, notifications.size());
                    return Flux.fromIterable(notifications);
                })
                .onErrorResume(error -> {
                    logger.error("Error creating project comment notifications for project {}", projectId, error);
                    return Flux.empty();
                });
    }

    /**
     * Helper method to create a notification with common fields
     */
    private Notification createNotification(String recipientUserId, String eventType, String entityId,
            com.pm.commoncontracts.domain.ParentType entityType, String message) {
        return Notification.builder()
                .recipientUserId(recipientUserId)
                .event(NotificationEvent.valueOf(eventType))
                .entityType(entityType)
                .entityId(entityId)
                .channel(NotificationChannel.WEBSOCKET)
                .message(message)
                .read(false)
                .createdAt(java.time.Instant.now())
                .build();
    }

    /**
     * Get all users who have previously commented on a task (excluding the
     * current author)
     */
    private Mono<Set<String>> getTaskCommentParticipants(String taskId, String excludeAuthor) {
        logger.debug("Fetching comment participants for task: {}", taskId);

        // Call comment service to get all comments for this task
        return commentWebClient.get()
                .uri("/task/{taskId}/participants", taskId)
                .retrieve()
                .bodyToFlux(String.class)
                .collect(HashSet<String>::new, Set::add)
                .map(participants -> {
                    Set<String> result = new HashSet<>(participants);
                    result.remove(excludeAuthor); // Remove current comment author
                    logger.debug("Found {} comment participants for task {}: {}", result.size(), taskId, result);
                    return (Set<String>) result;
                })
                .onErrorResume(error -> {
                    logger.warn("Failed to get comment participants for task {}: {}", taskId, error.getMessage());
                    return Mono.just(new HashSet<>());
                });
    }

    /**
     * Get all users who have previously commented on a project (excluding the
     * current author)
     */
    private Mono<Set<String>> getProjectCommentParticipants(String projectId, String excludeAuthor) {
        logger.debug("Fetching comment participants for project: {}", projectId);

        // Call comment service to get all comments for this project
        return commentWebClient.get()
                .uri("/project/{projectId}/participants", projectId)
                .retrieve()
                .bodyToFlux(String.class)
                .collect(HashSet<String>::new, Set::add)
                .map(participants -> {
                    Set<String> result = new HashSet<>(participants);
                    result.remove(excludeAuthor); // Remove current comment author
                    logger.debug("Found {} comment participants for project {}: {}", result.size(), projectId, result);
                    return result;
                })
                .onErrorResume(error -> {
                    logger.warn("Failed to get comment participants for project {}: {}", projectId, error.getMessage());
                    return Mono.just(new HashSet<>());
                });
    }

    /**
     * Get all users who have participated in a comment thread (including the
     * parent comment and all replies)
     */
    private Mono<Set<String>> getCommentThreadParticipants(String parentCommentId, String excludeAuthor) {
        logger.debug("Fetching comment thread participants for parent comment: {}", parentCommentId);

        // Call comment service to get all participants in the comment thread
        return commentWebClient.get()
                .uri("/comment/{parentCommentId}/thread-participants", parentCommentId)
                .retrieve()
                .bodyToFlux(String.class)
                .collect(HashSet<String>::new, Set::add)
                .map(participants -> {
                    Set<String> result = new HashSet<>(participants);
                    result.remove(excludeAuthor); // Remove current comment author
                    logger.debug("Found {} comment thread participants for parent comment {}: {}", result.size(), parentCommentId, result);
                    return result;
                })
                .onErrorResume(error -> {
                    logger.warn("Failed to get comment thread participants for parent comment {}: {}", parentCommentId, error.getMessage());
                    return Mono.just(new HashSet<>());
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

    private String getCommentNotificationMessage(String eventType, String context) {
        return switch (eventType) {
            case "COMMENT_ADDED" ->
                "A new comment was added to " + context;
            case "COMMENT_DELETED" ->
                "A comment was deleted from " + context;
            case "COMMENT_EDITED" ->
                "A comment was edited on " + context;
            default ->
                "A comment was updated on " + context;
        };
    }

    public Flux<NotificationDto> getNotificationsForUser(String recipientUserId) {
        logger.debug("Fetching notifications for user: {}", recipientUserId);
        return notificationRepository.findByrecipientUserId(recipientUserId)
                .map(notification -> {
                    NotificationDto dto = NotificationUtils.entityToDto(notification);
                    logger.debug("Mapped notification: id={}, entityType={}, entityId={}, eventType={}",
                            dto.getId(), dto.getEntityType(), dto.getEntityId(), dto.getEventType());
                    return dto;
                })
                .doOnComplete(() -> logger.debug("Completed fetching notifications for user: {}", recipientUserId))
                .doOnError(e -> logger.error("Error fetching notifications for user: {}", recipientUserId, e))
                .onErrorResume(e -> Flux.error(new RuntimeException("Failed to fetch notifications for user", e)))
                .onErrorContinue((throwable, o) -> logger.error("Unhandled error in getNotificationsForUser, skipping element", throwable));
    }

    public Mono<Void> markNotificationRead(String notificationId, String userId) {
        return Mono.deferContextual(contextView
                -> notificationRepository.findById(notificationId)
                        .switchIfEmpty(Mono.error(new RuntimeException("Notification not found with ID: " + notificationId)))
                        .flatMap(notification -> {
                            if (!notification.getRecipientUserId().equals(userId)) {
                                return Mono.error(new RuntimeException("User not authorized to mark this notification as read"));
                            }

                            // If already read, just return without error
                            if (notification.isRead()) {
                                logger.debug("Notification {} is already marked as read", notificationId);
                                return Mono.empty();
                            }

                            notification.setRead(true);
                            notification.setReadAt(java.time.Instant.now());
                            return notificationRepository.save(notification)
                                    .doOnSuccess(saved -> {
                                        logger.info("Successfully marked notification {} as read for user {}", notificationId, userId);
                                        publishNotificationReadEvent(saved, contextView);
                                    })
                                    .then();
                        })
                        .doOnError(error -> logger.error("Error marking notification {} as read for user {}: {}",
                        notificationId, userId, error.getMessage()))
        );
    }

    /**
     * Publishes a NotificationReadEvent after the notification is marked as
     * read.
     */
    private void publishNotificationReadEvent(Notification notification, ContextView contextView) {
        String correlationId = contextView.getOrDefault(
                MdcLoggingFilter.CORRELATION_ID_CONTEXT_KEY,
                "N/A-notification-read"
        );
        NotificationDto dto = NotificationUtils.entityToDto(notification);
        NotificationReadEventPayload payload = new NotificationReadEventPayload(dto);
        EventEnvelope<NotificationReadEventPayload> envelope = new EventEnvelope<>(
                correlationId,
                NotificationReadEventPayload.EVENT_TYPE,
                serviceName,
                payload
        );
        kafkaTemplate.send(notificationEventsTopic, envelope)
                .doOnError(e -> logger.error("Failed to send NotificationReadEvent envelope. CorrID: {}", correlationId, e))
                .subscribe();
    }

    private Flux<Notification> handleProjectCreatedEventFallback(Object payload, String eventType, String correlationId) {
        try {
            // Try to extract project data from the payload object
            if (payload instanceof java.util.Map<?, ?> payloadMap) {
                Object projectDtoObj = payloadMap.get("projectDto");
                if (projectDtoObj instanceof java.util.Map<?, ?> projectMap) {
                    String projectId = (String) projectMap.get("id");
                    String projectName = (String) projectMap.get("name");
                    String ownerId = (String) projectMap.get("ownerId");
                    Object memberIdsObj = projectMap.get("memberIds");

                    List<Notification> notifications = new ArrayList<>();

                    // Notify the project owner
                    if (ownerId != null && projectId != null && projectName != null) {
                        logger.info("Successfully extracted project data from fallback deserialization. Project: {}, Owner: {}, CorrID: {}",
                                projectName, ownerId, correlationId);

                        String ownerMessage = String.format("You have been assigned as owner of the new project '%s'", projectName);
                        notifications.add(Notification.builder()
                                .recipientUserId(ownerId)
                                .event(NotificationEvent.PROJECT_CREATED)
                                .entityType(com.pm.commoncontracts.domain.ParentType.PROJECT)
                                .entityId(projectId)
                                .channel(NotificationChannel.WEBSOCKET)
                                .message(ownerMessage)
                                .read(false)
                                .createdAt(java.time.Instant.now())
                                .build());

                        logger.info("Created fallback notification for project owner: {} for project: {}, CorrID: {}",
                                ownerId, projectName, correlationId);
                    }

                    // Notify team members
                    if (memberIdsObj instanceof java.util.List<?> memberIdsList && projectId != null && projectName != null) {
                        String memberMessage = String.format("You have been added to the new project '%s'", projectName);

                        for (Object memberIdObj : memberIdsList) {
                            if (memberIdObj instanceof String memberId) {
                                // Skip the owner as they already received an owner-specific notification
                                if (!memberId.equals(ownerId)) {
                                    notifications.add(Notification.builder()
                                            .recipientUserId(memberId)
                                            .event(NotificationEvent.PROJECT_CREATED)
                                            .entityType(com.pm.commoncontracts.domain.ParentType.PROJECT)
                                            .entityId(projectId)
                                            .channel(NotificationChannel.WEBSOCKET)
                                            .message(memberMessage)
                                            .read(false)
                                            .createdAt(java.time.Instant.now())
                                            .build());

                                    logger.info("Created fallback notification for team member: {} for project: {}, CorrID: {}",
                                            memberId, projectName, correlationId);
                                }
                            }
                        }
                    }

                    if (!notifications.isEmpty()) {
                        logger.info("Total fallback notifications created for project '{}': {}, CorrID: {}",
                                projectName, notifications.size(), correlationId);
                        return Flux.fromIterable(notifications);
                    }
                }
            }

            logger.warn("Could not extract project data from payload fallback for PROJECT_CREATED event. CorrID: {}", correlationId);
            return Flux.empty();
        } catch (Exception e) {
            logger.error("Error in PROJECT_CREATED fallback handler. CorrID: {}", correlationId, e);
            return Flux.empty();
        }
    }

    /**
     * Publishes comment domain events to WebSocket topics for frontend
     * real-time updates
     */
    private Mono<Void> publishCommentDomainEventToWebSocket(Object eventPayload, String parentType, String parentId, String correlationId) {
        // Create envelope for WebSocket dispatch
        EventEnvelope<?> websocketEnvelope = new EventEnvelope<>(
                correlationId,
                determineWebSocketEventType(eventPayload),
                serviceName,
                eventPayload
        );

        // Determine the WebSocket topic based on parent type and ID
        String websocketTopic = parentType.toLowerCase() + ":" + parentId;

        logger.debug("Publishing comment domain event to WebSocket topic: {} for CorrID: {}", websocketTopic, correlationId);

        return kafkaTemplate.send(websocketDispatchTopic, websocketTopic, websocketEnvelope)
                .doOnError(e -> logger.error("Failed to publish comment domain event to WebSocket topic: {}. CorrID: {}", websocketTopic, correlationId, e))
                .then();
    }

    /**
     * Determines the WebSocket event type based on the payload
     */
    private String determineWebSocketEventType(Object eventPayload) {
        if (eventPayload instanceof CommentAddedEventPayload) {
            return "COMMENT_ADDED";
        } else if (eventPayload instanceof CommentEditedEventPayload) {
            return "COMMENT_EDITED";
        } else if (eventPayload instanceof CommentDeletedEventPayload) {
            return "COMMENT_DELETED";
        }
        return "UNKNOWN_COMMENT_EVENT";
    }
}
