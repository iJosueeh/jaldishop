package com.jaldishop.backend.notification.web.controller;

import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.notification.application.*;
import com.jaldishop.backend.notification.domain.Notification;
import com.jaldishop.backend.notification.web.dto.NotificationResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final ListUserNotificationsService listUserNotificationsService;
    private final CountUnreadNotificationsService countUnreadNotificationsService;
    private final MarkNotificationAsReadService markNotificationAsReadService;

    public NotificationController(ListUserNotificationsService listUserNotificationsService,
                                  CountUnreadNotificationsService countUnreadNotificationsService,
                                  MarkNotificationAsReadService markNotificationAsReadService) {
        this.listUserNotificationsService = listUserNotificationsService;
        this.countUnreadNotificationsService = countUnreadNotificationsService;
        this.markNotificationAsReadService = markNotificationAsReadService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> listNotifications(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Notification> notifications = listUserNotificationsService.execute(principal.userId(), pageable);
        List<NotificationResponse> response = notifications.stream()
                .map(NotificationResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> countUnread(
            @AuthenticationPrincipal JwtPrincipal principal) {
        long count = countUnreadNotificationsService.execute(principal.userId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        Notification notification = markNotificationAsReadService.execute(id, principal.userId());
        return ResponseEntity.ok(NotificationResponse.fromDomain(notification));
    }
}
