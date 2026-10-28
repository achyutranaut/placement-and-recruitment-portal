-- ============================================================
-- V28__align_analytics_and_offer_status.sql
-- Align Analytics Stored Functions with Multi-Offer States
-- Filters out DECLINED/WITHDRAWN offers from placement rate & average CTC
-- ============================================================

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
        WHERE r.Program_Id = p_program_id
          AND (ol.Status IS NULL OR UPPER(ol.Status) NOT IN ('DECLINED', 'WITHDRAWN'));
    ELSE
        -- Global placement rate across all registered students
        SELECT COUNT(DISTINCT Student_Id) INTO v_total_students FROM STUDENT;

        SELECT COUNT(DISTINCT sdr.Student_Id) INTO v_placed_students
        FROM OFFER_LETTER ol
        JOIN APPLICATION a ON ol.Application_Id = a.Application_Id
        JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date
        WHERE (ol.Status IS NULL OR UPPER(ol.Status) NOT IN ('DECLINED', 'WITHDRAWN'));
    END IF;

    IF v_total_students > 0 THEN
        v_rate := ROUND((v_placed_students / v_total_students) * 100, 2);
    END IF;

    RETURN v_rate;
END GET_PLACEMENT_RATE;
/

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
        WHERE UPPER(c.Industry) LIKE '%' || UPPER(p_industry) || '%'
          AND (ol.Status IS NULL OR UPPER(ol.Status) NOT IN ('DECLINED', 'WITHDRAWN'));
    ELSE
        SELECT NVL(ROUND(AVG(CTC_LPA), 2), 0) INTO v_avg_ctc
        FROM OFFER_LETTER
        WHERE (Status IS NULL OR UPPER(Status) NOT IN ('DECLINED', 'WITHDRAWN'));
    END IF;

    RETURN v_avg_ctc;
END GET_AVERAGE_PACKAGE;
/
