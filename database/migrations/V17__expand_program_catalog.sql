-- ============================================================
-- V17__expand_program_catalog.sql
-- Expand Academic Degree Program Catalog to Complete Institutional Curriculum
-- ============================================================

-- 1. Add Curriculum Metadata & Validation Columns to PROGRAM Table
ALTER TABLE PROGRAM ADD (
    Program_Level     VARCHAR2(30)  DEFAULT 'Bachelor''s' NOT NULL,
    Discipline_Family VARCHAR2(50)  DEFAULT 'Engineering' NOT NULL,
    Specialization    VARCHAR2(100),
    Reg_Pattern       VARCHAR2(100),
    Reg_Example       VARCHAR2(30),
    Is_Active         CHAR(1)       DEFAULT 'Y' NOT NULL
);

-- 2. Mark legacy 4-option / abbreviation entries as Inactive Legacy
UPDATE PROGRAM SET Is_Active = 'N', Program_Level = 'Legacy' WHERE Program_Id IN ('BCE', 'BCT', 'BEC', 'BME', 'BDS', 'BEE', 'BCV');

-- 3. Seed Complete Institutional Program Catalog (107 Programs)
MERGE INTO PROGRAM p
USING (
    SELECT 'BTECH-CSE' AS pid, 'B.Tech CSE' AS pname, 'Bachelor''s' AS plevel, 'Computer Science' AS pfamily, NULL AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCE[0-9]{4}$' AS pat, '21BCE1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-CSE-AIML' AS pid, 'B.Tech CSE (AI & ML)' AS pname, 'Bachelor''s' AS plevel, 'Computer Science' AS pfamily, 'AI & ML' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCE[0-9]{4}$' AS pat, '21BCE1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-CSE-AIDA' AS pid, 'B.Tech CSE (AI & Data Analytics)' AS pname, 'Bachelor''s' AS plevel, 'Computer Science' AS pfamily, 'AI & Data Analytics' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCE[0-9]{4}$' AS pat, '21BCE1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-CSE-BLOCK' AS pid, 'B.Tech CSE (Blockchain)' AS pname, 'Bachelor''s' AS plevel, 'Computer Science' AS pfamily, 'Blockchain' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCE[0-9]{4}$' AS pat, '21BCE1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-CSE-CYBER' AS pid, 'B.Tech CSE (Cyber Security)' AS pname, 'Bachelor''s' AS plevel, 'Computer Science' AS pfamily, 'Cyber Security' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCE[0-9]{4}$' AS pat, '21BCE1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-CSE-DS' AS pid, 'B.Tech CSE (Data Science)' AS pname, 'Bachelor''s' AS plevel, 'Computer Science' AS pfamily, 'Data Science' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCE[0-9]{4}$' AS pat, '21BCE1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-CSE-GAMING' AS pid, 'B.Tech CSE (Gaming Tech)' AS pname, 'Bachelor''s' AS plevel, 'Computer Science' AS pfamily, 'Gaming Tech' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCE[0-9]{4}$' AS pat, '21BCE1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-CSE-IOT' AS pid, 'B.Tech CSE (IoT)' AS pname, 'Bachelor''s' AS plevel, 'Computer Science' AS pfamily, 'IoT' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCE[0-9]{4}$' AS pat, '21BCE1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-CSE-SWE' AS pid, 'B.Tech CSE (Software Engineering)' AS pname, 'Bachelor''s' AS plevel, 'Computer Science' AS pfamily, 'Software Engineering' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCE[0-9]{4}$' AS pat, '21BCE1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-ECE' AS pid, 'B.Tech ECE' AS pname, 'Bachelor''s' AS plevel, 'Electronics' AS pfamily, NULL AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BEC[0-9]{4}$' AS pat, '21BEC1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-ECE-AICYBER' AS pid, 'B.Tech ECE (AI & Cybernetics)' AS pname, 'Bachelor''s' AS plevel, 'Electronics' AS pfamily, 'AI & Cybernetics' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BEC[0-9]{4}$' AS pat, '21BEC1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-ECE-EMBED' AS pid, 'B.Tech ECE (Embedded Systems)' AS pname, 'Bachelor''s' AS plevel, 'Electronics' AS pfamily, 'Embedded Systems' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BEC[0-9]{4}$' AS pat, '21BEC1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-ECE-VLSI' AS pid, 'B.Tech ECE (VLSI)' AS pname, 'Bachelor''s' AS plevel, 'Electronics' AS pfamily, 'VLSI' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BEC[0-9]{4}$' AS pat, '21BEC1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-ECE-BIOMED' AS pid, 'B.Tech ECE (Biomedical)' AS pname, 'Bachelor''s' AS plevel, 'Electronics' AS pfamily, 'Biomedical' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BEC[0-9]{4}$' AS pat, '21BEC1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-EEE' AS pid, 'B.Tech EEE' AS pname, 'Bachelor''s' AS plevel, 'Electrical' AS pfamily, NULL AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BEE[0-9]{4}$' AS pat, '21BEE1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-ELE-COMP' AS pid, 'B.Tech Electronics & Computer Engineering' AS pname, 'Bachelor''s' AS plevel, 'Electronics' AS pfamily, 'Computer Engineering' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BEC[0-9]{4}$' AS pat, '21BEC1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-ELE-INST' AS pid, 'B.Tech Electronics & Instrumentation' AS pname, 'Bachelor''s' AS plevel, 'Electronics' AS pfamily, 'Instrumentation' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BEI[0-9]{4}$' AS pat, '21BEI1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-MECH' AS pid, 'B.Tech Mechanical' AS pname, 'Bachelor''s' AS plevel, 'Mechanical' AS pfamily, NULL AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BME[0-9]{4}$' AS pat, '21BME1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-MECH-AUTO' AS pid, 'B.Tech Mechanical (Automotive)' AS pname, 'Bachelor''s' AS plevel, 'Mechanical' AS pfamily, 'Automotive' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BME[0-9]{4}$' AS pat, '21BME1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-MECH-EV' AS pid, 'B.Tech Mechanical (EV)' AS pname, 'Bachelor''s' AS plevel, 'Mechanical' AS pfamily, 'EV' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BME[0-9]{4}$' AS pat, '21BME1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-MECH-MFG' AS pid, 'B.Tech Mechanical (Manufacturing)' AS pname, 'Bachelor''s' AS plevel, 'Mechanical' AS pfamily, 'Manufacturing' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BME[0-9]{4}$' AS pat, '21BME1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-MECH-ROBOT' AS pid, 'B.Tech Mechanical (Robotics)' AS pname, 'Bachelor''s' AS plevel, 'Mechanical' AS pfamily, 'Robotics' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BME[0-9]{4}$' AS pat, '21BME1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-CIVIL' AS pid, 'B.Tech Civil' AS pname, 'Bachelor''s' AS plevel, 'Civil' AS pfamily, NULL AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCV[0-9]{4}$' AS pat, '21BCV1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-CIVIL-LT' AS pid, 'B.Tech Civil (L&T Collaboration)' AS pname, 'Bachelor''s' AS plevel, 'Civil' AS pfamily, 'L&T Collaboration' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCV[0-9]{4}$' AS pat, '21BCV1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-CHEM' AS pid, 'B.Tech Chemical' AS pname, 'Bachelor''s' AS plevel, 'Chemical' AS pfamily, NULL AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCH[0-9]{4}$' AS pat, '21BCH1234' AS ex FROM DUAL
) src
ON (p.Program_Id = src.pid)
WHEN MATCHED THEN
    UPDATE SET
        p.Program_Name = src.pname,
        p.Program_Level = src.plevel,
        p.Discipline_Family = src.pfamily,
        p.Specialization = src.spec,
        p.Duration = src.dur,
        p.Beginner = src.beg,
        p.Intermediate = src.intm,
        p.Advanced = src.adv,
        p.Reg_Pattern = src.pat,
        p.Reg_Example = src.ex,
        p.Is_Active = 'Y'
