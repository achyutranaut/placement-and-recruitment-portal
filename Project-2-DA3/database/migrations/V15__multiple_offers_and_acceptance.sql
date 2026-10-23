-- ============================================================
-- V15__multiple_offers_and_acceptance.sql
-- Multiple Offer Selection, Offer Acceptance Tracking,
-- Student Placement Status, and Canonical APPLICATION.Drive_Id Relation
-- ============================================================

-- 1. Add canonical Drive_Id to APPLICATION table
ALTER TABLE APPLICATION ADD (
    Drive_Id VARCHAR2(20)
);

-- 2. Backfill existing APPLICATION records from STUDENT_DAILY_ROUTINE_DRIVE
UPDATE APPLICATION a
SET a.Drive_Id = (
    SELECT sdr.Drive_Id
    FROM STUDENT_DAILY_ROUTINE_DRIVE sdr
    WHERE sdr.Student_Id = a.Student_Id 
      AND TRUNC(sdr.Apply_Date) = TRUNC(a.Apply_Date)
      AND ROWNUM = 1
)
WHERE a.Drive_Id IS NULL;

-- 3. Add Foreign Key for APPLICATION.Drive_Id -> PLACEMENT_DRIVE.Drive_Id
ALTER TABLE APPLICATION ADD CONSTRAINT FK_APP_DRIVE
    FOREIGN KEY (Drive_Id) REFERENCES PLACEMENT_DRIVE (Drive_Id) ON DELETE SET NULL;

-- 4. Add Status and Accepted_At to OFFER_LETTER if not already present
DECLARE
    v_cnt NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_cnt FROM USER_TAB_COLS WHERE TABLE_NAME = 'OFFER_LETTER' AND COLUMN_NAME = 'STATUS';
    IF v_cnt = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE OFFER_LETTER ADD (Status VARCHAR2(20) DEFAULT ''OFFERED'' NOT NULL)';
    END IF;
    SELECT COUNT(*) INTO v_cnt FROM USER_TAB_COLS WHERE TABLE_NAME = 'OFFER_LETTER' AND COLUMN_NAME = 'ACCEPTED_AT';
    IF v_cnt = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE OFFER_LETTER ADD (Accepted_At TIMESTAMP)';
    END IF;
END;
/

-- 5. Add Check Constraint on OFFER_LETTER.Status
DECLARE
    v_cnt NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_cnt FROM USER_CONSTRAINTS WHERE CONSTRAINT_NAME = 'CHK_OFFER_STATUS';
    IF v_cnt = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE OFFER_LETTER ADD CONSTRAINT CHK_OFFER_STATUS CHECK (Status IN (''OFFERED'', ''ACCEPTED'', ''DECLINED'', ''WITHDRAWN''))';
    END IF;
END;
/

-- 6. Expand APPLICATION.Status check constraint to support ACCEPTED and DECLINED
ALTER TABLE APPLICATION DROP CONSTRAINT CHK_APP_STATUS;
ALTER TABLE APPLICATION ADD CONSTRAINT CHK_APP_STATUS
    CHECK (Status IN ('APPLIED', 'SHORTLISTED', 'INTERVIEWING', 'SELECTED', 'OFFERED', 'ACCEPTED', 'DECLINED', 'REJECTED'));

-- 7. Add Placement_Status to STUDENT
ALTER TABLE STUDENT ADD (
    Placement_Status VARCHAR2(20) DEFAULT 'NOT_PLACED' NOT NULL
);

ALTER TABLE STUDENT ADD CONSTRAINT CHK_STUDENT_PLACEMENT_STATUS
    CHECK (Placement_Status IN ('NOT_PLACED', 'OFFERED', 'PLACED'));

-- 8. Synchronize initial Placement_Status for students who have received offers
UPDATE STUDENT s
SET s.Placement_Status = 'OFFERED'
WHERE s.Student_Id IN (
    SELECT a.Student_Id
    FROM APPLICATION a
    JOIN OFFER_LETTER o ON a.Application_Id = o.Application_Id
);
