-- =============================================================
-- FoodBridge Triggers
-- Run after schema.sql
-- =============================================================

DELIMITER $$

-- 1. Auto-expire listings when expires_at passes
--    Note: actual expiry is also handled by the Spring @Scheduled task.
--    This trigger fires on UPDATE to food_listings if an admin manually changes status.

-- 2. Audit trigger: log organization verification changes
DROP TRIGGER IF EXISTS trg_org_verification_change$$
CREATE TRIGGER trg_org_verification_change
AFTER UPDATE ON organizations
FOR EACH ROW
BEGIN
    IF OLD.verification_status <> NEW.verification_status THEN
        INSERT INTO audit_log (actor_user_id, action, entity_type, entity_id, metadata)
        VALUES (
            NEW.verified_by,
            'ORG_VERIFICATION_CHANGED',
            'organization',
            CAST(NEW.organization_id AS CHAR),
            JSON_OBJECT(
                'org_name', NEW.organization_name,
                'old_status', OLD.verification_status,
                'new_status', NEW.verification_status
            )
        );
    END IF;
END$$

-- 3. Prevent negative available_quantity on food_listings
DROP TRIGGER IF EXISTS trg_listing_quantity_check$$
CREATE TRIGGER trg_listing_quantity_check
BEFORE UPDATE ON food_listings
FOR EACH ROW
BEGIN
    IF NEW.available_quantity < 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'available_quantity cannot be negative';
    END IF;
    IF NEW.available_quantity > NEW.total_quantity THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'available_quantity cannot exceed total_quantity';
    END IF;
END$$

-- 4. Auto-mark listing as FULLY_ALLOCATED when available_quantity drops to 0
DROP TRIGGER IF EXISTS trg_listing_fully_allocated$$
CREATE TRIGGER trg_listing_fully_allocated
BEFORE UPDATE ON food_listings
FOR EACH ROW
BEGIN
    IF NEW.available_quantity = 0 AND NEW.listing_status = 'ACTIVE' THEN
        SET NEW.listing_status = 'FULLY_ALLOCATED';
    END IF;
END$$

-- 5. On collection COLLECTED: mark allocation COMPLETED and request FULFILLED
DROP TRIGGER IF EXISTS trg_collection_complete$$
CREATE TRIGGER trg_collection_complete
AFTER UPDATE ON collections
FOR EACH ROW
BEGIN
    IF NEW.collection_status = 'COLLECTED' AND OLD.collection_status <> 'COLLECTED' THEN
        UPDATE allocations
           SET allocation_status = 'COMPLETED',
               updated_at = NOW()
         WHERE allocation_id = NEW.allocation_id;

        UPDATE food_requests fr
          JOIN allocations a ON fr.request_id = a.request_id
           SET fr.request_status = 'FULFILLED',
               fr.updated_at = NOW()
         WHERE a.allocation_id = NEW.allocation_id;
    END IF;
END$$

DELIMITER ;
