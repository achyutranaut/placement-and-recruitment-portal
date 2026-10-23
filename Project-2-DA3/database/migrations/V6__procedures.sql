-- ============================================================
-- V6__procedures.sql
-- PL/SQL Stored Procedures for Core Placement Transactions
-- Includes TCL (SAVEPOINT, COMMIT, ROLLBACK TO SAVEPOINT),
-- explicit cursors, and custom exception handling.
-- ============================================================

-- ------------------------------------------------------------
-- 1. PROCEDURE: REGISTER_STUDENT_PROGRAM
-- ------------------------------------------------------------
CREATE OR REPLACE PROCEDURE REGISTER_STUDENT_PROGRAM (
    p_student_id IN VARCHAR2,
    p_program_id IN VARCHAR2,
    p_reg_date   IN DATE DEFAULT SYSDATE
) AS
    v_student_count NUMBER;
    v_program_count NUMBER;
    v_already_reg   NUMBER;

    -- User-defined exceptions
    ex_student_not_found EXCEPTION;
    ex_program_not_found EXCEPTION;
    ex_duplicate_reg     EXCEPTION;
    PRAGMA EXCEPTION_INIT(ex_duplicate_reg, -20001);
    PRAGMA EXCEPTION_INIT(ex_student_not_found, -20002);
    PRAGMA EXCEPTION_INIT(ex_program_not_found, -20003);
BEGIN
    SAVEPOINT sp_register;

    -- Validate Student
    SELECT COUNT(*) INTO v_student_count FROM STUDENT WHERE Student_Id = p_student_id;
    IF v_student_count = 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Student ID ' || p_student_id || ' does not exist.');
    END IF;

    -- Validate Program
    SELECT COUNT(*) INTO v_program_count FROM PROGRAM WHERE Program_Id = p_program_id;
    IF v_program_count = 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'Program ID ' || p_program_id || ' does not exist.');
    END IF;

    -- Check Existing Registration
    SELECT COUNT(*) INTO v_already_reg 
    FROM REGISTERS 
    WHERE Student_Id = p_student_id AND Program_Id = p_program_id;

    IF v_already_reg > 0 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Student is already enrolled in this training program.');
    END IF;

    -- Execute Registration
    INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date)
    VALUES (p_student_id, p_program_id, NVL(p_reg_date, SYSDATE));

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO SAVEPOINT sp_register;
        RAISE;
END REGISTER_STUDENT_PROGRAM;
/

-- ------------------------------------------------------------
-- 2. PROCEDURE: APPLY_FOR_DRIVE
-- ------------------------------------------------------------
CREATE OR REPLACE PROCEDURE APPLY_FOR_DRIVE (
    p_student_id     IN VARCHAR2,
    p_drive_id       IN VARCHAR2,
    p_apply_date     IN DATE DEFAULT SYSDATE,
    o_application_id OUT VARCHAR2
) AS
    v_student_cgpa   NUMBER(4,2);
    v_min_cgpa       NUMBER(4,2);
    v_daily_conflict NUMBER;
    v_next_num       NUMBER;
    v_target_date    DATE := TRUNC(NVL(p_apply_date, SYSDATE));

    ex_ineligible_cgpa EXCEPTION;
    ex_daily_limit     EXCEPTION;
    PRAGMA EXCEPTION_INIT(ex_ineligible_cgpa, -20010);
    PRAGMA EXCEPTION_INIT(ex_daily_limit, -20011);
BEGIN
    SAVEPOINT sp_apply_drive;

    -- Verify CGPA Eligibility
    SELECT CGPA INTO v_student_cgpa FROM STUDENT WHERE Student_Id = p_student_id;
    SELECT Min_CGPA INTO v_min_cgpa FROM PLACEMENT_DRIVE WHERE Drive_Id = p_drive_id;

    IF v_student_cgpa < v_min_cgpa THEN
        RAISE_APPLICATION_ERROR(-20010, 'Ineligible: Student CGPA (' || v_student_cgpa || 
                                ') is below minimum required CGPA (' || v_min_cgpa || ').');
    END IF;

    -- Enforce Daily Routine Limit (At most one drive per calendar day)
    SELECT COUNT(*) INTO v_daily_conflict
    FROM STUDENT_DAILY_ROUTINE_DRIVE
    WHERE Student_Id = p_student_id AND TRUNC(Apply_Date) = v_target_date;

    IF v_daily_conflict > 0 THEN
        RAISE_APPLICATION_ERROR(-20011, 'Daily limit reached: A student may apply to at most one drive per day.');
    END IF;

    -- Generate Unique Application ID
    SELECT NVL(MAX(TO_NUMBER(REGEXP_SUBSTR(Application_Id, '[0-9]+'))), 0) + 1 
    INTO v_next_num 
    FROM APPLICATION;

    o_application_id := 'APP' || LPAD(v_next_num, 3, '0');

    -- Insert into Routine Relation
    INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id)
    VALUES (p_student_id, v_target_date, p_drive_id);

    -- Insert into Application
    INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status)
    VALUES (o_application_id, p_student_id, v_target_date, 'APPLIED');

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO SAVEPOINT sp_apply_drive;
        RAISE;
