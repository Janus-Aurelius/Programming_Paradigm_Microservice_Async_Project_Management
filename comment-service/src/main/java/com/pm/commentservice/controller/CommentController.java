package com.pm.commentservice.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pm.commentservice.service.CommentService;
import com.pm.commoncontracts.domain.ParentType;
import com.pm.commoncontracts.dto.CommentDto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    private String extractUserIdFromHeader(ServerHttpRequest request) {
        return request.getHeaders().getFirst("X-User-Id");
    }

    @PreAuthorize("@commentPermissionEvaluator.hasPermission(authentication, #commentDto, 'CMT_CREATE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CommentDto> createComment(@RequestBody CommentDto commentDto, @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId, ServerHttpRequest request) {
        String userId = extractUserIdFromHeader(request);
        commentDto.setAuthorId(userId);
        return commentService.createComment(commentDto, correlationId != null ? correlationId : "N/A");
    }

    @GetMapping
    public Flux<CommentDto> getAllComments(Pageable pageable) { // Or custom pagination params
        return commentService.getAllComments(pageable);
    }

    @PreAuthorize("@commentPermissionEvaluator.hasPermission(authentication, #projectId, 'CMT_READ')")
    @GetMapping("/project/{projectId}")
    public Flux<CommentDto> getCommentsForProject(@PathVariable String projectId) {
        return commentService.getCommentsForParent(projectId, ParentType.PROJECT);
    }

    @PreAuthorize("@commentPermissionEvaluator.hasPermission(authentication, #taskId, 'CMT_READ')")
    @GetMapping("/task/{taskId}")
    public Flux<CommentDto> getCommentsForTask(@PathVariable String taskId) {
        return commentService.getCommentsForParent(taskId, ParentType.TASK);
    }

    @PreAuthorize("@commentPermissionEvaluator.hasPermission(authentication, #parentCommentId, 'CMT_CREATE')")
    @PostMapping("/{parentCommentId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CommentDto> createReply(@PathVariable String parentCommentId, @RequestBody CommentDto replyDto, @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId, ServerHttpRequest request) {
        String userId = extractUserIdFromHeader(request);
        replyDto.setAuthorId(userId);
        return commentService.createReply(parentCommentId, replyDto, correlationId != null ? correlationId : "N/A");
    }

    @PreAuthorize("@commentPermissionEvaluator.hasPermission(authentication, #parentCommentId, 'CMT_READ')")
    @GetMapping("/{parentCommentId}/replies")
    public Flux<CommentDto> getRepliesForComment(@PathVariable String parentCommentId) {
        return commentService.getRepliesForComment(parentCommentId);
    }

    @PreAuthorize("@commentPermissionEvaluator.hasPermission(authentication, #commentId, 'CMT_UPDATE')")
    @PutMapping("/{commentId}")
    public Mono<CommentDto> updateComment(@PathVariable String commentId, @RequestBody CommentDto commentDto, @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return commentService.updateComment(commentId, commentDto.getContent(), correlationId != null ? correlationId : "N/A");
    }

    @PreAuthorize("@commentPermissionEvaluator.hasPermission(authentication, #commentId, 'CMT_DELETE')")
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteComment(@PathVariable String commentId, @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return commentService.deleteComment(commentId, correlationId != null ? correlationId : "N/A");
    }

    @PreAuthorize("@commentPermissionEvaluator.hasPermission(authentication, #commentId, 'CMT_READ')")
    @GetMapping("/{commentId}/permissions/check")
    public Mono<ResponseEntity<Map<String, Boolean>>> checkCommentPermissions(@PathVariable String commentId) {
        log.info("Checking permissions for comment ID: {}", commentId);
        // Since we're using @PreAuthorize, if this method is reached, the user has permission
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("hasAccess", true);
        return Mono.just(ResponseEntity.ok(permissions));
    }
}
