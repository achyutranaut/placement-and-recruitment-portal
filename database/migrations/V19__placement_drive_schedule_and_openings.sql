-- ============================================================
-- V19__placement_drive_schedule_and_openings.sql
-- Add Drive_Date, Application_Deadline, and Openings to PLACEMENT_DRIVE
-- Aligns Oracle database schema with PlacementDrive JPA entity & Frontend Drive Cards
-- ============================================================

-- 1. Add missing attributes to PLACEMENT_DRIVE
ALTER TABLE PLACEMENT_DRIVE ADD (
    Drive_Date           DATE,
    Application_Deadline DATE,
    Openings             NUMBER(5) DEFAULT 10
);

-- 2. Populate realistic drive schedules and openings for seeded drives
UPDATE PLACEMENT_DRIVE SET 
    Drive_Date = TO_DATE('2026-09-25', 'YYYY-MM-DD'),
    Application_Deadline = TO_DATE('2026-09-20', 'YYYY-MM-DD'),
    Openings = 15
WHERE Drive_Id = 'DRV001';

UPDATE PLACEMENT_DRIVE SET 
    Drive_Date = TO_DATE('2026-09-28', 'YYYY-MM-DD'),
    Application_Deadline = TO_DATE('2026-09-22', 'YYYY-MM-DD'),
    Openings = 50
WHERE Drive_Id = 'DRV002';

UPDATE PLACEMENT_DRIVE SET 
    Drive_Date = TO_DATE('2026-10-02', 'YYYY-MM-DD'),
    Application_Deadline = TO_DATE('2026-09-26', 'YYYY-MM-DD'),
    Openings = 25
WHERE Drive_Id = 'DRV003';

UPDATE PLACEMENT_DRIVE SET 
    Drive_Date = TO_DATE('2026-10-05', 'YYYY-MM-DD'),
    Application_Deadline = TO_DATE('2026-09-29', 'YYYY-MM-DD'),
    Openings = 20
WHERE Drive_Id = 'DRV004';

UPDATE PLACEMENT_DRIVE SET 
    Drive_Date = TO_DATE('2026-10-08', 'YYYY-MM-DD'),
    Application_Deadline = TO_DATE('2026-10-01', 'YYYY-MM-DD'),
    Openings = 12
WHERE Drive_Id = 'DRV005';

UPDATE PLACEMENT_DRIVE SET 
    Drive_Date = TO_DATE('2026-10-12', 'YYYY-MM-DD'),
    Application_Deadline = TO_DATE('2026-10-04', 'YYYY-MM-DD'),
    Openings = 10
WHERE Drive_Id = 'DRV006';

UPDATE PLACEMENT_DRIVE SET 
    Drive_Date = TO_DATE('2026-10-15', 'YYYY-MM-DD'),
    Application_Deadline = TO_DATE('2026-10-07', 'YYYY-MM-DD'),
    Openings = 18
WHERE Drive_Id = 'DRV007';

UPDATE PLACEMENT_DRIVE SET 
    Drive_Date = TO_DATE('2026-10-18', 'YYYY-MM-DD'),
    Application_Deadline = TO_DATE('2026-10-10', 'YYYY-MM-DD'),
    Openings = 15
WHERE Drive_Id = 'DRV008';

-- Fallback for any other drives
UPDATE PLACEMENT_DRIVE SET 
    Drive_Date = SYSDATE + 14,
    Application_Deadline = SYSDATE + 7,
    Openings = 10
WHERE Drive_Date IS NULL;

COMMIT;
