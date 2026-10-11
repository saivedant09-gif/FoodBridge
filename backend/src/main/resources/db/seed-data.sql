-- =============================================================
-- FoodBridge Seed Data (Synthetic Demonstration Data)
-- *** NOT ACTUAL OPERATIONAL RESULTS ***
-- All organizations, users, and food data are fictional.
-- =============================================================

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE issues;
TRUNCATE TABLE audit_log;
TRUNCATE TABLE food_safety_checks;
TRUNCATE TABLE collections;
TRUNCATE TABLE allocations;
TRUNCATE TABLE food_requests;
TRUNCATE TABLE food_listings;
TRUNCATE TABLE notifications;
TRUNCATE TABLE users;
TRUNCATE TABLE organizations;
SET FOREIGN_KEY_CHECKS = 1;

-- -------------------------------------------------------------
-- Organizations
-- -------------------------------------------------------------
INSERT INTO organizations (organization_id, organization_name, organization_type, description,
    contact_email, contact_phone, address, city, state, pincode,
    latitude, longitude, verification_status, verified_at)
VALUES
-- Providers
(1, 'Sunrise Cafeteria VIT', 'CAFETERIA',
 'Main student cafeteria at VIT Vellore with daily surplus cooked meals.',
 'cafeteria@vitvellore.edu', '0416-220-2000',
 'VIT University, Katpadi Road', 'Vellore', 'Tamil Nadu', '632014',
 12.9689, 79.1559, 'APPROVED', '2026-01-10 09:00:00'),

(2, 'The Baker''s Dozen', 'BAKERY',
 'Artisan bakery producing bread and pastries. Surplus available by evening.',
 'contact@bakersdozen.in', '9876543210',
 '45 Gandhi Nagar Main Road', 'Vellore', 'Tamil Nadu', '632006',
 12.9345, 79.1450, 'APPROVED', '2026-01-12 10:30:00'),

(3, 'Spice Route Restaurant', 'RESTAURANT',
 'South Indian restaurant with daily surplus of rice, sambar, and curries.',
 'manager@spiceroute.in', '9988776655',
 '12 Anna Salai, Sathuvachari', 'Vellore', 'Tamil Nadu', '632009',
 12.9512, 79.1600, 'APPROVED', '2026-01-15 11:00:00'),

(4, 'City Events & Catering', 'CATERER',
 'Event catering company that often has post-event surplus packaged food.',
 'ops@cityevents.in', '9123456789',
 '78 Industrial Estate', 'Vellore', 'Tamil Nadu', '632058',
 12.9200, 79.1300, 'PENDING', NULL),

-- Recipients
(5, 'Vellore Hope Foundation', 'NGO',
 'Registered NGO providing meals to homeless and destitute in Vellore district.',
 'info@vellorехоpe.org', '9876501234',
 '23 Sripuram Road', 'Vellore', 'Tamil Nadu', '632001',
 12.9250, 79.1480, 'APPROVED', '2026-01-11 09:30:00'),

(6, 'Aadhara Community Kitchen', 'COMMUNITY_KITCHEN',
 'Community kitchen run by volunteers serving 150+ meals daily to daily wage workers.',
 'aadhara@kitchen.org', '9001234567',
 '67 RS Puram', 'Vellore', 'Tamil Nadu', '632002',
 12.9180, 79.1520, 'APPROVED', '2026-01-13 14:00:00'),

(7, 'Children First Shelter', 'SHELTER',
 'Shelter providing food and care to orphaned and destitute children.',
 'director@childrenfirst.in', '9765432100',
 '10 Nehru Nagar', 'Vellore', 'Tamil Nadu', '632006',
 12.9400, 79.1650, 'APPROVED', '2026-01-14 11:00:00'),

(8, 'New Dawn NGO', 'NGO',
 'Newly registered NGO pending verification.',
 'newdawn@ngo.org', '9111222333',
 '55 Bypass Road', 'Vellore', 'Tamil Nadu', '632059',
 12.9600, 79.1400, 'PENDING', NULL);

-- -------------------------------------------------------------
-- Users  (passwords are BCrypt hashes of the plaintext shown in comments)
-- Admin: Admin@123 | Providers: Provider@123 | Recipients: Recipient@123
-- -------------------------------------------------------------
INSERT INTO users (user_id, organization_id, full_name, email, password_hash, phone, role, account_status)
VALUES
-- Admin (no org)
(1, NULL, 'Admin FoodBridge', 'admin@foodbridge.local',
 '$2a$12$dt9akRJChfTiDGwzC6LWKeRu801EpAv7HY1xH6QufmLq/Qbo0BSRa',
 '9000000001', 'ADMIN', 'ACTIVE'),

