-- =============================================================
-- FoodBridge Indexes
-- Run after schema.sql
-- =============================================================

-- users
CREATE INDEX idx_users_org ON users(organization_id);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(account_status);

-- organizations
CREATE INDEX idx_org_status ON organizations(verification_status);
CREATE INDEX idx_org_type ON organizations(organization_type);
CREATE INDEX idx_org_city ON organizations(city);

-- food_listings
CREATE INDEX idx_listings_provider ON food_listings(provider_organization_id);
CREATE INDEX idx_listings_status ON food_listings(listing_status);
CREATE INDEX idx_listings_expires ON food_listings(expires_at);
CREATE INDEX idx_listings_deadline ON food_listings(pickup_deadline);
CREATE INDEX idx_listings_category ON food_listings(category);
-- composite for the most common available-food search
CREATE INDEX idx_listings_active_cat ON food_listings(listing_status, category, expires_at);
CREATE INDEX idx_listings_city ON food_listings(pickup_city);

-- food_requests
CREATE INDEX idx_requests_listing ON food_requests(listing_id);
CREATE INDEX idx_requests_recipient ON food_requests(recipient_organization_id);
CREATE INDEX idx_requests_status ON food_requests(request_status);
CREATE INDEX idx_requests_created ON food_requests(created_at);

-- allocations
CREATE INDEX idx_alloc_listing ON allocations(listing_id);
CREATE INDEX idx_alloc_recipient ON allocations(recipient_organization_id);
CREATE INDEX idx_alloc_status ON allocations(allocation_status);

-- collections
CREATE INDEX idx_coll_status ON collections(collection_status);
CREATE INDEX idx_coll_deadline ON collections(pickup_deadline);

-- notifications
CREATE INDEX idx_notif_user_unread ON notifications(user_id, is_read);
CREATE INDEX idx_notif_created ON notifications(created_at);

-- audit_log
CREATE INDEX idx_audit_actor ON audit_log(actor_user_id);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_log(created_at);

-- issues
CREATE INDEX idx_issues_reporter ON issues(reported_by);
CREATE INDEX idx_issues_status ON issues(issue_status);
