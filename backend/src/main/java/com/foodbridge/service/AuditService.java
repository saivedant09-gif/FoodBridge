package com.foodbridge.service;

import com.foodbridge.dto.PageResponse;
import com.foodbridge.model.AuditLog;
import com.foodbridge.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void log(Long actorUserId, String action, String entityType, String entityId, String metadata) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .actorUserId(actorUserId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .metadata(metadata)
                    .build();
            auditRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log for action: {}, error: {}", action, e.getMessage());
        }
    }

    public PageResponse<AuditLog> getAll(int page, int size) {
        if (size <= 0) {
            size = 10;
        }
        if (page < 0) {
            page = 0;
        }

        List<AuditLog> content = auditRepository.findAll(page, size);
        long totalElements = auditRepository.count();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean last = (page + 1) >= totalPages;

        return PageResponse.<AuditLog>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }
}