WHEN NOT MATCHED THEN
    INSERT (Program_Id, Program_Name, Fee, Duration, Beginner, Intermediate, Advanced, Program_Level, Discipline_Family, Specialization, Reg_Pattern, Reg_Example, Is_Active)
    VALUES (src.pid, src.pname, 0, src.dur, src.beg, src.intm, src.adv, src.plevel, src.pfamily, src.spec, src.pat, src.ex, 'Y');

MERGE INTO PROGRAM p
USING (
    SELECT 'BTECH-BIOTECH' AS pid, 'B.Tech Biotechnology' AS pname, 'Bachelor''s' AS plevel, 'Biotechnology' AS pfamily, NULL AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BBT[0-9]{4}$' AS pat, '21BBT1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-AERO' AS pid, 'B.Tech Aerospace' AS pname, 'Bachelor''s' AS plevel, 'Aerospace' AS pfamily, NULL AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BAE[0-9]{4}$' AS pat, '21BAE1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-BIOENG' AS pid, 'B.Tech Bioengineering' AS pname, 'Bachelor''s' AS plevel, 'Bioengineering' AS pfamily, NULL AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BBI[0-9]{4}$' AS pat, '21BBI1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-IT' AS pid, 'B.Tech IT' AS pname, 'Bachelor''s' AS plevel, 'Information Technology' AS pfamily, NULL AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCT[0-9]{4}$' AS pat, '21BCT1234' AS ex FROM DUAL UNION ALL
    SELECT 'BTECH-FASHION' AS pid, 'B.Tech Fashion Technology' AS pname, 'Bachelor''s' AS plevel, 'Design' AS pfamily, 'Fashion Technology' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BFT[0-9]{4}$' AS pat, '21BFT1234' AS ex FROM DUAL UNION ALL
    SELECT 'BARCH' AS pid, 'B.Arch' AS pname, 'Bachelor''s' AS plevel, 'Architecture' AS pfamily, NULL AS spec, '5 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BAR[0-9]{4}$' AS pat, '21BAR1001' AS ex FROM DUAL UNION ALL
    SELECT 'BDES-PROD' AS pid, 'B.Des Industrial/Product Design' AS pname, 'Bachelor''s' AS plevel, 'Design' AS pfamily, 'Industrial/Product Design' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BDS[0-9]{4}$' AS pat, '21BDS1001' AS ex FROM DUAL UNION ALL
    SELECT 'BSC-CS' AS pid, 'B.Sc Computer Science' AS pname, 'Bachelor''s' AS plevel, 'Computer Science' AS pfamily, NULL AS spec, '3 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BSC[0-9]{4}$' AS pat, '21BSC1001' AS ex FROM DUAL UNION ALL
    SELECT 'BSC-ECON' AS pid, 'B.Sc Economics (Hons.)' AS pname, 'Bachelor''s' AS plevel, 'Science' AS pfamily, 'Economics' AS spec, '3 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BSC[0-9]{4}$' AS pat, '21BSC1001' AS ex FROM DUAL UNION ALL
    SELECT 'BSC-FASHION' AS pid, 'B.Sc Fashion Design' AS pname, 'Bachelor''s' AS plevel, 'Design' AS pfamily, 'Fashion Design' AS spec, '3 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BSC[0-9]{4}$' AS pat, '21BSC1001' AS ex FROM DUAL UNION ALL
    SELECT 'BSC-HOSP' AS pid, 'B.Sc Hospitality & Hotel Administration' AS pname, 'Bachelor''s' AS plevel, 'Management' AS pfamily, 'Hospitality & Hotel Administration' AS spec, '3 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BSC[0-9]{4}$' AS pat, '21BSC1001' AS ex FROM DUAL UNION ALL
    SELECT 'BSC-ANIM' AS pid, 'B.Sc Multimedia & Animation' AS pname, 'Bachelor''s' AS plevel, 'Design' AS pfamily, 'Multimedia & Animation' AS spec, '3 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BSC[0-9]{4}$' AS pat, '21BSC1001' AS ex FROM DUAL UNION ALL
    SELECT 'BSC-VISCOM' AS pid, 'B.Sc Visual Communication' AS pname, 'Bachelor''s' AS plevel, 'Design' AS pfamily, 'Visual Communication' AS spec, '3 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BSC[0-9]{4}$' AS pat, '21BSC1001' AS ex FROM DUAL UNION ALL
    SELECT 'BSC-AGRI' AS pid, 'B.Sc Agriculture (Hons.)' AS pname, 'Bachelor''s' AS plevel, 'Science' AS pfamily, 'Agriculture' AS spec, '4 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BAG[0-9]{4}$' AS pat, '21BAG1001' AS ex FROM DUAL UNION ALL
    SELECT 'BBA' AS pid, 'BBA' AS pname, 'Bachelor''s' AS plevel, 'Management' AS pfamily, NULL AS spec, '3 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BBA[0-9]{4}$' AS pat, '21BBA1001' AS ex FROM DUAL UNION ALL
    SELECT 'BBA-HONS' AS pid, 'BBA (Hons.)' AS pname, 'Bachelor''s' AS plevel, 'Management' AS pfamily, 'Honours' AS spec, '3 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BBA[0-9]{4}$' AS pat, '21BBA1001' AS ex FROM DUAL UNION ALL
    SELECT 'BBA-DIGITAL' AS pid, 'BBA Digital Business' AS pname, 'Bachelor''s' AS plevel, 'Management' AS pfamily, 'Digital Business' AS spec, '3 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BBA[0-9]{4}$' AS pat, '21BBA1001' AS ex FROM DUAL UNION ALL
    SELECT 'BCOM' AS pid, 'B.Com' AS pname, 'Bachelor''s' AS plevel, 'Commerce' AS pfamily, NULL AS spec, '3 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCM[0-9]{4}$' AS pat, '21BCM1001' AS ex FROM DUAL UNION ALL
    SELECT 'BCOM-HONS' AS pid, 'B.Com (Hons.)' AS pname, 'Bachelor''s' AS plevel, 'Commerce' AS pfamily, 'Honours' AS spec, '3 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCM[0-9]{4}$' AS pat, '21BCM1001' AS ex FROM DUAL UNION ALL
    SELECT 'BCA' AS pid, 'BCA' AS pname, 'Bachelor''s' AS plevel, 'Computer Science' AS pfamily, NULL AS spec, '3 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BCA[0-9]{4}$' AS pat, '21BCA1001' AS ex FROM DUAL UNION ALL
    SELECT 'BALLB-HONS' AS pid, 'BA LLB (Hons.)' AS pname, 'Bachelor''s' AS plevel, 'Law' AS pfamily, 'Honours' AS spec, '5 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BLW[0-9]{4}$' AS pat, '21BLW1001' AS ex FROM DUAL UNION ALL
    SELECT 'BBALLB-HONS' AS pid, 'BBA LLB (Hons.)' AS pname, 'Bachelor''s' AS plevel, 'Law' AS pfamily, 'Honours' AS spec, '5 Years' AS dur, 'Y' AS beg, 'N' AS intm, 'N' AS adv, '^2[0-9]BLW[0-9]{4}$' AS pat, '21BLW1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-CSE' AS pid, 'M.Tech CSE' AS pname, 'Master''s' AS plevel, 'Computer Science' AS pfamily, NULL AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MCS[0-9]{4}$' AS pat, '24MCS1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-CSE-BIGDATA' AS pid, 'M.Tech CSE (Big Data)' AS pname, 'Master''s' AS plevel, 'Computer Science' AS pfamily, 'Big Data' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MCS[0-9]{4}$' AS pat, '24MCS1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-CSE-CYBER' AS pid, 'M.Tech CSE (Cybersecurity)' AS pname, 'Master''s' AS plevel, 'Computer Science' AS pfamily, 'Cybersecurity' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MCS[0-9]{4}$' AS pat, '24MCS1001' AS ex FROM DUAL
) src
ON (p.Program_Id = src.pid)
WHEN MATCHED THEN
    UPDATE SET
        p.Program_Name = src.pname,
        p.Program_Level = src.plevel,
        p.Discipline_Family = src.pfamily,
        p.Specialization = src.spec,
        p.Duration = src.dur,
        p.Beginner = src.beg,
        p.Intermediate = src.intm,
        p.Advanced = src.adv,
        p.Reg_Pattern = src.pat,
        p.Reg_Example = src.ex,
        p.Is_Active = 'Y'
