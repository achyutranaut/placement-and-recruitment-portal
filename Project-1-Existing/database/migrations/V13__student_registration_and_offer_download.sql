-- ============================================================
-- V13__student_registration_and_offer_download.sql
-- Student Registration Number, Email Domain Validation & Seed Update
-- ============================================================

-- 1. Add Registration_No column to STUDENT
ALTER TABLE STUDENT ADD (
    Registration_No VARCHAR2(20)
);

-- 2. Populate Registration Numbers and @vitstudent.ac.in Emails for Seed Dataset
UPDATE STUDENT SET Registration_No = '21BCE1001', Email = 'aarav.sharma@vitstudent.ac.in' WHERE Student_Id = 'STU001';
UPDATE STUDENT SET Registration_No = '21BCT1002', Email = 'diya.patel@vitstudent.ac.in' WHERE Student_Id = 'STU002';
UPDATE STUDENT SET Registration_No = '21BCE1003', Email = 'rohan.iyer@vitstudent.ac.in' WHERE Student_Id = 'STU003';
UPDATE STUDENT SET Registration_No = '21BDS1004', Email = 'ananya.gupta@vitstudent.ac.in' WHERE Student_Id = 'STU004';
UPDATE STUDENT SET Registration_No = '21BEC1005', Email = 'ishaan.verma@vitstudent.ac.in' WHERE Student_Id = 'STU005';
UPDATE STUDENT SET Registration_No = '21BCE1006', Email = 'priya.nair@vitstudent.ac.in' WHERE Student_Id = 'STU006';
UPDATE STUDENT SET Registration_No = '21BCT1007', Email = 'siddharth.rao@vitstudent.ac.in' WHERE Student_Id = 'STU007';
UPDATE STUDENT SET Registration_No = '21BCE1008', Email = 'tanvi.joshi@vitstudent.ac.in' WHERE Student_Id = 'STU008';
UPDATE STUDENT SET Registration_No = '21BME1009', Email = 'aditya.deshmukh@vitstudent.ac.in' WHERE Student_Id = 'STU009';
UPDATE STUDENT SET Registration_No = '21BCE1010', Email = 'sneha.kulkarni@vitstudent.ac.in' WHERE Student_Id = 'STU010';
UPDATE STUDENT SET Registration_No = '21BDS1011', Email = 'vikram.reddy@vitstudent.ac.in' WHERE Student_Id = 'STU011';
UPDATE STUDENT SET Registration_No = '21BCE1012', Email = 'neha.sengupta@vitstudent.ac.in' WHERE Student_Id = 'STU012';
UPDATE STUDENT SET Registration_No = '21BCT1013', Email = 'rahul.mehra@vitstudent.ac.in' WHERE Student_Id = 'STU013';
UPDATE STUDENT SET Registration_No = '21BEC1014', Email = 'riya.kapoor@vitstudent.ac.in' WHERE Student_Id = 'STU014';
UPDATE STUDENT SET Registration_No = '21BEE1015', Email = 'arjun.das@vitstudent.ac.in' WHERE Student_Id = 'STU015';
UPDATE STUDENT SET Registration_No = '21BCE1016', Email = 'pooja.hegde@vitstudent.ac.in' WHERE Student_Id = 'STU016';
UPDATE STUDENT SET Registration_No = '21BME1017', Email = 'varun.choudhury@vitstudent.ac.in' WHERE Student_Id = 'STU017';
UPDATE STUDENT SET Registration_No = '21BCT1018', Email = 'meera.nambiar@vitstudent.ac.in' WHERE Student_Id = 'STU018';
UPDATE STUDENT SET Registration_No = '21BCL1019', Email = 'kunal.malhotra@vitstudent.ac.in' WHERE Student_Id = 'STU019';
UPDATE STUDENT SET Registration_No = '21BDS1020', Email = 'shreya.mukherjee@vitstudent.ac.in' WHERE Student_Id = 'STU020';
UPDATE STUDENT SET Registration_No = '21BCE1021', Email = 'karthik.subramanian@vitstudent.ac.in' WHERE Student_Id = 'STU021';
UPDATE STUDENT SET Registration_No = '21BEC1022', Email = 'divya.menon@vitstudent.ac.in' WHERE Student_Id = 'STU022';

-- Populate Branch for seed students
UPDATE STUDENT SET Branch = 'Computer Science & Engineering' WHERE Registration_No LIKE '%BCE%';
UPDATE STUDENT SET Branch = 'Information Technology' WHERE Registration_No LIKE '%BCT%';
UPDATE STUDENT SET Branch = 'Electronics & Communication' WHERE Registration_No LIKE '%BEC%';
UPDATE STUDENT SET Branch = 'Mechanical Engineering' WHERE Registration_No LIKE '%BME%';
UPDATE STUDENT SET Branch = 'Data Science' WHERE Registration_No LIKE '%BDS%';
UPDATE STUDENT SET Branch = 'Electrical & Electronics' WHERE Registration_No LIKE '%BEE%';
UPDATE STUDENT SET Branch = 'Civil Engineering' WHERE Registration_No LIKE '%BCL%';

-- 3. Add Constraints
ALTER TABLE STUDENT ADD CONSTRAINT UQ_STUDENT_REGNO UNIQUE (Registration_No);
ALTER TABLE STUDENT ADD CONSTRAINT CHK_STUDENT_EMAIL CHECK (Email LIKE '%@vitstudent.ac.in');

COMMIT;