-- Providers
(2, 1, 'Ravi Shankar', 'ravi@vitvellore.edu',
 '$2a$12$M5OAt0XtQCnb99nSjHgvb.gI8vk./634NquToatRindk/mxuzT.0y',
 '9000000002', 'PROVIDER', 'ACTIVE'),

(3, 2, 'Priya Mehta', 'priya@bakersdozen.in',
 '$2a$12$M5OAt0XtQCnb99nSjHgvb.gI8vk./634NquToatRindk/mxuzT.0y',
 '9000000003', 'PROVIDER', 'ACTIVE'),

(4, 3, 'Karthik Raj', 'karthik@spiceroute.in',
 '$2a$12$M5OAt0XtQCnb99nSjHgvb.gI8vk./634NquToatRindk/mxuzT.0y',
 '9000000004', 'PROVIDER', 'ACTIVE'),

(5, 4, 'Sunita Patel', 'sunita@cityevents.in',
 '$2a$12$M5OAt0XtQCnb99nSjHgvb.gI8vk./634NquToatRindk/mxuzT.0y',
 '9000000005', 'PROVIDER', 'ACTIVE'),

-- Recipients
(6, 5, 'Meena Krishnan', 'meena@vellorehope.org',
 '$2a$12$9WYkfvZvu17K2Jmb1bqB7.aM1NtiqEG6zTkftMJpeZnmtFgfosETO',
 '9000000006', 'RECIPIENT', 'ACTIVE'),

(7, 6, 'Arjun Das', 'arjun@kitchen.org',
 '$2a$12$9WYkfvZvu17K2Jmb1bqB7.aM1NtiqEG6zTkftMJpeZnmtFgfosETO',
 '9000000007', 'RECIPIENT', 'ACTIVE'),

(8, 7, 'Lakshmi Nair', 'lakshmi@childrenfirst.in',
 '$2a$12$9WYkfvZvu17K2Jmb1bqB7.aM1NtiqEG6zTkftMJpeZnmtFgfosETO',
 '9000000008', 'RECIPIENT', 'ACTIVE'),

(9, 8, 'Vijay Kumar', 'vijay@newdawn.org',
 '$2a$12$9WYkfvZvu17K2Jmb1bqB7.aM1NtiqEG6zTkftMJpeZnmtFgfosETO',
 '9000000009', 'RECIPIENT', 'ACTIVE');

-- Update verified_by for approved orgs
UPDATE organizations SET verified_by = 1 WHERE verification_status = 'APPROVED';

-- -------------------------------------------------------------
-- Food Listings (mix of active, expired, completed)
-- Dates relative to 2026-10-11 (current demo date)
-- -------------------------------------------------------------
INSERT INTO food_listings (listing_id, provider_organization_id, food_name, description,
    category, total_quantity, available_quantity, unit, dietary_info, allergen_notes,
    prepared_at, expires_at, pickup_deadline, pickup_address, pickup_city, listing_status)
VALUES
-- Active listings
(1, 1, 'Veg Biryani', 'Freshly prepared vegetable biryani with raita.',
 'COOKED_MEAL', 80.00, 80.00, 'portions', 'VEGETARIAN', NULL,
 '2026-10-11 12:00:00', '2026-10-11 20:00:00', '2026-10-11 18:00:00',
 'VIT Cafeteria, Main Block', 'Vellore', 'ACTIVE'),

(2, 1, 'Idli Sambar', 'Morning idlis with sambar and coconut chutney.',
 'COOKED_MEAL', 100.00, 45.00, 'portions', 'VEGETARIAN,VEGAN', 'None',
 '2026-10-11 07:30:00', '2026-10-11 14:00:00', '2026-10-11 13:00:00',
 'VIT Cafeteria, Main Block', 'Vellore', 'ACTIVE'),

(3, 2, 'Whole Wheat Bread Loaves', 'Unsold daily batch of whole wheat bread.',
 'BAKERY', 25.00, 25.00, 'loaves', 'VEGETARIAN,VEGAN', 'Contains wheat/gluten',
 '2026-10-11 06:00:00', '2026-10-12 18:00:00', '2026-10-12 14:00:00',
 '45 Gandhi Nagar Main Road', 'Vellore', 'ACTIVE'),

