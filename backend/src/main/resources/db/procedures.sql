-- =============================================================
-- FoodBridge Stored Procedures
-- Run after schema.sql
-- =============================================================

DELIMITER $$

-- 1. Generate impact summary for admin dashboard
DROP PROCEDURE IF EXISTS sp_platform_impact_summary$$
CREATE PROCEDURE sp_platform_impact_summary()
BEGIN
    SELECT
        (SELECT COUNT(*) FROM organizations WHERE verification_status = 'APPROVED') AS verified_orgs,
        (SELECT COUNT(*) FROM food_listings)                                          AS total_listings,
        (SELECT COUNT(*) FROM food_listings WHERE listing_status = 'ACTIVE')          AS active_listings,
        (SELECT COUNT(*) FROM food_listings WHERE listing_status = 'COMPLETED')       AS completed_listings,
        (SELECT COUNT(*) FROM food_listings WHERE listing_status = 'EXPIRED')         AS expired_listings,
        (SELECT COUNT(*) FROM food_requests)                                           AS total_requests,
        (SELECT COUNT(*) FROM food_requests WHERE request_status = 'FULFILLED')       AS fulfilled_requests,
        (SELECT COALESCE(SUM(allocated_quantity),0) FROM allocations
          WHERE allocation_status IN ('CONFIRMED','COMPLETED'))                        AS total_allocated,
        (SELECT COUNT(*) FROM collections WHERE collection_status = 'COLLECTED')      AS completed_collections,
        (SELECT COUNT(*) FROM users WHERE role = 'PROVIDER')                          AS provider_users,
        (SELECT COUNT(*) FROM users WHERE role = 'RECIPIENT')                         AS recipient_users;
END$$

-- 2. Expire overdue listings (idempotent - safe to run repeatedly)
DROP PROCEDURE IF EXISTS sp_expire_overdue_listings$$
CREATE PROCEDURE sp_expire_overdue_listings()
BEGIN
    DECLARE affected INT DEFAULT 0;

    UPDATE food_listings
       SET listing_status = 'EXPIRED'
     WHERE listing_status = 'ACTIVE'
       AND (expires_at < NOW() OR pickup_deadline < NOW());

    SET affected = ROW_COUNT();

    -- Log the expiry batch if any listings were expired
    IF affected > 0 THEN
        INSERT INTO audit_log (actor_user_id, action, entity_type, entity_id, metadata)
        VALUES (NULL, 'BATCH_EXPIRY', 'food_listing', 'BATCH',
                JSON_OBJECT('expired_count', affected, 'run_at', NOW()));
    END IF;

    SELECT affected AS listings_expired;
END$$

-- 3. Provider stats for a given organization
DROP PROCEDURE IF EXISTS sp_provider_stats$$
CREATE PROCEDURE sp_provider_stats(IN p_org_id INT UNSIGNED)
BEGIN
    SELECT
        COUNT(DISTINCT fl.listing_id)                                    AS total_listings,
        SUM(CASE WHEN fl.listing_status = 'ACTIVE'    THEN 1 ELSE 0 END) AS active_listings,
        SUM(CASE WHEN fl.listing_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_listings,
        SUM(CASE WHEN fl.listing_status = 'EXPIRED'   THEN 1 ELSE 0 END) AS expired_listings,
        COALESCE(SUM(fl.total_quantity), 0)                               AS total_qty_listed,
        COALESCE(SUM(a.allocated_quantity), 0)                            AS total_qty_allocated,
        COUNT(DISTINCT fr.request_id)                                    AS pending_requests,
        COUNT(DISTINCT CASE WHEN c.collection_status='COLLECTED'
                        THEN c.collection_id END)                         AS completed_collections
    FROM food_listings fl
    LEFT JOIN food_requests fr ON fl.listing_id = fr.listing_id
        AND fr.request_status = 'PENDING'
    LEFT JOIN allocations a ON fl.listing_id = a.listing_id
        AND a.allocation_status IN ('CONFIRMED','COMPLETED')
    LEFT JOIN collections c ON a.allocation_id = c.allocation_id
    WHERE fl.provider_organization_id = p_org_id;
END$$

DELIMITER ;
