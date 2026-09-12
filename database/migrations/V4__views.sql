-- ============================================================
-- V4__views.sql
-- Database Views for Reporting, Analytics, and Modular Monolith
-- (Updated per Locked Decision 1: Program-Scoped Batch_No)
-- ============================================================

-- 1. Detailed Student Applications View
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
JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr 
    ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date
JOIN STUDENT s 
    ON sdr.Student_Id = s.Student_Id
JOIN PLACEMENT_DRIVE d 
    ON sdr.Drive_Id = d.Drive_Id
JOIN JOB_COMPANY jc 
    ON d.Job_Title = jc.Job_Title
JOIN COMPANY c 
    ON jc.Company_Id = c.Company_Id
JOIN EMAIL_COMPANY ec 
    ON c.Email = ec.Email;

-- 2. Placement Drives Overview & Aggregate Metrics
CREATE OR REPLACE VIEW V_PLACEMENT_DRIVES_OVERVIEW AS
SELECT 
    d.Drive_Id,
    d.Job_Title,
    c.Company_Id,
    ec.Company_Name,
    c.Industry,
    d.Min_CGPA,
    COUNT(a.Application_Id) AS Total_Applicants,
    NVL(SUM(CASE WHEN a.Status = 'SHORTLISTED' THEN 1 ELSE 0 END), 0) AS Shortlisted_Count,
    NVL(SUM(CASE WHEN a.Status IN ('SELECTED', 'OFFERED') THEN 1 ELSE 0 END), 0) AS Selected_Count
FROM PLACEMENT_DRIVE d
JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
JOIN COMPANY c ON jc.Company_Id = c.Company_Id
JOIN EMAIL_COMPANY ec ON c.Email = ec.Email
LEFT JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON d.Drive_Id = sdr.Drive_Id
LEFT JOIN APPLICATION a ON sdr.Student_Id = a.Student_Id AND sdr.Apply_Date = a.Apply_Date
GROUP BY 
    d.Drive_Id, d.Job_Title, c.Company_Id, ec.Company_Name, c.Industry, d.Min_CGPA;

-- 3. Offer Summary View
CREATE OR REPLACE VIEW V_OFFER_SUMMARY AS
SELECT 
    ol.Offer_Id,
    ol.Offer_Date,
    ol.CTC_LPA,
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
JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date
JOIN STUDENT s ON sdr.Student_Id = s.Student_Id
JOIN PLACEMENT_DRIVE d ON sdr.Drive_Id = d.Drive_Id
JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
JOIN COMPANY c ON jc.Company_Id = c.Company_Id
JOIN EMAIL_COMPANY ec ON c.Email = ec.Email;

-- 4. Student Training & Assessment View
CREATE OR REPLACE VIEW V_STUDENT_TRAINING_PROFILE AS
SELECT 
    s.Student_Id,
    s.Name AS Student_Name,
    p.Program_Id,
    p.Program_Name,
    p.Fee,
    b.Batch_No,
    b.Schedule,
    b.Room,
    b.Start_Date,
    b.End_Date
FROM STUDENT s
JOIN REGISTERS r ON s.Student_Id = r.Student_Id
JOIN PROGRAM p ON r.Program_Id = p.Program_Id
JOIN ATTENDS att ON s.Student_Id = att.Student_Id AND p.Program_Id = att.Program_Id
JOIN BATCH b ON att.Program_Id = b.Program_Id AND att.Batch_No = b.Batch_No;

-- 5. Comprehensive Interview Schedule View
CREATE OR REPLACE VIEW V_INTERVIEW_SCHEDULE AS
SELECT 
    i.Application_Id,
    s.Student_Id,
    s.Name AS Student_Name,
    ir.Interviewer_Name,
    ir.Interview_Round_No,
    i.OA,
    i.GD,
    i.HR,
    i.Result AS Interview_Result,
    i."ONLINE",
    i."OFFLINE",
    d.Drive_Id,
    d.Job_Title,
    ec.Company_Name
FROM INTERVIEW i
JOIN INTERVIEWER_ROUND ir ON i.Interviewer_Name = ir.Interviewer_Name
JOIN APPLICATION a ON i.Application_Id = a.Application_Id
JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date
JOIN STUDENT s ON sdr.Student_Id = s.Student_Id
JOIN PLACEMENT_DRIVE d ON sdr.Drive_Id = d.Drive_Id
JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
JOIN COMPANY c ON jc.Company_Id = c.Company_Id
JOIN EMAIL_COMPANY ec ON c.Email = ec.Email;
