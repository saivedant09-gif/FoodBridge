package com.foodbridge.controller;

import com.foodbridge.dto.ApiResponse;
import com.foodbridge.dto.PageResponse;
import com.foodbridge.model.Notification;
import com.foodbridge.model.User;
import com.foodbridge.repository.UserRepository;
import com.foodbridge.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + auth.getName()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Notification>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        PageResponse<Notification> notifications = notificationService.getMyNotifications(user.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved successfully", notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication authentication) {
        User user = getCurrentUser(authentication);
        long count = notificationService.countUnread(user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Unread count retrieved", Map.of("unreadCount", count)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        notificationService.markRead(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", "Marked as read"));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<String>> markAllRead(Authentication authentication) {
        User user = getCurrentUser(authentication);
        notificationService.markAllRead(user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", "All marked as read"));
    }
}
