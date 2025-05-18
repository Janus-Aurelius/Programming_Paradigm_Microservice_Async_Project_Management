package com.pm.commentservice.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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

    @PreAuthorize("hasPermission(#commentDto, 'Comment', 'CREATE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CommentDto> createComment(@RequestBody CommentDto commentDto, @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId, ServerHttpRequest request) {
        String userId = extractUserIdFromHeader(request);
        commentDto.setAuthorId(userId);
        return commentService.createComment(commentDto, correlationId != null ? correlationId : "N/A");
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Flux<CommentDto> getAllComments(Pageable pageable) { // Or custom pagination params
        return commentService.getAllComments(pageable);
    }

    @PreAuthorize("hasPermission(#projectId, 'Project', 'VIEW')")
    @GetMapping("/project/{projectId}")
    public Flux<CommentDto> getCommentsForProject(@PathVariable String projectId) {
        return commentService.getCommentsForParent(projectId, ParentType.PROJECT);
    }

    @PreAuthorize("hasPermission(#taskId, 'Task', 'VIEW')")
    @GetMapping("/task/{taskId}")
    public Flux<CommentDto> getCommentsForTask(@PathVariable String taskId) {
        return commentService.getCommentsForParent(taskId, ParentType.TASK);
    }

    @PreAuthorize("hasPermission(#parentCommentId, 'Comment', 'REPLY')")
    @PostMapping("/{parentCommentId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CommentDto> createReply(@PathVariable String parentCommentId, @RequestBody CommentDto replyDto, @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId, ServerHttpRequest request) {
        String userId = extractUserIdFromHeader(request);
        replyDto.setAuthorId(userId);
        return commentService.createReply(parentCommentId, replyDto, correlationId != null ? correlationId : "N/A");
    }

    @PreAuthorize("hasPermission(#parentCommentId, 'Comment', 'VIEW_REPLIES')")
    @GetMapping("/{parentCommentId}/replies")
    public Flux<CommentDto> getRepliesForComment(@PathVariable String parentCommentId) {
        return commentService.getRepliesForComment(parentCommentId);
    }

    @PreAuthorize("hasPermission(#commentId, 'Comment', 'EDIT')")
    @PutMapping("/{commentId}")
    public Mono<CommentDto> updateComment(@PathVariable String commentId, @RequestBody CommentDto commentDto, @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return commentService.updateComment(commentId, commentDto.getContent(), correlationId != null ? correlationId : "N/A");
    }

    @PreAuthorize("hasPermission(#commentId, 'Comment', 'DELETE')")
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteComment(@PathVariable String commentId, @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return commentService.deleteComment(commentId, correlationId != null ? correlationId : "N/A");
    }
}
