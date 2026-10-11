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
public class AuditLog {
    private Long auditId;
    private Long actorUserId;
    private String action;
    private String entityType;
    private String entityId;
    private String metadata;
    private String ipAddress;
    private LocalDateTime createdAt;
}
