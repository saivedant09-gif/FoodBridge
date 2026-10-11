package com.foodbridge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for background maintenance operations.
 * Each task is idempotent and transaction-safe.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {

    private final FoodListingService foodListingService;
    private final AuditService auditService;

    /**
     * Expire overdue food listings every 15 minutes.
     * A listing is expired when expires_at < NOW() or pickup_deadline < NOW()
     * and listing_status = 'ACTIVE'.
     * This is idempotent — running it multiple times has the same effect.
     */
    @Scheduled(fixedDelay = 900_000) // every 15 minutes
    public void expireOverdueListings() {
        try {
            int expired = foodListingService.expireOverdueListings();
            if (expired > 0) {
                log.info("[ScheduledTask] Expired {} overdue food listing(s).", expired);
                auditService.log(null, "BATCH_EXPIRY", "food_listing", "BATCH",
                        "{\"expired_count\":" + expired + "}");
            }
        } catch (Exception e) {
            log.error("[ScheduledTask] Error during listing expiry: {}", e.getMessage());
        }
    }
}