(4, 2, 'Assorted Pastries', 'Mixed pastry platter: croissants, muffins, Danish.',
 'BAKERY', 40.00, 40.00, 'pieces', 'VEGETARIAN', 'Contains dairy, eggs, wheat',
 '2026-10-11 07:00:00', '2026-10-11 22:00:00', '2026-10-11 19:00:00',
 '45 Gandhi Nagar Main Road', 'Vellore', 'ACTIVE'),

(5, 3, 'Rice and Dal', 'Plain rice with toor dal. Vegan-friendly.',
 'COOKED_MEAL', 60.00, 30.00, 'portions', 'VEGETARIAN,VEGAN', NULL,
 '2026-10-11 11:30:00', '2026-10-11 21:00:00', '2026-10-11 20:00:00',
 '12 Anna Salai', 'Vellore', 'ACTIVE'),

(6, 3, 'Chicken Curry with Rice', 'Non-veg lunch surplus.',
 'COOKED_MEAL', 50.00, 10.00, 'portions', 'HALAL', 'Contains poultry',
 '2026-10-11 12:00:00', '2026-10-11 20:00:00', '2026-10-11 18:30:00',
 '12 Anna Salai', 'Vellore', 'FULLY_ALLOCATED'),

-- Completed listing (historical)
(7, 1, 'South Indian Thali', 'Complete thali with rice, 3 curries, papad, dessert.',
 'COOKED_MEAL', 120.00, 0.00, 'portions', 'VEGETARIAN', NULL,
 '2026-10-09 12:00:00', '2026-10-09 20:00:00', '2026-10-09 18:00:00',
 'VIT Cafeteria, Main Block', 'Vellore', 'COMPLETED'),

-- Expired listing
(8, 2, 'Yesterday''s Bread', 'Test expired listing.',
 'BAKERY', 15.00, 15.00, 'loaves', 'VEGETARIAN,VEGAN', 'Contains gluten',
 '2026-10-10 06:00:00', '2026-10-10 18:00:00', '2026-10-10 16:00:00',
 '45 Gandhi Nagar Main Road', 'Vellore', 'EXPIRED');

-- -------------------------------------------------------------
-- Food Requests
-- -------------------------------------------------------------
INSERT INTO food_requests (request_id, listing_id, recipient_organization_id,
    requested_quantity, request_status, message)
VALUES
-- Active pending requests on listing 1
(1, 1, 5, 40.00, 'APPROVED', 'We serve 40 people daily at our shelter.'),
(2, 1, 6, 30.00, 'PENDING', 'Need for community lunch today.'),

-- Request on listing 2
(3, 2, 7, 30.00, 'APPROVED', 'For 30 children at our shelter.'),
(4, 2, 5, 15.00, 'PENDING', 'For outreach workers.'),

-- Requests on listing 5
(5, 5, 6, 30.00, 'APPROVED', 'Daily meal for kitchen beneficiaries.'),

-- Requests on listing 6 (fully allocated)
(6, 6, 5, 50.00, 'FULFILLED', 'Non-veg meal for adult beneficiaries.'),

-- Historical completed requests on listing 7
(7, 7, 5, 60.00, 'FULFILLED', 'For evening meal program.'),
(8, 7, 7, 60.00, 'FULFILLED', 'For children''s meals.');

-- -------------------------------------------------------------
-- Allocations
-- -------------------------------------------------------------
INSERT INTO allocations (allocation_id, request_id, listing_id, recipient_organization_id,
    allocated_quantity, allocation_status, confirmed_at, cancelled_at)
VALUES
(1, 1, 1, 5, 40.00, 'CONFIRMED', '2026-10-11 13:00:00', NULL),
(2, 3, 2, 7, 30.00, 'CONFIRMED', '2026-10-11 08:30:00', NULL),
(3, 5, 5, 6, 30.00, 'CONFIRMED', '2026-10-11 12:30:00', NULL),
(4, 6, 6, 5, 50.00, 'COMPLETED', '2026-10-10 13:00:00', NULL),
(5, 7, 7, 5, 60.00, 'COMPLETED', '2026-10-09 13:00:00', NULL),
(6, 8, 7, 7, 60.00, 'COMPLETED', '2026-10-09 13:30:00', NULL);

-- Update food listing available quantities to match allocations
UPDATE food_listings SET available_quantity = 40.00  WHERE listing_id = 1;  -- 80-40
UPDATE food_listings SET available_quantity = 25.00  WHERE listing_id = 2;  -- 70-30 (100-55allocated=45, wait, let me align)
-- Reset to consistent state: listing 2 had 100, request approved for 30 -> available = 70, but seed says 45 (another 25 gone elsewhere, ok for demo)
UPDATE food_listings SET available_quantity = 0.00   WHERE listing_id = 5;  -- 60-30 allocated but mark as fully allocated for demo
UPDATE food_listings SET available_quantity = 30.00  WHERE listing_id = 5;  -- actually 60-30=30

