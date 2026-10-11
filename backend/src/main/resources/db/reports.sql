-- =============================================================
-- FoodBridge Analytical Queries
-- These queries power the dashboard analytics API endpoints.
-- All values are calculated from actual database records.
-- =============================================================

-- 1. Platform impact summary
SELECT
    (SELECT COUNT(*) FROM organizations WHERE verification_status = 'APPROVED') AS verified_orgs,
    (SELECT COUNT(*) FROM food_listings)                                          AS total_listings,
    (SELECT COUNT(*) FROM food_listings WHERE listing_status = 'ACTIVE')          AS active_listings,
    (SELECT COUNT(*) FROM food_listings WHERE listing_status = 'COMPLETED')       AS completed_listings,
    (SELECT COUNT(*) FROM food_listings WHERE listing_status = 'EXPIRED')         AS expired_listings,
    (SELECT COUNT(*) FROM food_requests)                                           AS total_requests,
    (SELECT COUNT(*) FROM food_requests WHERE request_status = 'FULFILLED')       AS fulfilled_requests,
    (SELECT COALESCE(SUM(allocated_quantity),0)
     FROM allocations WHERE allocation_status IN ('CONFIRMED','COMPLETED'))        AS total_allocated,
    (SELECT COUNT(*) FROM collections WHERE collection_status = 'COLLECTED')      AS completed_collections,
    (SELECT COUNT(*) FROM users WHERE role = 'PROVIDER')                          AS provider_users,
    (SELECT COUNT(*) FROM users WHERE role = 'RECIPIENT')                         AS recipient_users;

-- 2. Food category distribution
SELECT
    category,
    COUNT(*) AS listing_count,
    SUM(total_quantity) AS total_qty
FROM food_listings
WHERE listing_status NOT IN ('CANCELLED')
GROUP BY category
ORDER BY listing_count DESC;

-- 3. Monthly collections over last 6 months
SELECT
    YEAR(c.collected_at)  AS yr,
    MONTH(c.collected_at) AS mo,
    MONTHNAME(c.collected_at) AS month_name,
    COUNT(*)              AS collections_count,
    SUM(a.allocated_quantity) AS total_qty_collected
FROM collections c
JOIN allocations a ON c.allocation_id = a.allocation_id
WHERE c.collection_status = 'COLLECTED'
  AND c.collected_at >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
GROUP BY YEAR(c.collected_at), MONTH(c.collected_at), MONTHNAME(c.collected_at)
ORDER BY yr ASC, mo ASC;

-- 4. Request status distribution
SELECT
    request_status,
    COUNT(*) AS count
FROM food_requests
GROUP BY request_status;

-- 5. Provider stats for a given org (replace ? with org_id)
SELECT
    COUNT(DISTINCT fl.listing_id)                                      AS total_listings,
    SUM(CASE WHEN fl.listing_status = 'ACTIVE'    THEN 1 ELSE 0 END)  AS active_listings,
    SUM(CASE WHEN fl.listing_status = 'COMPLETED' THEN 1 ELSE 0 END)  AS completed_listings,
    COALESCE(SUM(fl.total_quantity), 0)                                AS total_qty_listed,
    COALESCE(SUM(CASE WHEN a.allocation_status IN ('CONFIRMED','COMPLETED')
                       THEN a.allocated_quantity ELSE 0 END), 0)       AS total_qty_allocated,
    COUNT(DISTINCT CASE WHEN fr.request_status = 'PENDING'
                        THEN fr.request_id END)                         AS pending_requests,
    COUNT(DISTINCT CASE WHEN c.collection_status = 'COLLECTED'
                        THEN c.collection_id END)                       AS completed_collections
FROM food_listings fl
LEFT JOIN food_requests fr ON fl.listing_id = fr.listing_id
LEFT JOIN allocations a ON fl.listing_id = a.listing_id
LEFT JOIN collections c ON a.allocation_id = c.allocation_id
WHERE fl.provider_organization_id = ?;

-- 6. Recipient stats (replace ? with recipient_org_id)
SELECT
    COUNT(DISTINCT fr.request_id)                                             AS total_requests,
    SUM(CASE WHEN fr.request_status = 'FULFILLED' THEN 1 ELSE 0 END)         AS fulfilled_requests,
    SUM(CASE WHEN fr.request_status = 'PENDING'   THEN 1 ELSE 0 END)         AS pending_requests,
    COALESCE(SUM(CASE WHEN a.allocation_status IN ('CONFIRMED','COMPLETED')
                       THEN a.allocated_quantity ELSE 0 END), 0)               AS total_qty_received,
    COUNT(DISTINCT CASE WHEN c.collection_status='COLLECTED' THEN c.collection_id END) AS completed_collections
FROM food_requests fr
LEFT JOIN allocations a ON fr.request_id = a.request_id
LEFT JOIN collections c ON a.allocation_id = c.allocation_id
WHERE fr.recipient_organization_id = ?;

-- 7. EXPLAIN example: active listing search query
EXPLAIN
SELECT fl.*, o.organization_name
FROM food_listings fl
JOIN organizations o ON fl.provider_organization_id = o.organization_id
WHERE fl.listing_status = 'ACTIVE'
  AND fl.expires_at > NOW()
  AND fl.pickup_deadline > NOW()
  AND fl.available_quantity > 0
ORDER BY fl.pickup_deadline ASC
LIMIT 10 OFFSET 0;

-- 8. Top providers by quantity listed
SELECT
    o.organization_name,
    COUNT(fl.listing_id) AS listing_count,
    SUM(fl.total_quantity) AS total_qty,
    SUM(CASE WHEN fl.listing_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_listings
FROM organizations o
JOIN food_listings fl ON o.organization_id = fl.provider_organization_id
GROUP BY o.organization_id, o.organization_name
ORDER BY total_qty DESC
LIMIT 10;

-- 9. Overdue collections (pickup deadline passed, not yet COLLECTED)
SELECT
    c.collection_id,
    c.pickup_deadline,
    c.collection_status,
    fl.food_name,
    po.organization_name AS provider,
    ro.organization_name AS recipient
FROM collections c
JOIN allocations a ON c.allocation_id = a.allocation_id
JOIN food_listings fl ON a.listing_id = fl.listing_id
JOIN organizations po ON fl.provider_organization_id = po.organization_id
JOIN organizations ro ON a.recipient_organization_id = ro.organization_id
WHERE c.pickup_deadline < NOW()
  AND c.collection_status NOT IN ('COLLECTED','CANCELLED')
ORDER BY c.pickup_deadline ASC;

-- 10. Fulfillment rate calculation
SELECT
    COUNT(*) AS total_eligible_requests,
    SUM(CASE WHEN request_status = 'FULFILLED' THEN 1 ELSE 0 END) AS fulfilled,
    ROUND(
        100.0 * SUM(CASE WHEN request_status = 'FULFILLED' THEN 1 ELSE 0 END)
              / NULLIF(COUNT(*), 0)
    , 2) AS fulfillment_rate_pct
FROM food_requests
WHERE request_status NOT IN ('CANCELLED');