WHEN NOT MATCHED THEN
    INSERT (Program_Id, Program_Name, Fee, Duration, Beginner, Intermediate, Advanced, Program_Level, Discipline_Family, Specialization, Reg_Pattern, Reg_Example, Is_Active)
    VALUES (src.pid, src.pname, 0, src.dur, src.beg, src.intm, src.adv, src.plevel, src.pfamily, src.spec, src.pat, src.ex, 'Y');

MERGE INTO PROGRAM p
USING (
    SELECT 'MTECH-CSE-INFOSEC' AS pid, 'M.Tech CSE (Information Security)' AS pname, 'Master''s' AS plevel, 'Computer Science' AS pfamily, 'Information Security' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MCS[0-9]{4}$' AS pat, '24MCS1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-ECE-COMM' AS pid, 'M.Tech ECE (Communication)' AS pname, 'Master''s' AS plevel, 'Electronics' AS pfamily, 'Communication' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MEC[0-9]{4}$' AS pat, '24MEC1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-ECE-VLSI' AS pid, 'M.Tech ECE (VLSI Design)' AS pname, 'Master''s' AS plevel, 'Electronics' AS pfamily, 'VLSI Design' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MEC[0-9]{4}$' AS pat, '24MEC1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-EEE-PED' AS pid, 'M.Tech EEE (Power Electronics & Drives)' AS pname, 'Master''s' AS plevel, 'Electrical' AS pfamily, 'Power Electronics & Drives' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MEE[0-9]{4}$' AS pat, '24MEE1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-EEE-CA' AS pid, 'M.Tech EEE (Control & Automation)' AS pname, 'Master''s' AS plevel, 'Electrical' AS pfamily, 'Control & Automation' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MEE[0-9]{4}$' AS pat, '24MEE1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-MECH-CADCAM' AS pid, 'M.Tech Mechanical (CAD/CAM)' AS pname, 'Master''s' AS plevel, 'Mechanical' AS pfamily, 'CAD/CAM' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MME[0-9]{4}$' AS pat, '24MME1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-MECH-MFG' AS pid, 'M.Tech Mechanical (Manufacturing)' AS pname, 'Master''s' AS plevel, 'Mechanical' AS pfamily, 'Manufacturing' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MME[0-9]{4}$' AS pat, '24MME1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-MECH-MECH' AS pid, 'M.Tech Mechanical (Mechatronics)' AS pname, 'Master''s' AS plevel, 'Mechanical' AS pfamily, 'Mechatronics' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MME[0-9]{4}$' AS pat, '24MME1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-MECH-AUTO' AS pid, 'M.Tech Mechanical (Automotive Engineering)' AS pname, 'Master''s' AS plevel, 'Mechanical' AS pfamily, 'Automotive Engineering' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MME[0-9]{4}$' AS pat, '24MME1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-MECH-AUTOE' AS pid, 'M.Tech Mechanical (Automotive Electronics)' AS pname, 'Master''s' AS plevel, 'Mechanical' AS pfamily, 'Automotive Electronics' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MME[0-9]{4}$' AS pat, '24MME1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-CIVIL-CTM' AS pid, 'M.Tech Civil (Construction Technology & Management)' AS pname, 'Master''s' AS plevel, 'Civil' AS pfamily, 'Construction Technology & Management' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MCV[0-9]{4}$' AS pat, '24MCV1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-CIVIL-STRUCT' AS pid, 'M.Tech Civil (Structural)' AS pname, 'Master''s' AS plevel, 'Civil' AS pfamily, 'Structural' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MCV[0-9]{4}$' AS pat, '24MCV1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-BIOMED' AS pid, 'M.Tech Biomedical' AS pname, 'Master''s' AS plevel, 'Bioengineering' AS pfamily, 'Biomedical' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MBM[0-9]{4}$' AS pat, '24MBM1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-BIOTECH' AS pid, 'M.Tech Biotechnology' AS pname, 'Master''s' AS plevel, 'Biotechnology' AS pfamily, NULL AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MBT[0-9]{4}$' AS pat, '24MBT1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-CLOUD' AS pid, 'M.Tech Cloud Computing' AS pname, 'Master''s' AS plevel, 'Computer Science' AS pfamily, 'Cloud Computing' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MCS[0-9]{4}$' AS pat, '24MCS1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-SWE' AS pid, 'M.Tech Software Engineering' AS pname, 'Master''s' AS plevel, 'Computer Science' AS pfamily, 'Software Engineering' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MCS[0-9]{4}$' AS pat, '24MCS1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-EMBED' AS pid, 'M.Tech Embedded Systems' AS pname, 'Master''s' AS plevel, 'Electronics' AS pfamily, 'Embedded Systems' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MEC[0-9]{4}$' AS pat, '24MEC1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-NANO' AS pid, 'M.Tech Nanotechnology' AS pname, 'Master''s' AS plevel, 'Science' AS pfamily, 'Nanotechnology' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MNT[0-9]{4}$' AS pat, '24MNT1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-ENV' AS pid, 'M.Tech Energy & Environmental Engineering' AS pname, 'Master''s' AS plevel, 'Civil' AS pfamily, 'Energy & Environmental Engineering' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MEE[0-9]{4}$' AS pat, '24MEE1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-IND-AUTO' AS pid, 'M.Tech Industrial Automation' AS pname, 'Master''s' AS plevel, 'Mechanical' AS pfamily, 'Industrial Automation' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MME[0-9]{4}$' AS pat, '24MME1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-IND-IOT' AS pid, 'M.Tech Industrial IoT' AS pname, 'Master''s' AS plevel, 'Computer Science' AS pfamily, 'Industrial IoT' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MCS[0-9]{4}$' AS pat, '24MCS1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-IOT-SENSOR' AS pid, 'M.Tech IoT & Sensor Systems' AS pname, 'Master''s' AS plevel, 'Electronics' AS pfamily, 'IoT & Sensor Systems' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MEC[0-9]{4}$' AS pat, '24MEC1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-APPLIED-CFD' AS pid, 'M.Tech Applied CFD' AS pname, 'Master''s' AS plevel, 'Mechanical' AS pfamily, 'Applied CFD' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MME[0-9]{4}$' AS pat, '24MME1001' AS ex FROM DUAL UNION ALL
    SELECT 'MTECH-SMART-MOB' AS pid, 'M.Tech Smart Mobility' AS pname, 'Master''s' AS plevel, 'Mechanical' AS pfamily, 'Smart Mobility' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MME[0-9]{4}$' AS pat, '24MME1001' AS ex FROM DUAL UNION ALL
    SELECT 'MBA' AS pid, 'MBA' AS pname, 'Master''s' AS plevel, 'Management' AS pfamily, NULL AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MBA[0-9]{4}$' AS pat, '24MBA1001' AS ex FROM DUAL
) src
ON (p.Program_Id = src.pid)
WHEN MATCHED THEN
    UPDATE SET
        p.Program_Name = src.pname,
        p.Program_Level = src.plevel,
        p.Discipline_Family = src.pfamily,
        p.Specialization = src.spec,
        p.Duration = src.dur,
        p.Beginner = src.beg,
        p.Intermediate = src.intm,
        p.Advanced = src.adv,
        p.Reg_Pattern = src.pat,
        p.Reg_Example = src.ex,
        p.Is_Active = 'Y'
