package com.pm.notificationservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.pm.commoncontracts.dto.NotificationDto;
import com.pm.notificationservice.service.NotificationService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    private String extractUserIdFromHeader(ServerHttpRequest request) {
        return request.getHeaders().getFirst("X-User-Id");
    }    @PreAuthorize("@notificationPermissionEvaluator.hasPermission(authentication, #userId, 'NOTI_READ')")
    @GetMapping
    public Flux<NotificationDto> getUserNotifications(ServerHttpRequest request) {
        String userId = extractUserIdFromHeader(request);
        return notificationService.getNotificationsForUser(userId);
    }

    @PreAuthorize("@notificationPermissionEvaluator.hasPermission(authentication, #notificationId, 'NOTI_UPDATE')")
    @PostMapping("/mark-read/{notificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> markNotificationRead(@PathVariable String notificationId, ServerHttpRequest request) {
        String userId = extractUserIdFromHeader(request);
        return notificationService.markNotificationRead(notificationId, userId);
    }
}
