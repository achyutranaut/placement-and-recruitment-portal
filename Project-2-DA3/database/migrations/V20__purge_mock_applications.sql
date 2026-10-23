-- ============================================================
-- V20__purge_mock_applications.sql
-- Purge seeded mock applications, interviews, and offers from initial sample data.
-- Preserves real applications submitted by registered students (e.g. APP036 for STU023).
-- ============================================================

-- 1. Remove mock offer letters
DELETE FROM OFFER_LETTER WHERE Application_Id != 'APP036';

-- 2. Remove mock interview evaluations
DELETE FROM INTERVIEW WHERE Application_Id != 'APP036';

-- 3. Remove mock resume evaluations
DELETE FROM RESUME_EVALUATION WHERE Application_Id != 'APP036';

-- 4. Remove mock student daily routine records for seeded students
DELETE FROM STUDENT_DAILY_ROUTINE_DRIVE WHERE Student_Id != 'STU023';

-- 5. Remove mock applications (APP001 through APP035)
DELETE FROM APPLICATION WHERE Application_Id != 'APP036';

-- 6. Ensure real application APP036 has explicit Drive_Id populated
UPDATE APPLICATION SET Drive_Id = 'DRV001' WHERE Application_Id = 'APP036';

COMMIT;
