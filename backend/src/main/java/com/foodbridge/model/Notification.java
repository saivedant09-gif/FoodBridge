package com.foodbridge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private Long notificationId;
    private Long userId;
    private String title;
    private String message;
    private String notificationType;
    private Boolean isRead;
    private String entityType;
    private Long entityId;
    private LocalDateTime createdAt;
}
