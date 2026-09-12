-- ============================================================
-- V14__drive_eligibility_and_canonical_programs.sql
-- Canonical Academic Programs, Student Program Mapping, and Drive Eligibility
-- ============================================================

-- 1. Insert Canonical Academic Degree Programs into PROGRAM table
-- Satisfies TRG_PROGRAM_LEVEL_MUTEX: Exactly one level flag must be 'Y'
MERGE INTO PROGRAM p
USING (
    SELECT 'BCE' AS pid, 'Computer Science & Engineering' AS pname FROM DUAL UNION ALL
    SELECT 'BCT' AS pid, 'Information Technology' AS pname FROM DUAL UNION ALL
    SELECT 'BEC' AS pid, 'Electronics & Communication' AS pname FROM DUAL UNION ALL
    SELECT 'BME' AS pid, 'Mechanical Engineering' AS pname FROM DUAL UNION ALL
    SELECT 'BDS' AS pid, 'Data Science' AS pname FROM DUAL UNION ALL
    SELECT 'BEE' AS pid, 'Electrical & Electronics Engineering' AS pname FROM DUAL UNION ALL
    SELECT 'BCV' AS pid, 'Civil Engineering' AS pname FROM DUAL
) src
ON (p.Program_Id = src.pid)
WHEN NOT MATCHED THEN
    INSERT (Program_Id, Program_Name, Fee, Duration, Beginner, Intermediate, Advanced)
    VALUES (src.pid, src.pname, 0, '4 Years', 'Y', 'N', 'N');

-- 2. Alter STUDENT Table to incorporate canonical Program_Id
ALTER TABLE STUDENT ADD (
    Program_Id VARCHAR2(20)
);

-- 3. Populate Program_Id for all existing students based on their registration numbers / branch
UPDATE STUDENT SET Program_Id = 'BCE' WHERE Registration_No LIKE '%BCE%' OR Program_Id IS NULL;
UPDATE STUDENT SET Program_Id = 'BCT' WHERE Registration_No LIKE '%BCT%';
UPDATE STUDENT SET Program_Id = 'BEC' WHERE Registration_No LIKE '%BEC%';
UPDATE STUDENT SET Program_Id = 'BME' WHERE Registration_No LIKE '%BME%';

ALTER TABLE STUDENT MODIFY (Program_Id NOT NULL);

ALTER TABLE STUDENT ADD CONSTRAINT FK_STUDENT_PROGRAM
    FOREIGN KEY (Program_Id) REFERENCES PROGRAM (Program_Id) ON DELETE CASCADE;

-- 4. Ensure students are also mapped in REGISTERS table for relational integrity
MERGE INTO REGISTERS r
USING (
    SELECT Student_Id, Program_Id FROM STUDENT
) s
ON (r.Student_Id = s.Student_Id AND r.Program_Id = s.Program_Id)
WHEN NOT MATCHED THEN
    INSERT (Student_Id, Program_Id, Reg_Date)
    VALUES (s.Student_Id, s.Program_Id, SYSDATE);

-- 5. Create DRIVE_ELIGIBILITY Table (Canonical Placement Drive <-> Academic Program Junction)
CREATE TABLE DRIVE_ELIGIBILITY (
    Drive_Id   VARCHAR2(20) NOT NULL,
    Program_Id VARCHAR2(20) NOT NULL,
    CONSTRAINT PK_DRIVE_ELIGIBILITY PRIMARY KEY (Drive_Id, Program_Id),
    CONSTRAINT FK_DE_DRIVE FOREIGN KEY (Drive_Id) REFERENCES PLACEMENT_DRIVE (Drive_Id) ON DELETE CASCADE,
    CONSTRAINT FK_DE_PROGRAM FOREIGN KEY (Program_Id) REFERENCES PROGRAM (Program_Id) ON DELETE CASCADE
);

-- 6. Populate Canonical Eligibility for all Placement Drives
-- DRV001: Microsoft SDE I -> Computer Science & Engineering (BCE), Information Technology (BCT), Data Science (BDS)
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV001', 'BCE');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV001', 'BCT');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV001', 'BDS');

-- DRV002: Accenture ASE -> All Engineering Disciplines
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV002', 'BCE');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV002', 'BCT');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV002', 'BEC');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV002', 'BME');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV002', 'BEE');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV002', 'BCV');

-- DRV003: Infosys Specialist -> Computer Science (BCE), IT (BCT), ECE (BEC)
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV003', 'BCE');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV003', 'BCT');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV003', 'BEC');

-- DRV004: Amazon Cloud Support -> BCE, BCT, BEC, BDS
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV004', 'BCE');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV004', 'BCT');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV004', 'BEC');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV004', 'BDS');

-- DRV005: Oracle Assoc App Eng -> BCE, BCT
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV005', 'BCE');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV005', 'BCT');

-- DRV006: Google Tech Analyst -> BCE, BCT, BDS
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV006', 'BCE');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV006', 'BCT');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV006', 'BDS');

-- DRV007: Cisco Backend Eng -> BCE, BCT, BDS
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV007', 'BCE');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV007', 'BCT');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV007', 'BDS');

-- DRV008: Cisco Network Eng -> BCE, BCT, BEC
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV008', 'BCE');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV008', 'BCT');
INSERT INTO DRIVE_ELIGIBILITY (Drive_Id, Program_Id) VALUES ('DRV008', 'BEC');

COMMIT;
