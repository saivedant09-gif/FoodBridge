-- =============================================================
-- FoodBridge Database Schema
-- MySQL 8.0 | InnoDB | UTF8MB4
-- =============================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS issues;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS food_safety_checks;
DROP TABLE IF EXISTS collections;
DROP TABLE IF EXISTS allocations;
DROP TABLE IF EXISTS food_requests;
DROP TABLE IF EXISTS food_listings;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS organization_members;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS organizations;

SET FOREIGN_KEY_CHECKS = 1;

-- -------------------------------------------------------------
-- 1. organizations
-- Stores both provider and recipient organizations.
-- Verification status controls whether recipients can request food.
-- -------------------------------------------------------------
CREATE TABLE organizations (
    organization_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_name VARCHAR(200)  NOT NULL,
    organization_type ENUM('RESTAURANT','CAFETERIA','BAKERY','CATERER',
                           'EVENT_ORGANIZER','NGO','SHELTER',
                           'COMMUNITY_KITCHEN','OTHER') NOT NULL,
    description       TEXT,
    contact_email     VARCHAR(255)  NOT NULL,
    contact_phone     VARCHAR(30),
    address           TEXT         NOT NULL,
    city              VARCHAR(100),
    state             VARCHAR(100),
    pincode           VARCHAR(20),
    latitude          DECIMAL(10,7),
    longitude         DECIMAL(10,7),
    verification_status ENUM('PENDING','APPROVED','REJECTED','SUSPENDED')
                        NOT NULL DEFAULT 'PENDING',
    verified_at       DATETIME,
    verified_by       INT UNSIGNED,    -- FK to users.user_id (admin), set after table creation
    created_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_org_email CHECK (contact_email LIKE '%@%')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
-- 2. users
-- Stores all user accounts. role determines access scope.
-- Each non-admin user belongs to exactly one organization.
-- -------------------------------------------------------------
CREATE TABLE users (
    user_id           INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    organization_id   INT UNSIGNED,
    full_name         VARCHAR(200)  NOT NULL,
    email             VARCHAR(255)  NOT NULL,
    password_hash     VARCHAR(255)  NOT NULL,
    phone             VARCHAR(30),
    role              ENUM('PROVIDER','RECIPIENT','ADMIN') NOT NULL,
    account_status    ENUM('ACTIVE','SUSPENDED','PENDING_VERIFICATION')
                      NOT NULL DEFAULT 'ACTIVE',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_org FOREIGN KEY (organization_id)
        REFERENCES organizations(organization_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_user_email CHECK (email LIKE '%@%')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add deferred FK from organizations.verified_by -> users
ALTER TABLE organizations
    ADD CONSTRAINT fk_org_verified_by
        FOREIGN KEY (verified_by) REFERENCES users(user_id)
        ON DELETE SET NULL ON UPDATE CASCADE;

-- -------------------------------------------------------------
-- 3. food_listings
-- Core inventory entity. available_quantity is updated atomically
-- during allocation. InnoDB row-level locking prevents overselling.
-- -------------------------------------------------------------
CREATE TABLE food_listings (
    listing_id              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    provider_organization_id INT UNSIGNED NOT NULL,
    food_name               VARCHAR(200)  NOT NULL,
    description             TEXT,
    category                ENUM('COOKED_MEAL','RAW_PRODUCE','BAKERY','DAIRY',
                                 'PACKAGED','BEVERAGES','SNACKS','OTHER') NOT NULL,
    total_quantity          DECIMAL(10,2) NOT NULL,
    available_quantity      DECIMAL(10,2) NOT NULL,
    unit                    VARCHAR(50)   NOT NULL,          -- e.g., portions, kg, litres
    dietary_info            SET('VEGETARIAN','VEGAN','GLUTEN_FREE',
                                'HALAL','KOSHER','NUT_FREE','DAIRY_FREE'),
    allergen_notes          TEXT,
    prepared_at             DATETIME,
    expires_at              DATETIME      NOT NULL,
    pickup_deadline         DATETIME      NOT NULL,
    pickup_address          TEXT          NOT NULL,
    pickup_city             VARCHAR(100),
    listing_status          ENUM('ACTIVE','FULLY_ALLOCATED','EXPIRED',
                                 'CANCELLED','COMPLETED') NOT NULL DEFAULT 'ACTIVE',
    created_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                          ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_listings_org FOREIGN KEY (provider_organization_id)
        REFERENCES organizations(organization_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_listing_quantity
        CHECK (total_quantity > 0 AND available_quantity >= 0
               AND available_quantity <= total_quantity),
    CONSTRAINT chk_listing_dates
        CHECK (expires_at >= pickup_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
-- 4. food_requests
-- A verified recipient requests a quantity from a listing.
-- -------------------------------------------------------------
CREATE TABLE food_requests (
    request_id              INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    listing_id              INT UNSIGNED NOT NULL,
    recipient_organization_id INT UNSIGNED NOT NULL,
    requested_quantity      DECIMAL(10,2) NOT NULL,
    request_status          ENUM('PENDING','APPROVED','REJECTED',
                                 'CANCELLED','FULFILLED') NOT NULL DEFAULT 'PENDING',
    message                 TEXT,
    rejection_reason        TEXT,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_requests_listing FOREIGN KEY (listing_id)
        REFERENCES food_listings(listing_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_requests_org FOREIGN KEY (recipient_organization_id)
        REFERENCES organizations(organization_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_request_quantity CHECK (requested_quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
-- 5. allocations
-- Created when a provider approves a request.
-- allocated_quantity <= available_quantity enforced transactionally.
-- -------------------------------------------------------------
CREATE TABLE allocations (
    allocation_id           INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    request_id              INT UNSIGNED NOT NULL,
    listing_id              INT UNSIGNED NOT NULL,
    recipient_organization_id INT UNSIGNED NOT NULL,
    allocated_quantity      DECIMAL(10,2) NOT NULL,
    allocation_status       ENUM('CONFIRMED','CANCELLED','COMPLETED','EXPIRED')
                            NOT NULL DEFAULT 'CONFIRMED',
    confirmed_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at            DATETIME,
    cancellation_reason     TEXT,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_allocation_request UNIQUE (request_id),
    CONSTRAINT fk_alloc_request FOREIGN KEY (request_id)
        REFERENCES food_requests(request_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_alloc_listing FOREIGN KEY (listing_id)
        REFERENCES food_listings(listing_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_alloc_org FOREIGN KEY (recipient_organization_id)
        REFERENCES organizations(organization_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_alloc_quantity CHECK (allocated_quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
-- 6. collections
-- Records the physical handover of food.
-- Both provider and recipient must confirm before status = COLLECTED.
-- -------------------------------------------------------------
CREATE TABLE collections (
    collection_id           INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    allocation_id           INT UNSIGNED NOT NULL,
    pickup_deadline         DATETIME NOT NULL,
    collection_status       ENUM('SCHEDULED','READY_FOR_PICKUP',
                                 'COLLECTED','MISSED','CANCELLED')
                            NOT NULL DEFAULT 'SCHEDULED',
    provider_confirmed      TINYINT(1) NOT NULL DEFAULT 0,
    provider_confirmed_at   DATETIME,
    recipient_confirmed     TINYINT(1) NOT NULL DEFAULT 0,
    recipient_confirmed_at  DATETIME,
    collected_at            DATETIME,
    notes                   TEXT,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_collection_allocation UNIQUE (allocation_id),
    CONSTRAINT fk_collection_alloc FOREIGN KEY (allocation_id)
        REFERENCES allocations(allocation_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
-- 7. food_safety_checks
-- User-entered records; the platform does NOT certify food safety.
-- -------------------------------------------------------------
CREATE TABLE food_safety_checks (
    check_id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    listing_id   INT UNSIGNED NOT NULL,
    checked_by   INT UNSIGNED NOT NULL,
    check_type   VARCHAR(100) NOT NULL,
    check_result ENUM('PASS','FAIL','ADVISORY') NOT NULL,
    notes        TEXT,
    checked_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_safety_listing FOREIGN KEY (listing_id)
        REFERENCES food_listings(listing_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_safety_user FOREIGN KEY (checked_by)
        REFERENCES users(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
-- 8. notifications
-- In-app notifications only; no email/SMS integration.
-- -------------------------------------------------------------
CREATE TABLE notifications (
    notification_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id           INT UNSIGNED NOT NULL,
    title             VARCHAR(255)  NOT NULL,
    message           TEXT          NOT NULL,
    notification_type ENUM('REQUEST_RECEIVED','REQUEST_APPROVED',
                           'REQUEST_REJECTED','ALLOCATION_CONFIRMED',
                           'PICKUP_REMINDER','COLLECTION_COMPLETED',
                           'LISTING_EXPIRED','ORG_VERIFIED',
                           'ORG_REJECTED','SYSTEM') NOT NULL,
    is_read           TINYINT(1)   NOT NULL DEFAULT 0,
    entity_type       VARCHAR(50),
    entity_id         INT UNSIGNED,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
-- 9. audit_log
-- Immutable record of important actions. Only admins can read.
-- No passwords, tokens, or raw personal data stored here.
-- -------------------------------------------------------------
CREATE TABLE audit_log (
    audit_id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    actor_user_id INT UNSIGNED,
    action        VARCHAR(100) NOT NULL,
    entity_type   VARCHAR(50)  NOT NULL,
    entity_id     VARCHAR(50),
    metadata      JSON,
    ip_address    VARCHAR(45),
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (actor_user_id)
        REFERENCES users(user_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------
-- 10. issues
-- Reports filed by users about listings or allocations.
-- -------------------------------------------------------------
CREATE TABLE issues (
    issue_id             INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    reported_by          INT UNSIGNED NOT NULL,
    related_listing_id   INT UNSIGNED,
    related_allocation_id INT UNSIGNED,
    issue_type           ENUM('FOOD_QUALITY','INCORRECT_QUANTITY',
                              'MISSED_PICKUP','FRAUD','OTHER') NOT NULL,
    description          TEXT NOT NULL,
    issue_status         ENUM('OPEN','INVESTIGATING','RESOLVED','CLOSED')
                         NOT NULL DEFAULT 'OPEN',
    resolution_notes     TEXT,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at          DATETIME,
    CONSTRAINT fk_issue_user FOREIGN KEY (reported_by)
        REFERENCES users(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_issue_listing FOREIGN KEY (related_listing_id)
        REFERENCES food_listings(listing_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_issue_alloc FOREIGN KEY (related_allocation_id)
        REFERENCES allocations(allocation_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
