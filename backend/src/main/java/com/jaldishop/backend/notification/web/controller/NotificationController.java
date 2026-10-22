package com.jaldishop.backend.notification.web.controller;

import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.notification.application.*;
import com.jaldishop.backend.notification.domain.Notification;
import com.jaldishop.backend.notification.web.dto.NotificationResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@Validated
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
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "La página no puede ser negativa") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
            @Max(value = 100, message = "El tamaño de página no puede exceder 100") int size) {
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