END APPLY_FOR_DRIVE;
/

-- ------------------------------------------------------------
-- 3. PROCEDURE: SCHEDULE_INTERVIEW
-- ------------------------------------------------------------
CREATE OR REPLACE PROCEDURE SCHEDULE_INTERVIEW (
    p_application_id   IN VARCHAR2,
    p_interviewer_name IN VARCHAR2,
    p_online           IN CHAR DEFAULT 'Y',
    p_offline          IN CHAR DEFAULT 'N'
) AS
    v_app_status VARCHAR2(30);
    v_round_cnt  NUMBER;
BEGIN
    SAVEPOINT sp_interview;

    -- Check Application Status
    SELECT Status INTO v_app_status 
    FROM APPLICATION 
    WHERE Application_Id = p_application_id;

    IF v_app_status NOT IN ('APPLIED', 'SHORTLISTED', 'INTERVIEWING') THEN
        RAISE_APPLICATION_ERROR(-20020, 'Application status ' || v_app_status || ' cannot be scheduled for interview.');
    END IF;

    -- Verify Interviewer
    SELECT COUNT(*) INTO v_round_cnt 
    FROM INTERVIEWER_ROUND 
    WHERE Interviewer_Name = p_interviewer_name;

    IF v_round_cnt = 0 THEN
        RAISE_APPLICATION_ERROR(-20021, 'Interviewer ' || p_interviewer_name || ' not found in round assignments.');
    END IF;

    -- Insert Interview Record
    INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
    VALUES (p_application_id, p_interviewer_name, NULL, NULL, NULL, 'PENDING', NVL(p_online, 'Y'), NVL(p_offline, 'N'));

    -- Update Application Status to INTERVIEWING
    UPDATE APPLICATION 
    SET Status = 'INTERVIEWING' 
    WHERE Application_Id = p_application_id;

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO SAVEPOINT sp_interview;
        RAISE;
END SCHEDULE_INTERVIEW;
/

-- ------------------------------------------------------------
-- 4. PROCEDURE: ISSUE_OFFER
-- ------------------------------------------------------------
CREATE OR REPLACE PROCEDURE ISSUE_OFFER (
    p_application_id IN VARCHAR2,
    p_ctc_lpa        IN NUMBER,
    p_offer_date     IN DATE DEFAULT SYSDATE,
    o_offer_id       OUT VARCHAR2
) AS
    v_cleared_cnt NUMBER;
    v_existing    NUMBER;
    v_next_num    NUMBER;
BEGIN
    SAVEPOINT sp_issue_offer;

    -- Verify Interview Cleared
    SELECT COUNT(*) INTO v_cleared_cnt 
    FROM INTERVIEW 
    WHERE Application_Id = p_application_id AND Result = 'CLEARED';

    IF v_cleared_cnt = 0 THEN
        RAISE_APPLICATION_ERROR(-20030, 'Cannot issue offer: Application has no CLEARED interview results.');
    END IF;

    -- Check if Offer already exists
    SELECT COUNT(*) INTO v_existing 
    FROM OFFER_LETTER 
    WHERE Application_Id = p_application_id;

    IF v_existing > 0 THEN
        RAISE_APPLICATION_ERROR(-20031, 'An offer has already been issued for this application.');
    END IF;

    -- Generate Next Offer ID
    SELECT NVL(MAX(TO_NUMBER(REGEXP_SUBSTR(Offer_Id, '[0-9]+'))), 0) + 1 
    INTO v_next_num 
    FROM OFFER_LETTER;

    o_offer_id := 'OFF' || LPAD(v_next_num, 3, '0');

    -- Insert Offer Letter (Triggers will automatically set APPLICATION.Status to 'OFFERED')
    INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA, Status)
    VALUES (o_offer_id, p_application_id, NVL(p_offer_date, SYSDATE), p_ctc_lpa, 'OFFERED');

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO SAVEPOINT sp_issue_offer;
        RAISE;
END ISSUE_OFFER;
/
