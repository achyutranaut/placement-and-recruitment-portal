-- ============================================================
-- V8__cursors.sql
-- PL/SQL Explicit Cursors, Parameterized Cursors, Cursor Attributes,
-- and Cursor FOR Loops in a Dedicated Reporting Package
-- ============================================================

CREATE OR REPLACE PACKAGE PKG_PLACEMENT_REPORTS AS
    PROCEDURE PROCESS_DRIVE_APPLICANTS(p_drive_id IN VARCHAR2);
    PROCEDURE GET_COMPANY_OFFER_BREAKDOWN(p_min_ctc IN NUMBER, p_out_cursor OUT SYS_REFCURSOR);
END PKG_PLACEMENT_REPORTS;
/

CREATE OR REPLACE PACKAGE BODY PKG_PLACEMENT_REPORTS AS

    -- Demonstrates Explicit Parameterized Cursor with %FOUND, %NOTFOUND, %ROWCOUNT
    PROCEDURE PROCESS_DRIVE_APPLICANTS(p_drive_id IN VARCHAR2) AS
        -- Explicit Parameterized Cursor
        CURSOR cur_applicants(cp_drive_id VARCHAR2) IS
            SELECT 
                a.Application_Id,
                s.Student_Id,
                s.Name,
                s.CGPA,
                a.Status
            FROM APPLICATION a
            JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr 
                ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date
            JOIN STUDENT s 
                ON sdr.Student_Id = s.Student_Id
            WHERE sdr.Drive_Id = cp_drive_id
            ORDER BY s.CGPA DESC;

        v_app_rec cur_applicants%ROWTYPE;
        v_processed_count NUMBER := 0;
    BEGIN
        -- Open explicit cursor
        OPEN cur_applicants(p_drive_id);

        LOOP
            FETCH cur_applicants INTO v_app_rec;
            EXIT WHEN cur_applicants%NOTFOUND;

            -- Demonstrate %ROWCOUNT
            v_processed_count := cur_applicants%ROWCOUNT;

            -- Update eligible applied candidates with CGPA >= 8.5 to SHORTLISTED automatically
            IF v_app_rec.Status = 'APPLIED' AND v_app_rec.CGPA >= 8.50 THEN
                UPDATE APPLICATION 
                SET Status = 'SHORTLISTED' 
                WHERE Application_Id = v_app_rec.Application_Id;
            END IF;
        END LOOP;

        -- Close explicit cursor
        IF cur_applicants%ISOPEN THEN
            CLOSE cur_applicants;
        END IF;

        COMMIT;
    END PROCESS_DRIVE_APPLICANTS;

    -- Demonstrates Cursor FOR Loop & SYS_REFCURSOR
    PROCEDURE GET_COMPANY_OFFER_BREAKDOWN(p_min_ctc IN NUMBER, p_out_cursor OUT SYS_REFCURSOR) AS
    BEGIN
        OPEN p_out_cursor FOR
            SELECT 
                ec.Company_Name,
                c.Industry,
                COUNT(ol.Offer_Id) AS Total_Offers,
                ROUND(AVG(ol.CTC_LPA), 2) AS Average_CTC,
                MAX(ol.CTC_LPA) AS Highest_CTC,
                MIN(ol.CTC_LPA) AS Lowest_CTC
            FROM OFFER_LETTER ol
            JOIN APPLICATION a ON ol.Application_Id = a.Application_Id
            JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date
            JOIN PLACEMENT_DRIVE d ON sdr.Drive_Id = d.Drive_Id
            JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
            JOIN COMPANY c ON jc.Company_Id = c.Company_Id
            JOIN EMAIL_COMPANY ec ON c.Email = ec.Email
            WHERE ol.CTC_LPA >= NVL(p_min_ctc, 0)
            GROUP BY ec.Company_Name, c.Industry
            ORDER BY Average_CTC DESC;
    END GET_COMPANY_OFFER_BREAKDOWN;

END PKG_PLACEMENT_REPORTS;
/
