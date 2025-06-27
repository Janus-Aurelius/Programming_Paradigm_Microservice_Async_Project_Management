package com.pm.commentservice.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.pm.commentservice.model.Comment;
import com.pm.commentservice.repository.CommentRepository;
import com.pm.commentservice.utils.CommentUtils;
import com.pm.commoncontracts.domain.ParentType;
import com.pm.commoncontracts.dto.CommentDto;
import com.pm.commoncontracts.envelope.EventEnvelope;
import com.pm.commoncontracts.events.comment.CommentAddedEventPayload;
import com.pm.commoncontracts.events.comment.CommentDeletedEventPayload;
import com.pm.commoncontracts.events.comment.CommentEditedEventPayload;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> kafkaProducerTemplate;
    private final String commentEventsTopic;
    private final String serviceName;

    public CommentService(CommentRepository commentRepository,
            ReactiveKafkaProducerTemplate<String, EventEnvelope<?>> kafkaProducerTemplate,
            @Value("${kafka.topic.comment-events}") String commentEventsTopic,
            @Value("${spring.application.name}") String serviceName) {
        this.commentRepository = commentRepository;
        this.kafkaProducerTemplate = kafkaProducerTemplate;
        this.commentEventsTopic = commentEventsTopic;
        this.serviceName = serviceName;
    }

    public Flux<CommentDto> getAllComments(Pageable pageable) {
        return commentRepository.findAll()
                .sort((c1, c2) -> {
                    int comparison = 0;
                    for (Sort.Order order : pageable.getSort()) {
                        switch (order.getProperty()) {
                            case "createdAt":
                                comparison = c1.getCreatedAt().compareTo(c2.getCreatedAt());
                                break;
                            case "updatedAt":
                                comparison = c1.getUpdatedAt().compareTo(c2.getUpdatedAt());
                                break;
                            default:
                                comparison = c1.getCreatedAt().compareTo(c2.getCreatedAt());
                        }
                        if (comparison != 0) {
                            return order.isAscending() ? comparison : -comparison;
                        }
                    }
                    return comparison;
                })
                .skip(pageable.getPageNumber() * pageable.getPageSize())
                .take(pageable.getPageSize())
                .map(CommentUtils::entityToDto)
                .doOnError(e -> log.error("Error fetching all comments", e))
                .onErrorResume(e -> Flux.error(new RuntimeException("Failed to fetch comments", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getAllComments, skipping element", throwable));
    }

    public Mono<CommentDto> createComment(CommentDto dto, String correlationId) {
        if (!StringUtils.hasText(dto.getParentId())) {
            return Mono.error(new IllegalArgumentException("Parent ID must be provided"));
        }
        if (dto.getParentType() == null) {
            return Mono.error(new IllegalArgumentException("Parent type must be provided"));
        }

        Comment comment = new Comment();
        comment.setContent(dto.getContent());
        comment.setUserId(dto.getAuthorId());
        comment.setParentId(dto.getParentId());
        comment.setParentType(dto.getParentType());
        comment.setCreatedAt(Instant.now());
        comment.setUpdatedAt(comment.getCreatedAt());

        return commentRepository.save(comment)
                .map(CommentUtils::entityToDto)
                .flatMap(savedDto -> publishCommentCreatedEvent(savedDto, correlationId)
                .thenReturn(savedDto))
                .doOnError(e -> log.error("Error creating comment", e))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to create comment", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in createComment, skipping element", throwable));
    }

    public Mono<CommentDto> createReply(String parentCommentId, CommentDto replyDto, String correlationId) {
        if (!StringUtils.hasText(parentCommentId)) {
            return Mono.error(new IllegalArgumentException("Parent comment ID must be provided"));
        }

        return commentRepository.findById(parentCommentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Parent comment not found")))
                .flatMap(parentComment -> {
                    Comment reply = new Comment();
                    reply.setContent(replyDto.getContent());
                    reply.setUserId(replyDto.getAuthorId());
                    reply.setParentId(parentCommentId);
                    reply.setParentType(ParentType.COMMENT);
                    reply.setCreatedAt(Instant.now());
                    reply.setUpdatedAt(reply.getCreatedAt());

                    return commentRepository.save(reply)
                            .map(CommentUtils::entityToDto)
                            .flatMap(savedDto -> publishCommentCreatedEvent(savedDto, correlationId)
                            .thenReturn(savedDto));
                })
                .doOnError(e -> log.error("Error creating reply", e))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to create reply", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in createReply, skipping element", throwable));
    }

    public Flux<CommentDto> getCommentsForParent(String parentId, ParentType parentType) {
        if (!StringUtils.hasText(parentId)) {
            return Flux.error(new IllegalArgumentException("Parent ID must be provided"));
        }
        return commentRepository.findByParentIdAndParentTypeOrderByCreatedAtAsc(parentId, parentType)
                .map(CommentUtils::entityToDto)
                .doOnError(e -> log.error("Error fetching comments for parent: {}", parentId, e))
                .onErrorResume(e -> Flux.error(new RuntimeException("Failed to fetch comments for parent", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getCommentsForParent, skipping element", throwable));
    }

    public Flux<CommentDto> getRepliesForComment(String parentCommentId) {
        if (!StringUtils.hasText(parentCommentId)) {
            return Flux.error(new IllegalArgumentException("Parent comment ID must be provided"));
        }

        return commentRepository.findByParentIdAndParentTypeOrderByCreatedAtAsc(
                parentCommentId, ParentType.COMMENT)
                .map(CommentUtils::entityToDto)
                .doOnError(e -> log.error("Error fetching replies for comment: {}", parentCommentId, e))
                .onErrorResume(e -> Flux.error(new RuntimeException("Failed to fetch replies for comment", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getRepliesForComment, skipping element", throwable));
    }

    public Mono<CommentDto> updateComment(String commentId, String newContent, String correlationId) {
        return commentRepository.findById(commentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Comment not found")))
                .flatMap(comment -> {
                    comment.setContent(newContent);
                    comment.setUpdatedAt(Instant.now());
                    return commentRepository.save(comment);
                })
                .map(CommentUtils::entityToDto)
                .flatMap(updatedDto -> publishCommentEditedEvent(updatedDto, correlationId).thenReturn(updatedDto))
                .doOnError(e -> log.error("Error updating comment", e))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to update comment", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in updateComment, skipping element", throwable));
    }

    public Mono<Void> deleteComment(String commentId, String correlationId) {
        if (!StringUtils.hasText(commentId)) {
            return Mono.error(new IllegalArgumentException("Comment ID must be provided"));
        }

        return commentRepository.findById(commentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Comment not found with ID: " + commentId)))
                .flatMap(comment -> {
                    // Create DTO before deletion for the event
                    CommentDto commentDto = CommentUtils.entityToDto(comment);

                    // Delete all replies first (recursive deletion)
                    return commentRepository.findByParentIdAndParentTypeOrderByCreatedAtAsc(commentId, ParentType.COMMENT)
                            .flatMap(reply -> deleteComment(reply.getId(), correlationId))
                            .then(commentRepository.delete(comment))
                            .then(publishCommentDeletedEvent(commentDto, correlationId))
                            .doOnSuccess(v -> log.info("Successfully deleted comment with ID: {}. CorrID: {}", commentId, correlationId));
                })
                .doOnError(e -> log.error("Error deleting comment with ID: {}. CorrID: {}", commentId, correlationId, e))
                .onErrorResume(e -> {
                    if (e instanceof IllegalArgumentException) {
                        return Mono.error(e); // Re-throw validation errors
                    }
                    return Mono.error(new RuntimeException("Failed to delete comment", e));
                })
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in deleteComment, skipping element", throwable));
    }

    private Mono<Void> publishCommentCreatedEvent(CommentDto commentDto, String correlationId) {
        var payload = new CommentAddedEventPayload(commentDto);
        var envelope = new EventEnvelope<>(correlationId, CommentAddedEventPayload.EVENT_TYPE, serviceName, payload);
        return kafkaProducerTemplate.send(commentEventsTopic, commentDto.getId(), envelope)
                .doOnError(e -> log.error("Failed to send CommentCreatedEvent envelope. CorrID: {}", correlationId, e))
                .onErrorResume(e -> {
                    // TODO: Optionally send to dead-letter topic here
                    log.error("Unrecoverable error publishing CommentCreatedEvent. CorrID: {}", correlationId, e);
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> publishCommentEditedEvent(CommentDto commentDto, String correlationId) {
        var payload = new CommentEditedEventPayload(commentDto);
        var envelope = new EventEnvelope<>(correlationId, CommentEditedEventPayload.EVENT_TYPE, serviceName, payload);
        return kafkaProducerTemplate.send(commentEventsTopic, commentDto.getId(), envelope)
                .doOnError(e -> log.error("Failed to send CommentEditedEvent envelope. CorrID: {}", correlationId, e))
                .onErrorResume(e -> {
                    // TODO: Optionally send to dead-letter topic here
                    log.error("Unrecoverable error publishing CommentEditedEvent. CorrID: {}", correlationId, e);
                    return Mono.empty();
                })
                .then();
    }

    private Mono<Void> publishCommentDeletedEvent(CommentDto commentDto, String correlationId) {
        var payload = new CommentDeletedEventPayload(commentDto);
        var envelope = new EventEnvelope<>(correlationId, CommentDeletedEventPayload.EVENT_TYPE, serviceName, payload);
        return kafkaProducerTemplate.send(commentEventsTopic, commentDto.getId(), envelope)
                .doOnError(e -> log.error("Failed to send CommentDeletedEvent envelope. CorrID: {}", correlationId, e))
                .onErrorResume(e -> {
                    // TODO: Optionally send to dead-letter topic here
                    log.error("Unrecoverable error publishing CommentDeletedEvent. CorrID: {}", correlationId, e);
                    return Mono.empty();
                })
                .then();
    }

    // ==============================
    // Read operations
    // ==============================
    public Mono<CommentDto> getCommentById(String commentId) {
        return commentRepository.findById(commentId)
                .map(CommentUtils::entityToDto)
                .doOnError(e -> log.error("Error fetching comment by ID: {}", commentId, e))
                .switchIfEmpty(Mono.error(new RuntimeException("Comment not found: " + commentId)))
                .onErrorResume(e -> Mono.error(new RuntimeException("Failed to fetch comment by id", e)))
                .onErrorContinue((throwable, o) -> log.error("Unhandled error in getCommentById, skipping element", throwable));
    }

    // ==============================
    // Permission checking operations
    // ==============================
    public Mono<Boolean> hasCommentAccess(org.springframework.security.core.Authentication authentication, CommentDto comment, String action) {
        return Mono.fromCallable(() -> {
            if (authentication == null || !authentication.isAuthenticated() || action == null) {
                return false;
            }

            String currentUserId = getCurrentUserId(authentication);

            try {
                boolean isAuthor = currentUserId != null && currentUserId.equals(comment.getAuthorId());

                // Basic permission checks based on action
                switch (action.toUpperCase()) {
                    case "CMT_READ":
                        // Anyone who can access the parent resource can read comments
                        return true; // Could add more sophisticated checks here
                    case "CMT_UPDATE":
                    case "CMT_EDIT":
                        return isAuthor || hasAdminRole(authentication);
                    case "CMT_DELETE":
                        return isAuthor || hasAdminRole(authentication);
                    case "CMT_REPLY":
                        return true; // Could add more sophisticated checks here
                    default:
                        return false;
                }
            } catch (Exception e) {
                log.error("Error checking comment access for user {} on comment {}", currentUserId, comment.getId(), e);
                return false;
            }
        });
    }

    private String getCurrentUserId(org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() != null) {
            return authentication.getName(); // or extract from principal based on your auth setup
        }
        return null;
    }

    private boolean hasAdminRole(org.springframework.security.core.Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
