-- ============================================================
-- V26__enforce_placement_drive_not_null.sql
-- Enforce NOT NULL on mandatory PLACEMENT_DRIVE columns:
-- DRIVE_DATE, APPLICATION_DEADLINE, OPENINGS
-- Rationale: A placement drive must have a scheduled date,
-- an application deadline, and a known number of openings.
-- All existing rows already have non-NULL values (verified pre-migration).
-- ============================================================

ALTER TABLE PLACEMENT_DRIVE MODIFY (
    Drive_Date           NOT NULL,
    Application_Deadline NOT NULL,
    Openings             NOT NULL
);

COMMIT;
