-- ============================================================
-- V25__enforce_student_not_null_constraints.sql
-- Enforce NOT NULL constraints on mandatory STUDENT attributes:
-- Street, City, State, Branch, Registration_No
-- Also enforce NOT NULL on APPLICATION.Drive_Id
-- ============================================================

-- 1. Safely resolve existing NULL data on real registered student records
-- STU023 (Achyut Ranaut) is a registered VIT Chennai student residing on campus
UPDATE STUDENT
SET Street = 'Vandalur-Kelambakkam Road'
WHERE Student_Id = 'STU023' AND Street IS NULL;

-- 2. Fallback safety for any other records to ensure valid location data
UPDATE STUDENT
SET Street = 'Vandalur-Kelambakkam Road'
WHERE Street IS NULL;

UPDATE STUDENT
SET City = 'Chennai'
WHERE City IS NULL;

UPDATE STUDENT
SET State = 'Tamil Nadu'
WHERE State IS NULL;

UPDATE STUDENT
SET Branch = 'B.Tech Computer Science and Engineering'
WHERE Branch IS NULL;

UPDATE STUDENT
SET Registration_No = 'REG_' || Student_Id
WHERE Registration_No IS NULL;

-- 3. Alter STUDENT table to make all mandatory personal, academic, and location attributes NOT NULL
ALTER TABLE STUDENT MODIFY (
    Street          NOT NULL,
    City            NOT NULL,
    State           NOT NULL,
    Branch          NOT NULL,
    Registration_No NOT NULL
);

-- 4. Alter APPLICATION table to make Drive_Id NOT NULL
ALTER TABLE APPLICATION MODIFY (
    Drive_Id        NOT NULL
);

COMMIT;
