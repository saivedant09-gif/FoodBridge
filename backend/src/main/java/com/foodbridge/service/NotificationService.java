package com.foodbridge.service;

import com.foodbridge.dto.PageResponse;
import com.foodbridge.model.Notification;
import com.foodbridge.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notifyUser(Long userId, String title, String message, String type, String entityType, Long entityId) {
        if (userId == null) {
            return;
        }
        try {
            Notification notification = Notification.builder()
                    .userId(userId)
                    .title(title)
                    .message(message)
                    .notificationType(type)
                    .isRead(false)
                    .entityType(entityType)
                    .entityId(entityId)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to create notification for userId {}: {}", userId, e.getMessage());
        }
    }

    public PageResponse<Notification> getMyNotifications(Long userId, int page, int size) {
        if (size <= 0) {
            size = 10;
        }
        if (page < 0) {
            page = 0;
        }

        List<Notification> content = notificationRepository.findByUserId(userId, page, size);
        long totalElements = notificationRepository.countByUserId(userId);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean last = (page + 1) >= totalPages;

        return PageResponse.<Notification>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }

    public long countUnread(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return notificationRepository.countUnreadByUserId(userId);
    }

    public void markRead(Long notificationId, Long userId) {
        if (notificationId != null && userId != null) {
            notificationRepository.markRead(notificationId, userId);
        }
    }

    public void markAllRead(Long userId) {
        if (userId != null) {
            notificationRepository.markAllRead(userId);
        }
    }
}
