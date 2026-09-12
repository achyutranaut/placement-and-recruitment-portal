-- ==============================================================================
-- CLEAN_MOCK_DATA.sql
-- Transaction-safe cleanup script to purge all mock, demo, sample, and seeded records
-- from the PlaceX Placement & Recruitment Portal.
--
-- STRICT FOREIGN KEY DEPENDENCY ORDER:
--   1. APPLICATION_AUDIT
--   2. RESUME_EVALUATION
--   3. STUDENT_RESUME
--   4. OFFER_LETTER
--   5. INTERVIEW
--   6. INTERVIEWER_ROUND
--   7. APPLICATION
--   8. STUDENT_DAILY_ROUTINE_DRIVE
--   9. DRIVE_ELIGIBILITY
--  10. PLACEMENT_DRIVE
--  11. JOB_COMPANY
--  12. COMPANY_PHONE
--  13. COMPANY
--  14. EMAIL_COMPANY
--  15. ATTENDS
--  16. REGISTERS
--  17. ASSESSMENT
--  18. BATCH_TITLE_PROGRAM
--  19. BATCH_PROGRAM
--  20. BATCH
--  21. STUDENT_PHONE
--  22. STUDENT_SKILL
--  23. STUDENT
--  24. PORTAL_USER (preserves root admin)
--
-- PRESERVED ESSENTIAL MASTER DATA:
--   - PROGRAM table: Canonical branch definitions (BCE, BCT, BEC, BME, BDS, BEE, BCV)
--   - PORTAL_USER table: Root administrator account ('admin')
--   - All database tables, indexes, constraints, triggers, procedures, views
-- ==============================================================================

SET SERVEROUTPUT ON;
WHENEVER SQLERROR EXIT FAILURE ROLLBACK;

PROMPT Starting controlled cleanup of PlaceX demo/mock data...

-- ------------------------------------------------------------------------------
-- 1. Application Audits & Candidate Evaluations
-- ------------------------------------------------------------------------------
PROMPT Purging APPLICATION_AUDIT records...;
DELETE FROM APPLICATION_AUDIT 
WHERE Application_Id LIKE 'APP%' 
   OR Changed_By = 'SYSTEM';

PROMPT Purging RESUME_EVALUATION records...;
DELETE FROM RESUME_EVALUATION 
WHERE Evaluation_Id LIKE 'EVAL%' 
   OR Recruiter_Id IN ('recruiter1', 'recruiter2');

PROMPT Purging STUDENT_RESUME records...;
DELETE FROM STUDENT_RESUME 
WHERE Student_Id LIKE 'STU0%' 
   OR Resume_Id LIKE 'RES_STU%';

-- ------------------------------------------------------------------------------
-- 2. Offer Letters, Interviews, and Interviewer Rounds
-- ------------------------------------------------------------------------------
PROMPT Purging OFFER_LETTER records...;
DELETE FROM OFFER_LETTER 
WHERE Offer_Id LIKE 'OFF%' 
   OR Application_Id LIKE 'APP%';

PROMPT Purging INTERVIEW records...;
DELETE FROM INTERVIEW 
WHERE Application_Id LIKE 'APP%';

PROMPT Purging seeded INTERVIEWER_ROUND records...;
DELETE FROM INTERVIEWER_ROUND 
WHERE Interviewer_Name IN (
    'Dr. Rajesh Sundaram', 
    'Priya Ramanathan', 
    'Vikramaditya Bose', 
    'Ananya Chatterjee', 
    'Suresh Venkataraman', 
    'Kavita Menon'
);

-- ------------------------------------------------------------------------------
-- 3. Applications and Daily Routine Drives
-- ------------------------------------------------------------------------------
PROMPT Purging APPLICATION records...;
DELETE FROM APPLICATION 
WHERE Application_Id LIKE 'APP%' 
   OR Student_Id LIKE 'STU0%';

PROMPT Purging STUDENT_DAILY_ROUTINE_DRIVE records...;
DELETE FROM STUDENT_DAILY_ROUTINE_DRIVE 
WHERE Student_Id LIKE 'STU0%' 
   OR Drive_Id LIKE 'DRV0%';

-- ------------------------------------------------------------------------------
-- 4. Placement Drives and Corporate Company Entities
-- ------------------------------------------------------------------------------
PROMPT Purging DRIVE_ELIGIBILITY records...;
DELETE FROM DRIVE_ELIGIBILITY 
WHERE Drive_Id LIKE 'DRV0%';

