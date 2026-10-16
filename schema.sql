-- MySQL setup for Lime Gym.
-- The app creates these tables by itself and fills an empty equipment table from the CSV,
-- so running this file is optional. It's here to show the design and for a manual reset.
-- Column names match what the DAO classes read and write.
CREATE DATABASE IF NOT EXISTS gym_equipment_tracker;
USE gym_equipment_tracker;

-- One row per machine. Searching and sorting happen in memory in our own structures;
-- MySQL keeps the data between runs.
CREATE TABLE IF NOT EXISTS equipment (
    equipment_id     VARCHAR(10)  PRIMARY KEY,   -- EQ-00001 ... EQ-05000
    name             VARCHAR(100) NOT NULL,
    category         VARCHAR(30)  NOT NULL,      -- CARDIO, STRENGTH_MACHINE, FREE_WEIGHT, FUNCTIONAL, ACCESSORY
    location         VARCHAR(50)  NOT NULL,      -- floor zone, same name as a graph node
    condition_status VARCHAR(20)  NOT NULL,      -- EXCELLENT, GOOD, FAIR, POOR, CRITICAL
    usage_count      INT          NOT NULL
);

-- Every finished service, so each machine's history survives a restart.
CREATE TABLE IF NOT EXISTS maintenance_log (
    log_id           INT AUTO_INCREMENT PRIMARY KEY,
    equipment_id     VARCHAR(10)  NOT NULL,
    note             VARCHAR(200) NOT NULL,
    condition_before VARCHAR(20)  NOT NULL,
    condition_after  VARCHAR(20)  NOT NULL,
    serviced_by      VARCHAR(32)  NOT NULL,      -- username
    serviced_on      DATE         NOT NULL,
    INDEX idx_log_equipment (equipment_id)
);

-- Sign-in accounts. Only a random salt and a PBKDF2 hash, never the password.
CREATE TABLE IF NOT EXISTS app_users (
    username         VARCHAR(32)  PRIMARY KEY,   -- stored in lowercase
    role             VARCHAR(10)  NOT NULL,      -- ADMIN, STAFF, VIEWER
    salt             VARCHAR(64)  NOT NULL,
    password_hash    VARCHAR(128) NOT NULL,
    created_order    INT          NOT NULL DEFAULT 0
);

-- To reset the equipment to the original 5,000 records before a demo:
--   TRUNCATE TABLE equipment;   then start the app (or run db.DatasetImporter).
-- To start over with accounts (the app will ask for a new admin):
--   TRUNCATE TABLE app_users;