WHEN NOT MATCHED THEN
    INSERT (Program_Id, Program_Name, Fee, Duration, Beginner, Intermediate, Advanced, Program_Level, Discipline_Family, Specialization, Reg_Pattern, Reg_Example, Is_Active)
    VALUES (src.pid, src.pname, 0, src.dur, src.beg, src.intm, src.adv, src.plevel, src.pfamily, src.spec, src.pat, src.ex, 'Y');

MERGE INTO PROGRAM p
USING (
    SELECT 'MCA' AS pid, 'MCA' AS pname, 'Master''s' AS plevel, 'Computer Science' AS pfamily, NULL AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MCA[0-9]{4}$' AS pat, '24MCA1001' AS ex FROM DUAL UNION ALL
    SELECT 'MSC-BIOTECH' AS pid, 'M.Sc Biotechnology' AS pname, 'Master''s' AS plevel, 'Biotechnology' AS pfamily, NULL AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MSB[0-9]{4}$' AS pat, '24MSB1001' AS ex FROM DUAL UNION ALL
    SELECT 'MSC-DATA-SCI' AS pid, 'M.Sc Data Science' AS pname, 'Master''s' AS plevel, 'Computer Science' AS pfamily, 'Data Science' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MSD[0-9]{4}$' AS pat, '24MSD1001' AS ex FROM DUAL UNION ALL
    SELECT 'MSC-CHEM' AS pid, 'M.Sc Chemistry' AS pname, 'Master''s' AS plevel, 'Science' AS pfamily, 'Chemistry' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MSC[0-9]{4}$' AS pat, '24MSC1001' AS ex FROM DUAL UNION ALL
    SELECT 'MSC-PHYS' AS pid, 'M.Sc Physics' AS pname, 'Master''s' AS plevel, 'Science' AS pfamily, 'Physics' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MSP[0-9]{4}$' AS pat, '24MSP1001' AS ex FROM DUAL UNION ALL
    SELECT 'MSC-APPL-MICRO' AS pid, 'M.Sc Applied Microbiology' AS pname, 'Master''s' AS plevel, 'Science' AS pfamily, 'Applied Microbiology' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MSM[0-9]{4}$' AS pat, '24MSM1001' AS ex FROM DUAL UNION ALL
    SELECT 'MSC-BUS-STAT' AS pid, 'M.Sc Business Statistics' AS pname, 'Master''s' AS plevel, 'Management' AS pfamily, 'Business Statistics' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MSB[0-9]{4}$' AS pat, '24MSB1001' AS ex FROM DUAL UNION ALL
    SELECT 'MSC-APPL-PSYCH' AS pid, 'M.Sc Applied Psychology' AS pname, 'Master''s' AS plevel, 'Science' AS pfamily, 'Applied Psychology' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MSY[0-9]{4}$' AS pat, '24MSY1001' AS ex FROM DUAL UNION ALL
    SELECT 'MDES-IND-DES' AS pid, 'M.Des Industrial Design' AS pname, 'Master''s' AS plevel, 'Design' AS pfamily, 'Industrial Design' AS spec, '2 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MDS[0-9]{4}$' AS pat, '24MDS1001' AS ex FROM DUAL UNION ALL
    SELECT 'LLM-CORP-LAW' AS pid, 'LLM Corporate Law' AS pname, 'Master''s' AS plevel, 'Law' AS pfamily, 'Corporate Law' AS spec, '1 Year' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]LLM[0-9]{4}$' AS pat, '24LLM1001' AS ex FROM DUAL UNION ALL
    SELECT 'LLM-IP-LAW' AS pid, 'LLM Intellectual Property Law' AS pname, 'Master''s' AS plevel, 'Law' AS pfamily, 'Intellectual Property Law' AS spec, '1 Year' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]LLM[0-9]{4}$' AS pat, '24LLM1001' AS ex FROM DUAL UNION ALL
    SELECT 'LLM-INT-LAW' AS pid, 'LLM International Law & Development' AS pname, 'Master''s' AS plevel, 'Law' AS pfamily, 'International Law & Development' AS spec, '1 Year' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]LLM[0-9]{4}$' AS pat, '24LLM1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MTECH-CSE' AS pid, 'Integrated M.Tech CSE' AS pname, 'Integrated' AS plevel, 'Computer Science' AS pfamily, NULL AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MIS[0-9]{4}$' AS pat, '21MIS1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MTECH-CSE-DS' AS pid, 'Integrated M.Tech CSE (Data Science)' AS pname, 'Integrated' AS plevel, 'Computer Science' AS pfamily, 'Data Science' AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MIS[0-9]{4}$' AS pat, '21MIS1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MTECH-BIOTECH' AS pid, 'Integrated M.Tech Biotechnology' AS pname, 'Integrated' AS plevel, 'Biotechnology' AS pfamily, NULL AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MIB[0-9]{4}$' AS pat, '21MIB1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MTECH-CDS' AS pid, 'Integrated M.Tech Computational & Data Science' AS pname, 'Integrated' AS plevel, 'Computer Science' AS pfamily, 'Computational & Data Science' AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MIS[0-9]{4}$' AS pat, '21MIS1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MTECH-CTM' AS pid, 'Integrated M.Tech Construction Technology & Management' AS pname, 'Integrated' AS plevel, 'Civil' AS pfamily, 'Construction Technology & Management' AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MIC[0-9]{4}$' AS pat, '21MIC1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MTECH-MECH' AS pid, 'Integrated M.Tech Mechanical' AS pname, 'Integrated' AS plevel, 'Mechanical' AS pfamily, NULL AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MIM[0-9]{4}$' AS pat, '21MIM1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MTECH-SWE' AS pid, 'Integrated M.Tech Software Engineering' AS pname, 'Integrated' AS plevel, 'Computer Science' AS pfamily, 'Software Engineering' AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MIS[0-9]{4}$' AS pat, '21MIS1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MTECH-AI' AS pid, 'Integrated M.Tech AI' AS pname, 'Integrated' AS plevel, 'Computer Science' AS pfamily, 'AI' AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MIS[0-9]{4}$' AS pat, '21MIS1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MTECH-AI-BIO' AS pid, 'Integrated M.Tech AI & Bioinformatics' AS pname, 'Integrated' AS plevel, 'Computer Science' AS pfamily, 'AI & Bioinformatics' AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MIS[0-9]{4}$' AS pat, '21MIS1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MTECH-CSE-SEC' AS pid, 'Integrated M.Tech CSE (Cyber Security)' AS pname, 'Integrated' AS plevel, 'Computer Science' AS pfamily, 'Cyber Security' AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]MIS[0-9]{4}$' AS pat, '21MIS1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MSC-BIOTECH' AS pid, 'Integrated M.Sc Biotechnology' AS pname, 'Integrated' AS plevel, 'Biotechnology' AS pfamily, NULL AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]IMS[0-9]{4}$' AS pat, '21IMS1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MSC-CSDA' AS pid, 'Integrated M.Sc Computational Statistics & Data Analytics' AS pname, 'Integrated' AS plevel, 'Computer Science' AS pfamily, 'Computational Statistics & Data Analytics' AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]IMS[0-9]{4}$' AS pat, '21IMS1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MSC-PHYS' AS pid, 'Integrated M.Sc Physics' AS pname, 'Integrated' AS plevel, 'Science' AS pfamily, 'Physics' AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]IMP[0-9]{4}$' AS pat, '21IMP1001' AS ex FROM DUAL
) src
ON (p.Program_Id = src.pid)
WHEN MATCHED THEN
    UPDATE SET
        p.Program_Name = src.pname,
        p.Program_Level = src.plevel,
        p.Discipline_Family = src.pfamily,
        p.Specialization = src.spec,
        p.Duration = src.dur,
        p.Beginner = src.beg,
        p.Intermediate = src.intm,
        p.Advanced = src.adv,
        p.Reg_Pattern = src.pat,
        p.Reg_Example = src.ex,
        p.Is_Active = 'Y'
