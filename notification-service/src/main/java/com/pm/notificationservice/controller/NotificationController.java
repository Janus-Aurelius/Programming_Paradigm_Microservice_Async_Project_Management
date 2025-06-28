package com.pm.notificationservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pm.commoncontracts.dto.NotificationDto;
import com.pm.notificationservice.security.ReactiveNotificationPermissionEvaluator;
import com.pm.notificationservice.service.NotificationService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final ReactiveNotificationPermissionEvaluator permissionEvaluator;

    private String extractUserIdFromHeader(ServerHttpRequest request) {
        return request.getHeaders().getFirst("X-User-Id");
    }

    @GetMapping
    public Flux<NotificationDto> getUserNotifications(ServerHttpRequest request, Authentication authentication) {
        String userId = extractUserIdFromHeader(request);

        return permissionEvaluator.hasGeneralPermission(authentication, "NOTI_READ")
                .flatMapMany(hasAccess -> {
                    if (hasAccess) {
                        return notificationService.getNotificationsForUser(userId);
                    } else {
                        return Flux.error(new org.springframework.security.access.AccessDeniedException("Access denied"));
                    }
                });
    }

    @PostMapping("/mark-read/{notificationId}")
    public Mono<ResponseEntity<Void>> markNotificationRead(@PathVariable String notificationId,
            ServerHttpRequest request,
            Authentication authentication) {
        String userId = extractUserIdFromHeader(request);

        return permissionEvaluator.hasPermission(authentication, notificationId, "NOTI_MARK_READ")
                .flatMap(hasAccess -> {
                    if (hasAccess) {
                        return notificationService.markNotificationRead(notificationId, userId)
                                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build());
                    }
                });
    }
}
