-- =============================================================
-- FoodBridge SQL Views
-- Run after schema.sql and seed_data.sql
-- =============================================================

-- 1. Active food listings with provider name
CREATE OR REPLACE VIEW vw_active_food_listings AS
SELECT
    fl.listing_id,
    fl.food_name,
    fl.description,
    fl.category,
    fl.available_quantity,
    fl.total_quantity,
    fl.unit,
    fl.dietary_info,
    fl.allergen_notes,
    fl.expires_at,
    fl.pickup_deadline,
    fl.pickup_address,
    fl.pickup_city,
    fl.listing_status,
    fl.created_at,
    o.organization_id AS provider_org_id,
    o.organization_name AS provider_name,
    o.contact_phone AS provider_phone
FROM food_listings fl
JOIN organizations o ON fl.provider_organization_id = o.organization_id
WHERE fl.listing_status = 'ACTIVE'
  AND fl.expires_at > NOW()
  AND fl.pickup_deadline > NOW()
  AND fl.available_quantity > 0;

-- 2. Platform-wide collection summary
CREATE OR REPLACE VIEW vw_collection_summary AS
SELECT
    c.collection_id,
    c.collection_status,
    c.pickup_deadline,
    c.collected_at,
    a.allocated_quantity,
    fl.food_name,
    fl.category,
    fl.unit,
    po.organization_name AS provider_name,
    ro.organization_name AS recipient_name
FROM collections c
JOIN allocations a ON c.allocation_id = a.allocation_id
JOIN food_listings fl ON a.listing_id = fl.listing_id
JOIN organizations po ON fl.provider_organization_id = po.organization_id
JOIN organizations ro ON a.recipient_organization_id = ro.organization_id;

-- 3. Organization activity summary (providers)
CREATE OR REPLACE VIEW vw_provider_activity AS
SELECT
    o.organization_id,
    o.organization_name,
    COUNT(DISTINCT fl.listing_id)                               AS total_listings,
    SUM(CASE WHEN fl.listing_status = 'ACTIVE'     THEN 1 ELSE 0 END) AS active_listings,
    SUM(CASE WHEN fl.listing_status = 'COMPLETED'  THEN 1 ELSE 0 END) AS completed_listings,
    SUM(CASE WHEN fl.listing_status = 'EXPIRED'    THEN 1 ELSE 0 END) AS expired_listings,
    COALESCE(SUM(fl.total_quantity), 0)                         AS total_quantity_listed,
    COALESCE(SUM(a.allocated_quantity), 0)                      AS total_quantity_allocated
FROM organizations o
LEFT JOIN food_listings fl ON o.organization_id = fl.provider_organization_id
LEFT JOIN allocations a ON fl.listing_id = a.listing_id
    AND a.allocation_status IN ('CONFIRMED', 'COMPLETED')
WHERE o.organization_type IN ('RESTAURANT','CAFETERIA','BAKERY','CATERER','EVENT_ORGANIZER','OTHER')
GROUP BY o.organization_id, o.organization_name;

-- 4. Recipient activity summary
CREATE OR REPLACE VIEW vw_recipient_activity AS
SELECT
    o.organization_id,
    o.organization_name,
    COUNT(DISTINCT fr.request_id)                                AS total_requests,
    SUM(CASE WHEN fr.request_status = 'FULFILLED' THEN 1 ELSE 0 END) AS fulfilled_requests,
    COALESCE(SUM(a.allocated_quantity), 0)                       AS total_quantity_received,
    COUNT(DISTINCT CASE WHEN c.collection_status = 'COLLECTED' THEN c.collection_id END) AS completed_collections
FROM organizations o
LEFT JOIN food_requests fr ON o.organization_id = fr.recipient_organization_id
LEFT JOIN allocations a ON fr.request_id = a.request_id
    AND a.allocation_status IN ('CONFIRMED','COMPLETED')
LEFT JOIN collections c ON a.allocation_id = c.allocation_id
WHERE o.organization_type IN ('NGO','SHELTER','COMMUNITY_KITCHEN','OTHER')
GROUP BY o.organization_id, o.organization_name;

-- 5. Pending organization verifications
CREATE OR REPLACE VIEW vw_pending_verifications AS
SELECT
    o.organization_id,
    o.organization_name,
    o.organization_type,
    o.contact_email,
    o.address,
    o.city,
    o.created_at,
    u.full_name  AS owner_name,
    u.email      AS owner_email
FROM organizations o
JOIN users u ON u.organization_id = o.organization_id
WHERE o.verification_status = 'PENDING';