WHEN NOT MATCHED THEN
    INSERT (Program_Id, Program_Name, Fee, Duration, Beginner, Intermediate, Advanced, Program_Level, Discipline_Family, Specialization, Reg_Pattern, Reg_Example, Is_Active)
    VALUES (src.pid, src.pname, 0, src.dur, src.beg, src.intm, src.adv, src.plevel, src.pfamily, src.spec, src.pat, src.ex, 'Y');

MERGE INTO PROGRAM p
USING (
    SELECT 'INT-MSC-CHEM' AS pid, 'Integrated M.Sc Chemistry' AS pname, 'Integrated' AS plevel, 'Science' AS pfamily, 'Chemistry' AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]IMC[0-9]{4}$' AS pat, '21IMC1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MSC-MATH' AS pid, 'Integrated M.Sc Mathematics' AS pname, 'Integrated' AS plevel, 'Science' AS pfamily, 'Mathematics' AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]IMM[0-9]{4}$' AS pat, '21IMM1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-MSC-PSYCH' AS pid, 'Integrated M.Sc Applied Psychology' AS pname, 'Integrated' AS plevel, 'Science' AS pfamily, 'Applied Psychology' AS spec, '5 Years' AS dur, 'N' AS beg, 'Y' AS intm, 'N' AS adv, '^2[0-9]IMY[0-9]{4}$' AS pat, '21IMY1001' AS ex FROM DUAL UNION ALL
    SELECT 'PHD-ENG' AS pid, 'Ph.D. Engineering' AS pname, 'Doctoral' AS plevel, 'Engineering' AS pfamily, NULL AS spec, '4 Years' AS dur, 'N' AS beg, 'N' AS intm, 'Y' AS adv, '^2[0-9]PHD[0-9]{4}$' AS pat, '24PHD1001' AS ex FROM DUAL UNION ALL
    SELECT 'PHD-MGMT' AS pid, 'Ph.D. Management' AS pname, 'Doctoral' AS plevel, 'Management' AS pfamily, NULL AS spec, '4 Years' AS dur, 'N' AS beg, 'N' AS intm, 'Y' AS adv, '^2[0-9]PHD[0-9]{4}$' AS pat, '24PHD1001' AS ex FROM DUAL UNION ALL
    SELECT 'PHD-SCI-LANG' AS pid, 'Ph.D. Science & Languages' AS pname, 'Doctoral' AS plevel, 'Science' AS pfamily, NULL AS spec, '4 Years' AS dur, 'N' AS beg, 'N' AS intm, 'Y' AS adv, '^2[0-9]PHD[0-9]{4}$' AS pat, '24PHD1001' AS ex FROM DUAL UNION ALL
    SELECT 'INT-PHD-ENG' AS pid, 'Integrated Ph.D. Engineering' AS pname, 'Doctoral' AS plevel, 'Engineering' AS pfamily, NULL AS spec, '5 Years' AS dur, 'N' AS beg, 'N' AS intm, 'Y' AS adv, '^2[0-9]IPH[0-9]{4}$' AS pat, '24IPH1001' AS ex FROM DUAL
) src
ON (p.Program_Id = src.pid)
WHEN MATCHED THEN
    UPDATE SET
        p.Program_Name = src.pname,
        p.Program_Level = src.plevel,
        p.Discipline_Family = src.pfamily,
        p.Specialization = src.spec,
        p.Duration = src.dur,
        p.Beginner = src.beg,
        p.Intermediate = src.intm,
        p.Advanced = src.adv,
        p.Reg_Pattern = src.pat,
        p.Reg_Example = src.ex,
        p.Is_Active = 'Y'
