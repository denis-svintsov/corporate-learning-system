package com.example.notifications.controller;

import com.example.notifications.dto.CreateNotificationRequest;
import com.example.notifications.dto.NotificationDeliveryStatDto;
import com.example.notifications.dto.NotificationDto;
import com.example.notifications.dto.UnreadCountDto;
import com.example.notifications.service.NotificationService;
import com.example.notifications.service.NotificationStreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationsController {

    private final NotificationService notificationService;
    private final NotificationStreamService notificationStreamService;

    @GetMapping("/my")
    public List<NotificationDto> my(@RequestHeader("X-User-Id") String userId) {
        return notificationService.findMy(userId);
    }

    @GetMapping("/unread-count")
    public UnreadCountDto unreadCount(@RequestHeader("X-User-Id") String userId) {
        return new UnreadCountDto(notificationService.unreadCount(userId));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestHeader("X-User-Id") String userId) {
        return notificationStreamService.subscribe(userId);
    }

    @PatchMapping("/{id}/read")
    public NotificationDto markRead(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId
    ) {
        return notificationService.markRead(id, userId);
    }

    @PatchMapping("/read-all")
    public void markAllRead(@RequestHeader("X-User-Id") String userId) {
        notificationService.markAllRead(userId);
    }

    @PostMapping
    public NotificationDto create(
            @Valid @RequestBody CreateNotificationRequest request,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader
    ) {
        Set<String> roles = parseRoles(rolesHeader);
        if (!roles.contains("ADMIN") && !roles.contains("HR")) {
            throw new com.example.notifications.service.AccessDeniedException("Only ADMIN or HR can create notifications manually");
        }
        return notificationService.create(request);
    }

    @GetMapping("/delivery-stats")
    public List<NotificationDeliveryStatDto> deliveryStats() {
        return notificationService.deliveryStats();
    }

    private Set<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(role -> !role.isBlank())
                .collect(Collectors.toSet());
    }
}
