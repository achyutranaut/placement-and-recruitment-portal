-- ============================================================
-- V10__demo_queries.sql
-- Comprehensive DQL Demonstration Queries for DA2 Evaluation
-- Includes Joins, Aggregation, GROUP BY, HAVING, Subqueries,
-- Correlated Subqueries, and Analytical Functions.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Multi-Table Join: Complete Candidate Placement Journey
-- ------------------------------------------------------------
SELECT 
    s.Student_Id,
    s.Name AS Student_Name,
    p.Program_Name,
    b.Batch_No,
    ec.Company_Name,
    d.Job_Title,
    a.Status AS Application_Status,
    ol.Offer_Id,
    ol.CTC_LPA
FROM STUDENT s
LEFT JOIN REGISTERS r ON s.Student_Id = r.Student_Id
LEFT JOIN PROGRAM p ON r.Program_Id = p.Program_Id
LEFT JOIN ATTENDS att ON s.Student_Id = att.Student_Id
LEFT JOIN BATCH b ON att.Batch_No = b.Batch_No
LEFT JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON s.Student_Id = sdr.Student_Id
LEFT JOIN APPLICATION a ON sdr.Student_Id = a.Student_Id AND sdr.Apply_Date = a.Apply_Date
LEFT JOIN PLACEMENT_DRIVE d ON sdr.Drive_Id = d.Drive_Id
LEFT JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
LEFT JOIN COMPANY c ON jc.Company_Id = c.Company_Id
LEFT JOIN EMAIL_COMPANY ec ON c.Email = ec.Email
LEFT JOIN OFFER_LETTER ol ON a.Application_Id = ol.Application_Id
ORDER BY s.Student_Id;

-- ------------------------------------------------------------
-- 2. Aggregation with GROUP BY and HAVING: Top Hiring Companies
-- ------------------------------------------------------------
SELECT 
    ec.Company_Name,
    c.Industry,
    COUNT(ol.Offer_Id) AS Total_Offers_Extended,
    ROUND(AVG(ol.CTC_LPA), 2) AS Average_CTC,
    MAX(ol.CTC_LPA) AS Peak_CTC
FROM COMPANY c
JOIN EMAIL_COMPANY ec ON c.Email = ec.Email
JOIN JOB_COMPANY jc ON c.Company_Id = jc.Company_Id
JOIN PLACEMENT_DRIVE d ON jc.Job_Title = d.Job_Title
JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON d.Drive_Id = sdr.Drive_Id
JOIN APPLICATION a ON sdr.Student_Id = a.Student_Id AND sdr.Apply_Date = a.Apply_Date
JOIN OFFER_LETTER ol ON a.Application_Id = ol.Application_Id
GROUP BY ec.Company_Name, c.Industry
HAVING COUNT(ol.Offer_Id) >= 1
ORDER BY Average_CTC DESC;

-- ------------------------------------------------------------
-- 3. Nested Subquery: Students with Offers Above Average CTC
-- ------------------------------------------------------------
SELECT 
    s.Student_Id,
    s.Name,
    s.Email,
    s.CGPA,
    ol.CTC_LPA
FROM STUDENT s
JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON s.Student_Id = sdr.Student_Id
JOIN APPLICATION a ON sdr.Student_Id = a.Student_Id AND sdr.Apply_Date = a.Apply_Date
JOIN OFFER_LETTER ol ON a.Application_Id = ol.Application_Id
WHERE ol.CTC_LPA > (SELECT AVG(CTC_LPA) FROM OFFER_LETTER)
ORDER BY ol.CTC_LPA DESC;

-- ------------------------------------------------------------
-- 4. Correlated Subquery: Students whose CGPA is Above Batch Average
-- ------------------------------------------------------------
SELECT 
    s.Student_Id,
    s.Name,
    s.CGPA,
    att.Batch_No
FROM STUDENT s
JOIN ATTENDS att ON s.Student_Id = att.Student_Id
WHERE s.CGPA > (
    SELECT AVG(s2.CGPA)
    FROM STUDENT s2
    JOIN ATTENDS att2 ON s2.Student_Id = att2.Student_Id
    WHERE att2.Batch_No = att.Batch_No
)
ORDER BY att.Batch_No, s.CGPA DESC;

-- ------------------------------------------------------------
-- 5. Analytical Ranking Function (DENSE_RANK) over Package
-- ------------------------------------------------------------
SELECT 
    s.Student_Id,
    s.Name AS Student_Name,
    ec.Company_Name,
    ol.CTC_LPA,
    DENSE_RANK() OVER (ORDER BY ol.CTC_LPA DESC) AS Salary_Rank
FROM OFFER_LETTER ol
JOIN APPLICATION a ON ol.Application_Id = a.Application_Id
JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date
JOIN STUDENT s ON sdr.Student_Id = s.Student_Id
JOIN PLACEMENT_DRIVE d ON sdr.Drive_Id = d.Drive_Id
JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
JOIN COMPANY c ON jc.Company_Id = c.Company_Id
JOIN EMAIL_COMPANY ec ON c.Email = ec.Email;