WHEN NOT MATCHED THEN
    INSERT (Program_Id, Program_Name, Fee, Duration, Beginner, Intermediate, Advanced, Program_Level, Discipline_Family, Specialization, Reg_Pattern, Reg_Example, Is_Active)
    VALUES (src.pid, src.pname, 0, src.dur, src.beg, src.intm, src.adv, src.plevel, src.pfamily, src.spec, src.pat, src.ex, 'Y');

-- 4. Populate Canonical Program Eligibility for Placement Drives
-- DRV001: Microsoft SDE I (All B.Tech CSE specializations & IT)
MERGE INTO DRIVE_ELIGIBILITY de
USING (
    SELECT 'DRV001' AS did, 'BTECH-CSE' AS pid FROM DUAL UNION ALL
    SELECT 'DRV001' AS did, 'BTECH-CSE-AIML' AS pid FROM DUAL UNION ALL
    SELECT 'DRV001' AS did, 'BTECH-CSE-AIDA' AS pid FROM DUAL UNION ALL
    SELECT 'DRV001' AS did, 'BTECH-CSE-BLOCK' AS pid FROM DUAL UNION ALL
    SELECT 'DRV001' AS did, 'BTECH-CSE-CYBER' AS pid FROM DUAL UNION ALL
    SELECT 'DRV001' AS did, 'BTECH-CSE-DS' AS pid FROM DUAL UNION ALL
    SELECT 'DRV001' AS did, 'BTECH-CSE-GAMING' AS pid FROM DUAL UNION ALL
    SELECT 'DRV001' AS did, 'BTECH-CSE-IOT' AS pid FROM DUAL UNION ALL
    SELECT 'DRV001' AS did, 'BTECH-CSE-SWE' AS pid FROM DUAL UNION ALL
    SELECT 'DRV001' AS did, 'BTECH-IT' AS pid FROM DUAL
) src ON (de.Drive_Id = src.did AND de.Program_Id = src.pid)
WHEN NOT MATCHED THEN INSERT (Drive_Id, Program_Id) VALUES (src.did, src.pid);