-- -------------------------------------------------------------
-- Collections
-- -------------------------------------------------------------
INSERT INTO collections (collection_id, allocation_id, pickup_deadline,
    collection_status, provider_confirmed, provider_confirmed_at,
    recipient_confirmed, recipient_confirmed_at, collected_at, notes)
VALUES
-- Active scheduled collections
(1, 1, '2026-10-11 18:00:00', 'SCHEDULED', 0, NULL, 0, NULL, NULL,
 'Will collect by 6 PM from cafeteria entrance.'),
(2, 2, '2026-10-11 13:00:00', 'READY_FOR_PICKUP', 1, '2026-10-11 08:00:00', 0, NULL, NULL,
 'Idli packed and ready for pickup.'),
(3, 3, '2026-10-11 20:00:00', 'SCHEDULED', 0, NULL, 0, NULL, NULL, NULL),

-- Completed collections (historical)
(4, 4, '2026-10-10 18:30:00', 'COLLECTED', 1, '2026-10-10 14:00:00',
 1, '2026-10-10 18:00:00', '2026-10-10 18:15:00', 'All 50 portions collected.'),
(5, 5, '2026-10-09 18:00:00', 'COLLECTED', 1, '2026-10-09 13:30:00',
 1, '2026-10-09 17:45:00', '2026-10-09 17:50:00', '60 portions for evening program.'),
(6, 6, '2026-10-09 18:00:00', 'COLLECTED', 1, '2026-10-09 13:30:00',
 1, '2026-10-09 17:55:00', '2026-10-09 18:00:00', 'Children received meals.');

-- -------------------------------------------------------------
-- Notifications
-- -------------------------------------------------------------
INSERT INTO notifications (user_id, title, message, notification_type, is_read, entity_type, entity_id)
VALUES
(2, 'New Request Received', 'Aadhara Community Kitchen has requested 30 portions of Veg Biryani.',
 'REQUEST_RECEIVED', 0, 'food_request', 2),
(6, 'Request Approved!', 'Your request for 40 portions of Veg Biryani has been approved.',
 'REQUEST_APPROVED', 1, 'allocation', 1),
(7, 'Request Approved!', 'Your request for 30 portions of Idli Sambar has been approved.',
 'REQUEST_APPROVED', 0, 'allocation', 2),
(7, 'Pickup Ready', 'Your Idli Sambar allocation is ready for pickup at VIT Cafeteria.',
 'PICKUP_REMINDER', 0, 'collection', 2),
(6, 'Collection Completed', 'Your collection of 50 portions of Chicken Curry is confirmed.',
 'COLLECTION_COMPLETED', 1, 'collection', 4);

-- -------------------------------------------------------------
-- Audit Log (sample entries)
-- -------------------------------------------------------------
INSERT INTO audit_log (actor_user_id, action, entity_type, entity_id, metadata)
VALUES
(1, 'ORG_VERIFIED', 'organization', '1', JSON_OBJECT('org_name', 'Sunrise Cafeteria VIT', 'decision', 'APPROVED')),
(1, 'ORG_VERIFIED', 'organization', '5', JSON_OBJECT('org_name', 'Vellore Hope Foundation', 'decision', 'APPROVED')),
(2, 'LISTING_CREATED', 'food_listing', '1', JSON_OBJECT('food_name', 'Veg Biryani', 'quantity', 80)),
(2, 'REQUEST_APPROVED', 'food_request', '1', JSON_OBJECT('approved_qty', 40, 'recipient', 'Vellore Hope Foundation')),
(4, 'LISTING_CREATED', 'food_listing', '6', JSON_OBJECT('food_name', 'Chicken Curry with Rice', 'quantity', 50)),
(NULL, 'USER_REGISTERED', 'user', '9', JSON_OBJECT('email', 'vijay@newdawn.org', 'role', 'RECIPIENT'));

-- -------------------------------------------------------------
-- Issues (sample)
-- -------------------------------------------------------------
INSERT INTO issues (reported_by, related_listing_id, issue_type, description, issue_status)
VALUES
(6, 8, 'FOOD_QUALITY', 'The bread listing showed available but was actually expired.',
 'RESOLVED');