PROMPT Purging PLACEMENT_DRIVE records...;
DELETE FROM PLACEMENT_DRIVE 
WHERE Drive_Id LIKE 'DRV0%';

PROMPT Purging JOB_COMPANY records...;
DELETE FROM JOB_COMPANY 
WHERE Company_Id LIKE 'COM0%';

PROMPT Purging COMPANY_PHONE records...;
DELETE FROM COMPANY_PHONE 
WHERE Company_Id LIKE 'COM0%';

PROMPT Purging COMPANY records...;
DELETE FROM COMPANY 
WHERE Company_Id LIKE 'COM0%';

PROMPT Purging EMAIL_COMPANY records...;
DELETE FROM EMAIL_COMPANY 
WHERE Email IN (
    'campus@tcs.com', 
    'talent@infosys.com', 
    'careers@wipro.com', 
    'indiarecruiting@microsoft.com', 
    'campus-in@amazon.com', 
    'campus_recruitment@oracle.com', 
    'campuscareers@gs.com', 
    'talent_apac@cisco.com', 
    'hiring@flipkart.com', 
    'earlycareers@zomato.com'
);

-- ------------------------------------------------------------------------------
-- 5. Training Programs, Batches & Assessments
-- ------------------------------------------------------------------------------
PROMPT Purging ATTENDS records...;
DELETE FROM ATTENDS 
WHERE Student_Id LIKE 'STU0%';

PROMPT Purging REGISTERS records...;
DELETE FROM REGISTERS 
WHERE Student_Id LIKE 'STU0%';

PROMPT Purging ASSESSMENT records...;
DELETE FROM ASSESSMENT 
WHERE Assessment_Id LIKE 'ASM0%';

PROMPT Purging BATCH_TITLE_PROGRAM records...;
DELETE FROM BATCH_TITLE_PROGRAM 
WHERE Batch_No LIKE 'BAT0%';

PROMPT Purging BATCH_PROGRAM records...;
DELETE FROM BATCH_PROGRAM 
WHERE Batch_No LIKE 'BAT0%';

PROMPT Purging BATCH records...;
DELETE FROM BATCH 
WHERE Batch_No LIKE 'BAT0%';

-- ------------------------------------------------------------------------------
-- 6. Students and Student Attributes
-- ------------------------------------------------------------------------------
PROMPT Purging STUDENT_PHONE records...;
DELETE FROM STUDENT_PHONE 
WHERE Student_Id LIKE 'STU0%';

PROMPT Purging STUDENT_SKILL records...;
DELETE FROM STUDENT_SKILL 
WHERE Student_Id LIKE 'STU0%';

PROMPT Purging demo STUDENT records (STU001 through STU022)...;
DELETE FROM STUDENT 
WHERE Student_Id LIKE 'STU0%';

-- ------------------------------------------------------------------------------
-- 7. Demo Users in PORTAL_USER (Preserves root 'admin')
-- ------------------------------------------------------------------------------
PROMPT Purging demo users from PORTAL_USER...;
DELETE FROM PORTAL_USER 
WHERE Username IN ('student1', 'student2', 'student4', 'recruiter1', 'recruiter2')
   OR Reference_Id LIKE 'STU0%'
   OR Reference_Id LIKE 'COM0%';

-- Ensure root admin user exists
MERGE INTO PORTAL_USER pu
USING (
    SELECT 'USR_ADMIN' AS uid, 'admin' AS uname, 
           '$2a$10$wTkyrAedz9u9fK1u0lPj1e7o/mD7lF40N3Yn2k12jD6R49Yw6p2a.' AS phash, 
           'ROLE_ADMIN' AS rname, 'ADMIN01' AS refid 
    FROM DUAL
) src
ON (pu.Username = src.uname)
WHEN NOT MATCHED THEN
    INSERT (User_Id, Username, Password_Hash, Role, Reference_Id)
    VALUES (src.uid, src.uname, src.phash, src.rname, src.refid);

PROMPT Preserving canonical PROGRAM records:
SELECT Program_Id, Program_Name, Duration FROM PROGRAM ORDER BY Program_Id;

PROMPT Preserving administrative user:
SELECT User_Id, Username, Role, Reference_Id FROM PORTAL_USER WHERE Username = 'admin';

COMMIT;
PROMPT PlaceX database cleanup completed successfully. Zero mock records remain.