-- DRV002: Accenture ASE / TCS ASE (All Engineering Disciplines)
MERGE INTO DRIVE_ELIGIBILITY de
USING (
    SELECT 'DRV002' AS did, 'BTECH-CSE' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-CSE-AIML' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-CSE-AIDA' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-CSE-BLOCK' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-CSE-CYBER' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-CSE-DS' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-CSE-GAMING' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-CSE-IOT' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-CSE-SWE' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-ECE' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-ECE-AICYBER' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-ECE-EMBED' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-ECE-VLSI' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-ECE-BIOMED' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-EEE' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-ELE-COMP' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-ELE-INST' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-MECH' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-MECH-AUTO' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-MECH-EV' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-MECH-MFG' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-MECH-ROBOT' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-CIVIL' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-CIVIL-LT' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-CHEM' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-BIOTECH' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-AERO' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-BIOENG' AS pid FROM DUAL UNION ALL
    SELECT 'DRV002' AS did, 'BTECH-IT' AS pid FROM DUAL
) src ON (de.Drive_Id = src.did AND de.Program_Id = src.pid)
WHEN NOT MATCHED THEN INSERT (Drive_Id, Program_Id) VALUES (src.did, src.pid);

-- DRV003: Infosys Specialist & DRV004: Amazon Cloud Support
MERGE INTO DRIVE_ELIGIBILITY de
USING (
    SELECT 'DRV003' AS did, 'BTECH-CSE' AS pid FROM DUAL UNION ALL
    SELECT 'DRV003' AS did, 'BTECH-CSE-AIML' AS pid FROM DUAL UNION ALL
    SELECT 'DRV003' AS did, 'BTECH-CSE-AIDA' AS pid FROM DUAL UNION ALL
    SELECT 'DRV003' AS did, 'BTECH-CSE-BLOCK' AS pid FROM DUAL UNION ALL
    SELECT 'DRV003' AS did, 'BTECH-CSE-CYBER' AS pid FROM DUAL UNION ALL
    SELECT 'DRV003' AS did, 'BTECH-CSE-DS' AS pid FROM DUAL UNION ALL
    SELECT 'DRV003' AS did, 'BTECH-CSE-GAMING' AS pid FROM DUAL UNION ALL
    SELECT 'DRV003' AS did, 'BTECH-CSE-IOT' AS pid FROM DUAL UNION ALL
    SELECT 'DRV003' AS did, 'BTECH-CSE-SWE' AS pid FROM DUAL UNION ALL
    SELECT 'DRV003' AS did, 'BTECH-IT' AS pid FROM DUAL UNION ALL
    SELECT 'DRV003' AS did, 'BTECH-ECE' AS pid FROM DUAL UNION ALL
    SELECT 'DRV003' AS did, 'BTECH-ECE-VLSI' AS pid FROM DUAL UNION ALL
    SELECT 'DRV003' AS did, 'BTECH-ECE-EMBED' AS pid FROM DUAL
) src ON (de.Drive_Id = src.did AND de.Program_Id = src.pid)
WHEN NOT MATCHED THEN INSERT (Drive_Id, Program_Id) VALUES (src.did, src.pid);

