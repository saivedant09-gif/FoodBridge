-- =============================================================
-- FoodBridge — Complete MySQL Database Setup Script
-- Executes complete schema, indexes, views, triggers,
-- procedures, and synthetic demonstration seed data.
-- =============================================================

CREATE DATABASE IF NOT EXISTS foodbridge
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE foodbridge;

SOURCE d:/FootBridge/database/mysql/schema.sql;
SOURCE d:/FootBridge/database/mysql/indexes.sql;
SOURCE d:/FootBridge/database/mysql/views.sql;
SOURCE d:/FootBridge/database/mysql/triggers.sql;
SOURCE d:/FootBridge/database/mysql/procedures.sql;
SOURCE d:/FootBridge/database/mysql/seed_data.sql;

SELECT 'FoodBridge database initialized successfully!' AS Status;
