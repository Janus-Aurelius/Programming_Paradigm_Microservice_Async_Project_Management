package com.pm.commentservice.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pm.commentservice.security.CommentPermissionEvaluator;
import com.pm.commentservice.service.CommentService;
import com.pm.commoncontracts.domain.ParentType;
import com.pm.commoncontracts.dto.CommentDto;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;
    private final CommentPermissionEvaluator commentPermissionEvaluator;

    public CommentController(CommentService commentService, CommentPermissionEvaluator commentPermissionEvaluator) {
        this.commentService = commentService;
        this.commentPermissionEvaluator = commentPermissionEvaluator;
    }

    private String extractUserIdFromHeader(ServerHttpRequest request) {
        return request.getHeaders().getFirst("X-User-Id");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CommentDto> createComment(
            @RequestBody CommentDto commentDto,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            ServerHttpRequest request,
            Authentication authentication) {
        String userId = extractUserIdFromHeader(request);
        commentDto.setAuthorId(userId);

        // Check permission reactively
        return commentPermissionEvaluator.hasGeneralPermission(authentication, "CMT_CREATE")
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return Mono.error(new AccessDeniedException("Access denied: insufficient permissions to create comment"));
                    }

                    return commentService.createComment(commentDto, correlationId != null ? correlationId : "N/A");
                });
    }

    @GetMapping
    public Flux<CommentDto> getAllComments(@RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return commentService.getAllComments(pageable);
    }

    @GetMapping("/project/{projectId}")
    public Flux<CommentDto> getCommentsForProject(@PathVariable String projectId, Authentication authentication) {
        // Check permission reactively - use general permission for reading comments
        return commentPermissionEvaluator.hasGeneralPermission(authentication, "CMT_READ")
                .flatMapMany(hasPermission -> {
                    if (!hasPermission) {
                        return Flux.error(new AccessDeniedException("Access denied: insufficient permissions to read comments"));
                    }

                    return commentService.getCommentsForParent(projectId, ParentType.PROJECT);
                });
    }

    @GetMapping("/task/{taskId}")
    public Flux<CommentDto> getCommentsForTask(@PathVariable String taskId, Authentication authentication) {
        // Check permission reactively - use general permission for reading comments
        return commentPermissionEvaluator.hasGeneralPermission(authentication, "CMT_READ")
                .flatMapMany(hasPermission -> {
                    if (!hasPermission) {
                        return Flux.error(new AccessDeniedException("Access denied: insufficient permissions to read comments"));
                    }

                    return commentService.getCommentsForParent(taskId, ParentType.TASK);
                });
    }

    @PostMapping("/{parentCommentId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CommentDto> createReply(
            @PathVariable String parentCommentId,
            @RequestBody CommentDto replyDto,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            ServerHttpRequest request,
            Authentication authentication) {
        String userId = extractUserIdFromHeader(request);
        replyDto.setAuthorId(userId);

        // Check permission reactively
        return commentPermissionEvaluator.hasGeneralPermission(authentication, "CMT_CREATE")
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return Mono.error(new AccessDeniedException("Access denied: insufficient permissions to create reply"));
                    }

                    return commentService.createReply(parentCommentId, replyDto, correlationId != null ? correlationId : "N/A");
                });
    }

    @GetMapping("/{parentCommentId}/replies")
    public Flux<CommentDto> getRepliesForComment(@PathVariable String parentCommentId, Authentication authentication) {
        // Check permission reactively
        return commentPermissionEvaluator.hasGeneralPermission(authentication, "CMT_READ")
                .flatMapMany(hasPermission -> {
                    if (!hasPermission) {
                        return Flux.error(new AccessDeniedException("Access denied: insufficient permissions to read replies"));
                    }

                    return commentService.getRepliesForComment(parentCommentId);
                });
    }

    @PutMapping("/{commentId}")
    public Mono<CommentDto> updateComment(
            @PathVariable String commentId,
            @RequestBody CommentDto commentDto,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication) {

        // Check permission reactively using commentId
        return commentPermissionEvaluator.hasPermission(authentication, commentId, "CMT_UPDATE")
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return Mono.error(new AccessDeniedException("Access denied: insufficient permissions to update this comment"));
                    }

                    return commentService.updateComment(commentId, commentDto.getContent(), correlationId != null ? correlationId : "N/A");
                });
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteComment(
            @PathVariable String commentId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            Authentication authentication) {

        // Check permission reactively using commentId
        return commentPermissionEvaluator.hasPermission(authentication, commentId, "CMT_DELETE")
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return Mono.error(new AccessDeniedException("Access denied: insufficient permissions to delete this comment"));
                    }

                    return commentService.deleteComment(commentId, correlationId != null ? correlationId : "N/A");
                });
    }

    @GetMapping("/{commentId}/permissions/check")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkCommentPermissions(
            @PathVariable String commentId,
            @RequestParam String action,
            Authentication authentication) {
        log.info("Checking permissions for comment ID: {} with action: {}", commentId, action);

        return commentPermissionEvaluator.hasPermission(authentication, commentId, action)
                .map(hasAccess -> {
                    Map<String, Boolean> permissions = new HashMap<>();
                    permissions.put("hasAccess", hasAccess);
                    return ResponseEntity.ok(permissions);
                })
                .onErrorResume(e -> {
                    log.error("Error checking permissions for comment {}: {}", commentId, e.getMessage());
                    Map<String, Boolean> permissions = new HashMap<>();
                    permissions.put("hasAccess", false);
                    return Mono.just(ResponseEntity.ok(permissions));
                })
                .switchIfEmpty(Mono.fromCallable(() -> {
                    Map<String, Boolean> permissions = new HashMap<>();
                    permissions.put("hasAccess", false);
                    return ResponseEntity.ok(permissions);
                }));
    }
}
