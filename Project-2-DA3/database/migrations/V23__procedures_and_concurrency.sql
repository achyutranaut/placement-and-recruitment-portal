-- ============================================================
-- V23__procedures_and_concurrency.sql
-- 1. Decouple PL/SQL transaction boundaries: Remove internal COMMIT;
--    statements so Spring @Transactional owns commit/rollback boundaries.
-- 2. Align Views V_STUDENT_APPLICATIONS and V_OFFER_SUMMARY with
--    canonical APPLICATION.Drive_Id foreign key relation.
-- 3. Enforce single accepted offer mutex compound trigger on OFFER_LETTER.
-- ============================================================

-- ------------------------------------------------------------
-- 1. PROCEDURE: REGISTER_STUDENT_PROGRAM (Decoupled TCL)
-- ------------------------------------------------------------
CREATE OR REPLACE PROCEDURE REGISTER_STUDENT_PROGRAM (
    p_student_id IN VARCHAR2,
    p_program_id IN VARCHAR2,
    p_reg_date   IN DATE DEFAULT SYSDATE
) AS
    v_student_count NUMBER;
    v_program_count NUMBER;
    v_already_reg   NUMBER;

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

    -- Note: COMMIT removed to honor caller / Spring @Transactional boundary
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO SAVEPOINT sp_register;
        RAISE;
END REGISTER_STUDENT_PROGRAM;
/

-- ------------------------------------------------------------
-- 2. PROCEDURE: APPLY_FOR_DRIVE (Decoupled TCL & Canonical Drive_Id)
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

    -- Insert into Application with canonical Drive_Id
    INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status, Drive_Id)
    VALUES (o_application_id, p_student_id, v_target_date, 'APPLIED', p_drive_id);

    -- Note: COMMIT removed to honor caller / Spring @Transactional boundary
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO SAVEPOINT sp_apply_drive;
        RAISE;
END APPLY_FOR_DRIVE;
/

-- ------------------------------------------------------------
-- 3. PROCEDURE: SCHEDULE_INTERVIEW (Decoupled TCL)
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

    -- Note: COMMIT removed to honor caller / Spring @Transactional boundary
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO SAVEPOINT sp_interview;
        RAISE;
END SCHEDULE_INTERVIEW;
/

-- ------------------------------------------------------------
-- 4. PROCEDURE: ISSUE_OFFER (Decoupled TCL)
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

    -- Note: COMMIT removed to honor caller / Spring @Transactional boundary
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO SAVEPOINT sp_issue_offer;
        RAISE;
END ISSUE_OFFER;
/

-- ------------------------------------------------------------
-- 5. CANONICAL VIEW: V_STUDENT_APPLICATIONS
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW V_STUDENT_APPLICATIONS AS
SELECT 
    a.Application_Id,
    s.Student_Id,
    s.Name AS Student_Name,
    s.Email AS Student_Email,
    s.CGPA AS Student_CGPA,
    a.Apply_Date,
    a.Status AS Application_Status,
    d.Drive_Id,
    d.Job_Title,
    c.Company_Id,
    ec.Company_Name,
    c.Industry,
    d.Min_CGPA
FROM APPLICATION a
JOIN STUDENT s 
    ON a.Student_Id = s.Student_Id
JOIN PLACEMENT_DRIVE d 
    ON a.Drive_Id = d.Drive_Id
JOIN JOB_COMPANY jc 
    ON d.Job_Title = jc.Job_Title
JOIN COMPANY c 
    ON jc.Company_Id = c.Company_Id
JOIN EMAIL_COMPANY ec 
    ON c.Email = ec.Email;

-- ------------------------------------------------------------
-- 6. CANONICAL VIEW: V_OFFER_SUMMARY
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW V_OFFER_SUMMARY AS
SELECT 
    ol.Offer_Id,
    ol.Offer_Date,
    ol.CTC_LPA,
    ol.Status AS Offer_Status,
    a.Application_Id,
    s.Student_Id,
    s.Name AS Student_Name,
    s.Email AS Student_Email,
    d.Drive_Id,
    d.Job_Title,
    ec.Company_Name,
    c.Industry
FROM OFFER_LETTER ol
JOIN APPLICATION a ON ol.Application_Id = a.Application_Id
JOIN STUDENT s ON a.Student_Id = s.Student_Id
LEFT JOIN PLACEMENT_DRIVE d ON a.Drive_Id = d.Drive_Id
LEFT JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
LEFT JOIN COMPANY c ON jc.Company_Id = c.Company_Id
LEFT JOIN EMAIL_COMPANY ec ON c.Email = ec.Email;

-- ------------------------------------------------------------
-- 7. ORACLE COMPOUND TRIGGER: TRG_SINGLE_ACCEPTED_OFFER
--    Guarantees at most one accepted offer per student.
-- ------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_SINGLE_ACCEPTED_OFFER
FOR INSERT OR UPDATE OF Status ON OFFER_LETTER
COMPOUND TRIGGER

    TYPE t_offer_rec IS RECORD (
        offer_id VARCHAR2(20),
        app_id   VARCHAR2(20)
    );
    TYPE t_offer_list IS TABLE OF t_offer_rec INDEX BY PLS_INTEGER;
    g_offers t_offer_list;

    BEFORE STATEMENT IS
    BEGIN
        g_offers.DELETE;
    END BEFORE STATEMENT;

    AFTER EACH ROW IS
    BEGIN
        IF :NEW.Status = 'ACCEPTED' THEN
            DECLARE
                v_idx PLS_INTEGER := g_offers.COUNT + 1;
            BEGIN
                g_offers(v_idx).offer_id := :NEW.Offer_Id;
                g_offers(v_idx).app_id   := :NEW.Application_Id;
            END;
        END IF;
    END AFTER EACH ROW;

    AFTER STATEMENT IS
        v_student_id   VARCHAR2(20);
        v_accepted_cnt NUMBER;
    BEGIN
        FOR i IN 1 .. g_offers.COUNT LOOP
            SELECT Student_Id INTO v_student_id
            FROM APPLICATION
            WHERE Application_Id = g_offers(i).app_id;

            SELECT COUNT(*) INTO v_accepted_cnt
            FROM OFFER_LETTER ol
            JOIN APPLICATION a ON ol.Application_Id = a.Application_Id
            WHERE a.Student_Id = v_student_id
              AND ol.Status = 'ACCEPTED'
              AND ol.Offer_Id != g_offers(i).offer_id;

            IF v_accepted_cnt > 0 THEN
                RAISE_APPLICATION_ERROR(-20040, 'Single Offer Mutex Violation: Student ' || v_student_id || ' already has an accepted employment offer.');
            END IF;
        END LOOP;
    END AFTER STATEMENT;
END TRG_SINGLE_ACCEPTED_OFFER;
/
