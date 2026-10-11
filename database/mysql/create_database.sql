-- =============================================================
-- FoodBridge: Create Database and Run All Scripts
-- Execute this file with: mysql -u root -p < create_database.sql
-- =============================================================

CREATE DATABASE IF NOT EXISTS foodbridge
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE foodbridge;

-- Schema and objects are in separate files.
-- Run in this order:
--   1. schema.sql
--   2. indexes.sql
--   3. views.sql
--   4. triggers.sql
--   5. procedures.sql
--   6. seed_data.sql    (demo data only)
