-- ============================================================
-- V9__triggers.sql
-- Database Triggers for Automated Status Synchronization,
-- Audit Logging, and Mutex Integrity Constraints
-- ============================================================

-- ------------------------------------------------------------
-- 1. TRIGGER: TRG_OFFER_APPLICATION_STATUS
-- Automatically marks Application as SELECTED when Offer is issued
-- ------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_OFFER_APPLICATION_STATUS
AFTER INSERT ON OFFER_LETTER
FOR EACH ROW
BEGIN
    UPDATE APPLICATION
    SET Status = 'OFFERED'
    WHERE Application_Id = :NEW.Application_Id;
END TRG_OFFER_APPLICATION_STATUS;
/

-- ------------------------------------------------------------
-- 2. TRIGGER: TRG_APPLICATION_AUDIT
-- Tracks full audit trail of Application status changes
-- ------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_APPLICATION_AUDIT
AFTER UPDATE OF Status ON APPLICATION
FOR EACH ROW
WHEN (OLD.Status IS NULL OR OLD.Status <> NEW.Status)
BEGIN
    INSERT INTO APPLICATION_AUDIT (
        Application_Id,
        Old_Status,
        New_Status,
        Changed_At,
        Changed_By
    ) VALUES (
        :NEW.Application_Id,
        :OLD.Status,
        :NEW.Status,
        CURRENT_TIMESTAMP,
        NVL(SYS_CONTEXT('USERENV', 'SESSION_USER'), 'SYSTEM')
    );
END TRG_APPLICATION_AUDIT;
/

-- ------------------------------------------------------------
-- 3. TRIGGER: TRG_PROGRAM_LEVEL_MUTEX
-- Ensures exactly one difficulty flag is 'Y'
-- ------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_PROGRAM_LEVEL_MUTEX
BEFORE INSERT OR UPDATE ON PROGRAM
FOR EACH ROW
DECLARE
    v_flags_sum NUMBER;
BEGIN
    v_flags_sum := (CASE WHEN :NEW.Beginner = 'Y' THEN 1 ELSE 0 END) +
                   (CASE WHEN :NEW.Intermediate = 'Y' THEN 1 ELSE 0 END) +
                   (CASE WHEN :NEW.Advanced = 'Y' THEN 1 ELSE 0 END);

    IF v_flags_sum <> 1 THEN
        RAISE_APPLICATION_ERROR(-20040, 'Integrity Error: Exactly one level flag (Beginner, Intermediate, Advanced) must be Y.');
    END IF;
END TRG_PROGRAM_LEVEL_MUTEX;
/

-- ------------------------------------------------------------
-- 4. TRIGGER: TRG_ASSESSMENT_LEVEL_MUTEX
-- Ensures exactly one assessment difficulty flag is 'Y'
-- ------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_ASSESSMENT_LEVEL_MUTEX
BEFORE INSERT OR UPDATE ON ASSESSMENT
FOR EACH ROW
DECLARE
    v_flags_sum NUMBER;
BEGIN
    v_flags_sum := (CASE WHEN :NEW.Beginner = 'Y' THEN 1 ELSE 0 END) +
                   (CASE WHEN :NEW.Intermediate = 'Y' THEN 1 ELSE 0 END) +
                   (CASE WHEN :NEW.Advanced = 'Y' THEN 1 ELSE 0 END);

    IF v_flags_sum <> 1 THEN
        RAISE_APPLICATION_ERROR(-20041, 'Integrity Error: Exactly one assessment level flag must be Y.');
    END IF;
END TRG_ASSESSMENT_LEVEL_MUTEX;
/

-- ------------------------------------------------------------
-- 5. TRIGGER: TRG_ENFORCE_MIN_CGPA
-- Guard against lowering placement drive Min_CGPA below zero or >10
-- ------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_ENFORCE_MIN_CGPA
BEFORE INSERT OR UPDATE ON PLACEMENT_DRIVE
FOR EACH ROW
BEGIN
    IF :NEW.Min_CGPA < 0.00 OR :NEW.Min_CGPA > 10.00 THEN
        RAISE_APPLICATION_ERROR(-20042, 'Invalid Min_CGPA: Value must be between 0.00 and 10.00.');
    END IF;
END TRG_ENFORCE_MIN_CGPA;
/
