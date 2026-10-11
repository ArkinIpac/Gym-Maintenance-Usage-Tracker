-- MySQL setup for the Gym Equipment Maintenance Prioritization System.
-- Run once in MySQL Workbench, then run db.DatasetImporter to load the 5,000 rows.
-- Column names match what EquipmentDAO reads and writes.
CREATE DATABASE IF NOT EXISTS gym_equipment_tracker;
USE gym_equipment_tracker;

CREATE TABLE IF NOT EXISTS equipment (
    equipment_id     VARCHAR(10)  PRIMARY KEY,   -- EQ-00001 ... EQ-05000
    name             VARCHAR(100) NOT NULL,
    category         VARCHAR(30)  NOT NULL,      -- CARDIO, STRENGTH_MACHINE, FREE_WEIGHT, FUNCTIONAL, ACCESSORY
    location         VARCHAR(50)  NOT NULL,      -- floor zone, same name as a graph node
    condition_status VARCHAR(20)  NOT NULL,      -- EXCELLENT, GOOD, FAIR, POOR, CRITICAL
    usage_count      INT          NOT NULL
);

-- To reset the data to the original 5,000 records before a demo:
--   TRUNCATE TABLE equipment;   then run db.DatasetImporter again.