MERGE INTO DRIVE_ELIGIBILITY de
USING (
    SELECT 'DRV004' AS did, 'BTECH-CSE' AS pid FROM DUAL UNION ALL
    SELECT 'DRV004' AS did, 'BTECH-CSE-AIML' AS pid FROM DUAL UNION ALL
    SELECT 'DRV004' AS did, 'BTECH-CSE-AIDA' AS pid FROM DUAL UNION ALL
    SELECT 'DRV004' AS did, 'BTECH-CSE-BLOCK' AS pid FROM DUAL UNION ALL
    SELECT 'DRV004' AS did, 'BTECH-CSE-CYBER' AS pid FROM DUAL UNION ALL
    SELECT 'DRV004' AS did, 'BTECH-CSE-DS' AS pid FROM DUAL UNION ALL
    SELECT 'DRV004' AS did, 'BTECH-CSE-GAMING' AS pid FROM DUAL UNION ALL
    SELECT 'DRV004' AS did, 'BTECH-CSE-IOT' AS pid FROM DUAL UNION ALL
    SELECT 'DRV004' AS did, 'BTECH-CSE-SWE' AS pid FROM DUAL UNION ALL
    SELECT 'DRV004' AS did, 'BTECH-IT' AS pid FROM DUAL UNION ALL
    SELECT 'DRV004' AS did, 'BTECH-ECE' AS pid FROM DUAL UNION ALL
    SELECT 'DRV004' AS did, 'BTECH-ECE-VLSI' AS pid FROM DUAL UNION ALL
    SELECT 'DRV004' AS did, 'BTECH-ECE-EMBED' AS pid FROM DUAL
) src ON (de.Drive_Id = src.did AND de.Program_Id = src.pid)
WHEN NOT MATCHED THEN INSERT (Drive_Id, Program_Id) VALUES (src.did, src.pid);

COMMIT;
