-- ============================================================
-- V7__functions.sql
-- PL/SQL Stored Functions for Analytics, Placement Rates,
-- and SYS_REFCURSOR Queries
-- ============================================================

-- ------------------------------------------------------------
-- 1. FUNCTION: GET_STUDENT_APPLICATION_COUNT
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION GET_STUDENT_APPLICATION_COUNT (
    p_student_id IN VARCHAR2
) RETURN NUMBER AS
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM APPLICATION
    WHERE Student_Id = p_student_id;

    RETURN v_count;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN 0;
END GET_STUDENT_APPLICATION_COUNT;
/

-- ------------------------------------------------------------
-- 2. FUNCTION: GET_PLACEMENT_RATE
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION GET_PLACEMENT_RATE (
    p_program_id IN VARCHAR2 DEFAULT NULL
) RETURN NUMBER AS
    v_total_students  NUMBER := 0;
    v_placed_students NUMBER := 0;
    v_rate            NUMBER(5,2) := 0.0;
BEGIN
    IF p_program_id IS NOT NULL THEN
        -- Program-specific placement rate
        SELECT COUNT(DISTINCT r.Student_Id) INTO v_total_students
        FROM REGISTERS r
        WHERE r.Program_Id = p_program_id;

        SELECT COUNT(DISTINCT sdr.Student_Id) INTO v_placed_students
        FROM OFFER_LETTER ol
        JOIN APPLICATION a ON ol.Application_Id = a.Application_Id
        JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date
        JOIN REGISTERS r ON sdr.Student_Id = r.Student_Id
        WHERE r.Program_Id = p_program_id;
    ELSE
        -- Global placement rate across all registered students
        SELECT COUNT(DISTINCT Student_Id) INTO v_total_students FROM STUDENT;

        SELECT COUNT(DISTINCT sdr.Student_Id) INTO v_placed_students
        FROM OFFER_LETTER ol
        JOIN APPLICATION a ON ol.Application_Id = a.Application_Id
        JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date;
    END IF;

    IF v_total_students > 0 THEN
        v_rate := ROUND((v_placed_students / v_total_students) * 100, 2);
    END IF;

    RETURN v_rate;
END GET_PLACEMENT_RATE;
/

-- ------------------------------------------------------------
-- 3. FUNCTION: GET_AVERAGE_PACKAGE
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION GET_AVERAGE_PACKAGE (
    p_industry IN VARCHAR2 DEFAULT NULL
) RETURN NUMBER AS
    v_avg_ctc NUMBER(6,2) := 0.0;
BEGIN
    IF p_industry IS NOT NULL THEN
        SELECT NVL(ROUND(AVG(ol.CTC_LPA), 2), 0) INTO v_avg_ctc
        FROM OFFER_LETTER ol
        JOIN APPLICATION a ON ol.Application_Id = a.Application_Id
        JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date
        JOIN PLACEMENT_DRIVE d ON sdr.Drive_Id = d.Drive_Id
        JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
        JOIN COMPANY c ON jc.Company_Id = c.Company_Id
        WHERE UPPER(c.Industry) LIKE '%' || UPPER(p_industry) || '%';
    ELSE
        SELECT NVL(ROUND(AVG(CTC_LPA), 2), 0) INTO v_avg_ctc
        FROM OFFER_LETTER;
    END IF;

    RETURN v_avg_ctc;
END GET_AVERAGE_PACKAGE;
/

-- ------------------------------------------------------------
-- 4. FUNCTION: GET_ELIGIBLE_STUDENTS (SYS_REFCURSOR)
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION GET_ELIGIBLE_STUDENTS (
    p_drive_id IN VARCHAR2
) RETURN SYS_REFCURSOR AS
    v_cursor   SYS_REFCURSOR;
    v_min_cgpa NUMBER(4,2);
BEGIN
    SELECT Min_CGPA INTO v_min_cgpa 
    FROM PLACEMENT_DRIVE 
    WHERE Drive_Id = p_drive_id;

    OPEN v_cursor FOR
        SELECT 
            s.Student_Id,
            s.Name,
            s.Email,
            s.City,
            s.CGPA
        FROM STUDENT s
        WHERE s.CGPA >= v_min_cgpa
          AND s.Student_Id NOT IN (
              SELECT sdr.Student_Id 
              FROM STUDENT_DAILY_ROUTINE_DRIVE sdr 
              WHERE sdr.Drive_Id = p_drive_id
          )
        ORDER BY s.CGPA DESC;

    RETURN v_cursor;
END GET_ELIGIBLE_STUDENTS;
/
