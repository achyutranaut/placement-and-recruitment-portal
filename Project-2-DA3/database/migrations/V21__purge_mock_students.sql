-- ============================================================
-- V21__purge_mock_students.sql
-- Purge seeded mock students (STU001 through STU022) and their child records.
-- Preserves real registered student STU023 (Achyut Ranaut) and related data.
-- ============================================================

-- 1. Remove mock attendance records
DELETE FROM ATTENDS WHERE Student_Id != 'STU023';

-- 2. Remove mock academic registrations
DELETE FROM REGISTERS WHERE Student_Id != 'STU023';

-- 3. Remove mock student phone numbers
DELETE FROM STUDENT_PHONE WHERE Student_Id != 'STU023';

-- 4. Remove mock student skills
DELETE FROM STUDENT_SKILL WHERE Student_Id != 'STU023';

-- 5. Remove mock student portal user accounts (student1, student2)
DELETE FROM PORTAL_USER WHERE Reference_Id LIKE 'STU0%' AND Reference_Id != 'STU023';

-- 6. Remove mock student records (STU001 - STU022)
DELETE FROM STUDENT WHERE Student_Id != 'STU023';

COMMIT;
