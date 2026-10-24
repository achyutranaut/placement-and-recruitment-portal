-- ==============================================================================
-- SEED DEMO PORTAL DATA (Oracle 23ai)
-- Guarantees all demo personas, companies, drives, and student records exist
-- ==============================================================================

SET DEFINE OFF;
SET SERVEROUTPUT ON;

-- 1. EMAIL_COMPANY & COMPANY (10 Companies)
INSERT INTO EMAIL_COMPANY (Email, Company_Name)
SELECT 'campus@tcs.com', 'Tata Consultancy Services' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM EMAIL_COMPANY WHERE Email = 'campus@tcs.com');

INSERT INTO EMAIL_COMPANY (Email, Company_Name)
SELECT 'talent@infosys.com', 'Infosys Limited' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM EMAIL_COMPANY WHERE Email = 'talent@infosys.com');

INSERT INTO EMAIL_COMPANY (Email, Company_Name)
SELECT 'careers@wipro.com', 'Wipro Enterprises' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM EMAIL_COMPANY WHERE Email = 'careers@wipro.com');

INSERT INTO EMAIL_COMPANY (Email, Company_Name)
SELECT 'indiarecruiting@microsoft.com', 'Microsoft India' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM EMAIL_COMPANY WHERE Email = 'indiarecruiting@microsoft.com');

INSERT INTO EMAIL_COMPANY (Email, Company_Name)
SELECT 'campus-in@amazon.com', 'Amazon Development Centre' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM EMAIL_COMPANY WHERE Email = 'campus-in@amazon.com');

INSERT INTO EMAIL_COMPANY (Email, Company_Name)
SELECT 'campus_recruitment@oracle.com', 'Oracle India Pvt Ltd' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM EMAIL_COMPANY WHERE Email = 'campus_recruitment@oracle.com');

INSERT INTO EMAIL_COMPANY (Email, Company_Name)
SELECT 'campuscareers@gs.com', 'Goldman Sachs Services' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM EMAIL_COMPANY WHERE Email = 'campuscareers@gs.com');

INSERT INTO EMAIL_COMPANY (Email, Company_Name)
SELECT 'talent_apac@cisco.com', 'Cisco Systems India' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM EMAIL_COMPANY WHERE Email = 'talent_apac@cisco.com');

INSERT INTO EMAIL_COMPANY (Email, Company_Name)
SELECT 'hiring@flipkart.com', 'Flipkart Internet Pvt Ltd' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM EMAIL_COMPANY WHERE Email = 'hiring@flipkart.com');

INSERT INTO EMAIL_COMPANY (Email, Company_Name)
SELECT 'earlycareers@zomato.com', 'Zomato Limited' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM EMAIL_COMPANY WHERE Email = 'earlycareers@zomato.com');

-- 2. COMPANY
INSERT INTO COMPANY (Company_Id, Email, Industry)
SELECT 'COM001', 'campus@tcs.com', 'IT Services & Consulting' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY WHERE Company_Id = 'COM001');

INSERT INTO COMPANY (Company_Id, Email, Industry)
SELECT 'COM002', 'talent@infosys.com', 'IT Services & Consulting' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY WHERE Company_Id = 'COM002');

INSERT INTO COMPANY (Company_Id, Email, Industry)
SELECT 'COM003', 'careers@wipro.com', 'IT Services & Consulting' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY WHERE Company_Id = 'COM003');

INSERT INTO COMPANY (Company_Id, Email, Industry)
SELECT 'COM004', 'indiarecruiting@microsoft.com', 'Software Products & Cloud' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY WHERE Company_Id = 'COM004');

INSERT INTO COMPANY (Company_Id, Email, Industry)
SELECT 'COM005', 'campus-in@amazon.com', 'E-Commerce & Cloud Computing' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY WHERE Company_Id = 'COM005');

INSERT INTO COMPANY (Company_Id, Email, Industry)
SELECT 'COM006', 'campus_recruitment@oracle.com', 'Enterprise Software & Database' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY WHERE Company_Id = 'COM006');

INSERT INTO COMPANY (Company_Id, Email, Industry)
SELECT 'COM007', 'campuscareers@gs.com', 'Financial Services & FinTech' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY WHERE Company_Id = 'COM007');

INSERT INTO COMPANY (Company_Id, Email, Industry)
SELECT 'COM008', 'talent_apac@cisco.com', 'Networking & Cyber Security' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY WHERE Company_Id = 'COM008');

INSERT INTO COMPANY (Company_Id, Email, Industry)
SELECT 'COM009', 'hiring@flipkart.com', 'E-Commerce & Retail Tech' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY WHERE Company_Id = 'COM009');

INSERT INTO COMPANY (Company_Id, Email, Industry)
SELECT 'COM010', 'earlycareers@zomato.com', 'FoodTech & Consumer Internet' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY WHERE Company_Id = 'COM010');

-- 3. COMPANY_PHONE
INSERT INTO COMPANY_PHONE (Company_Id, Phone_No)
SELECT 'COM001', '+91-22-67789999' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY_PHONE WHERE Company_Id = 'COM001' AND Phone_No = '+91-22-67789999');

INSERT INTO COMPANY_PHONE (Company_Id, Phone_No)
SELECT 'COM002', '+91-80-28520261' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY_PHONE WHERE Company_Id = 'COM002' AND Phone_No = '+91-80-28520261');

INSERT INTO COMPANY_PHONE (Company_Id, Phone_No)
SELECT 'COM003', '+91-80-28440011' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY_PHONE WHERE Company_Id = 'COM003' AND Phone_No = '+91-80-28440011');

INSERT INTO COMPANY_PHONE (Company_Id, Phone_No)
SELECT 'COM004', '+91-40-66930000' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY_PHONE WHERE Company_Id = 'COM004' AND Phone_No = '+91-40-66930000');

INSERT INTO COMPANY_PHONE (Company_Id, Phone_No)
SELECT 'COM005', '+91-80-41030000' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY_PHONE WHERE Company_Id = 'COM005' AND Phone_No = '+91-80-41030000');

INSERT INTO COMPANY_PHONE (Company_Id, Phone_No)
SELECT 'COM006', '+91-80-41084000' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY_PHONE WHERE Company_Id = 'COM006' AND Phone_No = '+91-80-41084000');

INSERT INTO COMPANY_PHONE (Company_Id, Phone_No)
SELECT 'COM007', '+91-80-41278000' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY_PHONE WHERE Company_Id = 'COM007' AND Phone_No = '+91-80-41278000');

INSERT INTO COMPANY_PHONE (Company_Id, Phone_No)
SELECT 'COM008', '+91-80-44260000' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY_PHONE WHERE Company_Id = 'COM008' AND Phone_No = '+91-80-44260000');

INSERT INTO COMPANY_PHONE (Company_Id, Phone_No)
SELECT 'COM009', '+91-80-67981111' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY_PHONE WHERE Company_Id = 'COM009' AND Phone_No = '+91-80-67981111');

INSERT INTO COMPANY_PHONE (Company_Id, Phone_No)
SELECT 'COM010', '+91-124-4268000' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM COMPANY_PHONE WHERE Company_Id = 'COM010' AND Phone_No = '+91-124-4268000');

-- 4. JOB_COMPANY
INSERT INTO JOB_COMPANY (Job_Title, Company_Id)
SELECT 'Associate Software Engineer', 'COM001' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM JOB_COMPANY WHERE Job_Title = 'Associate Software Engineer');

INSERT INTO JOB_COMPANY (Job_Title, Company_Id)
SELECT 'Systems Engineer - Specialist', 'COM002' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM JOB_COMPANY WHERE Job_Title = 'Systems Engineer - Specialist');

INSERT INTO JOB_COMPANY (Job_Title, Company_Id)
SELECT 'Project Engineer', 'COM003' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM JOB_COMPANY WHERE Job_Title = 'Project Engineer');

INSERT INTO JOB_COMPANY (Job_Title, Company_Id)
SELECT 'Software Development Engineer I', 'COM004' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM JOB_COMPANY WHERE Job_Title = 'Software Development Engineer I');

INSERT INTO JOB_COMPANY (Job_Title, Company_Id)
SELECT 'Cloud Support Associate', 'COM005' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM JOB_COMPANY WHERE Job_Title = 'Cloud Support Associate');

INSERT INTO JOB_COMPANY (Job_Title, Company_Id)
SELECT 'Associate Applications Engineer', 'COM006' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM JOB_COMPANY WHERE Job_Title = 'Associate Applications Engineer');

INSERT INTO JOB_COMPANY (Job_Title, Company_Id)
SELECT 'Technology Analyst', 'COM007' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM JOB_COMPANY WHERE Job_Title = 'Technology Analyst');

INSERT INTO JOB_COMPANY (Job_Title, Company_Id)
SELECT 'Network Consulting Engineer', 'COM008' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM JOB_COMPANY WHERE Job_Title = 'Network Consulting Engineer');

INSERT INTO JOB_COMPANY (Job_Title, Company_Id)
SELECT 'Backend Software Engineer', 'COM009' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM JOB_COMPANY WHERE Job_Title = 'Backend Software Engineer');

INSERT INTO JOB_COMPANY (Job_Title, Company_Id)
SELECT 'Software Engineer - Platform', 'COM010' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM JOB_COMPANY WHERE Job_Title = 'Software Engineer - Platform');

-- 5. PLACEMENT_DRIVE
MERGE INTO PLACEMENT_DRIVE pd
USING (
    SELECT 'DRV001' AS did, 'Software Development Engineer I' AS title, 8.50 AS cpa,
           'Architect and build scalable distributed cloud systems, high-throughput microservices, and AI-enabled platforms at Microsoft India.' AS jdesc,
           'Bengaluru / Hyderabad' AS loc, 'BCE,BCT,BDS' AS br,
           'Round 1: Online Coding (DSA) | Round 2: Tech DSA & System Design | Round 3: Fitment' AS sel,
           45.00 AS ctc, TO_DATE('2026-11-15', 'YYYY-MM-DD') AS ddate, TO_DATE('2026-11-10', 'YYYY-MM-DD') AS dline, 15 AS op FROM DUAL UNION ALL
    SELECT 'DRV002', 'Associate Software Engineer', 6.50,
           'Develop and deploy enterprise software solutions, digital transformation workflows, and cloud-native applications for global clients.',
           'Pan India (Chennai / Bengaluru / Pune)', 'BCE,BCT,BEC,BEE,BME,BCV',
           'Round 1: TCS NQT Assessment | Round 2: Technical Interview | Round 3: HR Interview',
           7.50, TO_DATE('2026-11-20', 'YYYY-MM-DD'), TO_DATE('2026-11-15', 'YYYY-MM-DD'), 50 FROM DUAL UNION ALL
    SELECT 'DRV003', 'Systems Engineer - Specialist', 7.00,
           'Design and implement full-stack enterprise applications, microservices, and automated data pipelines.',
           'Bengaluru / Mysuru / Hyderabad', 'BCE,BCT,BEC',
           'Round 1: InfyTQ Online Test | Round 2: Technical Interview | Round 3: Leadership Assessment',
           9.50, TO_DATE('2026-11-25', 'YYYY-MM-DD'), TO_DATE('2026-11-18', 'YYYY-MM-DD'), 25 FROM DUAL UNION ALL
    SELECT 'DRV004', 'Cloud Support Associate', 7.50,
           'Provide architectural and operational support for AWS cloud services, serverless architectures, and distributed systems.',
           'Bengaluru / Hyderabad', 'BCE,BCT,BEC,BEE',
           'Round 1: Amazon Online Assessment | Round 2: Technical Deep Dive | Round 3: Leadership Principles',
           20.00, TO_DATE('2026-12-01', 'YYYY-MM-DD'), TO_DATE('2026-11-25', 'YYYY-MM-DD'), 20 FROM DUAL UNION ALL
    SELECT 'DRV005', 'Associate Applications Engineer', 8.00,
           'Engineer cutting-edge cloud infrastructure, Autonomous Database tools, and enterprise SaaS solutions at Oracle.',
           'Bengaluru / Hyderabad', 'BCE,BCT,BDS',
           'Round 1: Oracle Technical Assessment | Round 2: Systems & DBMS Interview | Round 3: Director Discussion',
           22.00, TO_DATE('2026-12-05', 'YYYY-MM-DD'), TO_DATE('2026-11-30', 'YYYY-MM-DD'), 12 FROM DUAL UNION ALL
    SELECT 'DRV006', 'Technology Analyst', 8.50,
           'Build low-latency trading engines, financial transaction systems, and resilient quantitative analytics pipelines.',
           'Bengaluru', 'BCE,BCT,BDS',
           'Round 1: HackerRank Technical Assessment | Round 2: Dual Tech Rounds | Round 3: VP Fitment',
           28.00, TO_DATE('2026-12-10', 'YYYY-MM-DD'), TO_DATE('2026-12-01', 'YYYY-MM-DD'), 10 FROM DUAL UNION ALL
    SELECT 'DRV007', 'Backend Software Engineer', 8.00,
           'Build high-scale e-commerce transactional platforms handling millions of queries per second during Big Billion Days.',
           'Bengaluru', 'BCE,BCT,BDS',
           'Round 1: Machine Coding Round | Round 2: Problem Solving & DSA | Round 3: Fitment',
           26.00, TO_DATE('2026-12-15', 'YYYY-MM-DD'), TO_DATE('2026-12-05', 'YYYY-MM-DD'), 18 FROM DUAL UNION ALL
    SELECT 'DRV008', 'Network Consulting Engineer', 7.50,
           'Design, test, and implement mission-critical enterprise network architectures and cloud security frameworks.',
           'Bengaluru / Chennai', 'BCE,BCT,BEC,BEE',
           'Round 1: Cisco Online Test | Round 2: Tech Interview | Round 3: HR Round',
           18.00, TO_DATE('2026-12-20', 'YYYY-MM-DD'), TO_DATE('2026-12-10', 'YYYY-MM-DD'), 15 FROM DUAL
) src ON (pd.Drive_Id = src.did)
WHEN MATCHED THEN
    UPDATE SET pd.Job_Title = src.title, pd.Min_CGPA = src.cpa, pd.Job_Description = src.jdesc,
               pd.Location = src.loc, pd.Eligible_Branches = src.br, pd.Selection_Process = src.sel,
               pd.CTC = src.ctc, pd.Drive_Date = src.ddate, pd.Application_Deadline = src.dline, pd.Openings = src.op
WHEN NOT MATCHED THEN
    INSERT (Drive_Id, Job_Title, Min_CGPA, Job_Description, Location, Eligible_Branches, Selection_Process, CTC, Drive_Date, Application_Deadline, Openings)
    VALUES (src.did, src.title, src.cpa, src.jdesc, src.loc, src.br, src.sel, src.ctc, src.ddate, src.dline, src.op);

-- 6. STUDENTS (STU001 as Achyut Ranaut / 25BCE1799, plus other students)
MERGE INTO STUDENT s
USING (
    SELECT 'STU001' AS sid, 'Achyut Ranaut' AS name, 'achyut.ranaut2025@vitstudent.ac.in' AS email,
           'VIT Chennai Vandalur – Kelambakkam Road' AS street, 'Chennai' AS city, 'Tamil Nadu' AS state,
           TO_DATE('2003-04-15', 'YYYY-MM-DD') AS dob, 9.25 AS cgpa, 'Computer Science & Engineering' AS branch,
           '25BCE1799' AS reg, 'BCE' AS pid, 'NOT_PLACED' AS ps FROM DUAL UNION ALL
    SELECT 'STU002', 'Diya Patel', 'diya.patel@vitstudent.ac.in',
           '45 CG Road', 'Ahmedabad', 'Gujarat',
           TO_DATE('2003-08-20', 'YYYY-MM-DD'), 8.80, 'Information Technology',
           '21BCT1002', 'BCT', 'NOT_PLACED' FROM DUAL UNION ALL
    SELECT 'STU003', 'Rohan Iyer', 'achyut.ranaut@vitstudent.ac.in',
           '78 Anna Salai', 'Chennai', 'Tamil Nadu',
           TO_DATE('2002-11-12', 'YYYY-MM-DD'), 8.45, 'Computer Science & Engineering',
           '25BCE1790', 'BCE', 'NOT_PLACED' FROM DUAL UNION ALL
    SELECT 'STU004', 'Ananya Gupta', 'ananya.gupta@vitstudent.ac.in',
           '23 Park Street', 'Kolkata', 'West Bengal',
           TO_DATE('2003-02-18', 'YYYY-MM-DD'), 9.50, 'Data Science',
           '21BDS1004', 'BDS', 'NOT_PLACED' FROM DUAL UNION ALL
    SELECT 'STU005', 'Ishaan Verma', 'ishaan.verma@vitstudent.ac.in',
           '89 Hazratganj', 'Lucknow', 'Uttar Pradesh',
           TO_DATE('2003-06-30', 'YYYY-MM-DD'), 7.90, 'Electronics & Communication',
           '21BEC1005', 'BEC', 'NOT_PLACED' FROM DUAL
) src ON (s.Student_Id = src.sid)
WHEN MATCHED THEN
    UPDATE SET s.Name = src.name, s.Email = src.email, s.Street = src.street, s.City = src.city,
               s.State = src.state, s.DOB = src.dob, s.CGPA = src.cgpa, s.Branch = src.branch,
               s.Registration_No = src.reg, s.Program_Id = src.pid, s.Placement_Status = src.ps
WHEN NOT MATCHED THEN
    INSERT (Student_Id, Name, Email, Street, City, State, DOB, CGPA, Branch, Registration_No, Program_Id, Placement_Status)
    VALUES (src.sid, src.name, src.email, src.street, src.city, src.state, src.dob, src.cgpa, src.branch, src.reg, src.pid, src.ps);

-- 7. STUDENT_PHONE
INSERT INTO STUDENT_PHONE (Student_Id, Phone)
SELECT 'STU001', '+91-8219631530' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM STUDENT_PHONE WHERE Student_Id = 'STU001' AND Phone = '+91-8219631530');

INSERT INTO STUDENT_PHONE (Student_Id, Phone)
SELECT 'STU002', '+91-9876543212' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM STUDENT_PHONE WHERE Student_Id = 'STU002' AND Phone = '+91-9876543212');

INSERT INTO STUDENT_PHONE (Student_Id, Phone)
SELECT 'STU003', '+91-9876543213' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM STUDENT_PHONE WHERE Student_Id = 'STU003' AND Phone = '+91-9876543213');

-- 8. STUDENT_SKILL
INSERT INTO STUDENT_SKILL (Student_Id, Skill_Name, Proficiency)
SELECT 'STU001', 'Java', 'ADVANCED' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM STUDENT_SKILL WHERE Student_Id = 'STU001' AND Skill_Name = 'Java');

INSERT INTO STUDENT_SKILL (Student_Id, Skill_Name, Proficiency)
SELECT 'STU001', 'Spring Boot', 'ADVANCED' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM STUDENT_SKILL WHERE Student_Id = 'STU001' AND Skill_Name = 'Spring Boot');

INSERT INTO STUDENT_SKILL (Student_Id, Skill_Name, Proficiency)
SELECT 'STU001', 'React', 'INTERMEDIATE' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM STUDENT_SKILL WHERE Student_Id = 'STU001' AND Skill_Name = 'React');

INSERT INTO STUDENT_SKILL (Student_Id, Skill_Name, Proficiency)
SELECT 'STU001', 'Neo4j', 'INTERMEDIATE' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM STUDENT_SKILL WHERE Student_Id = 'STU001' AND Skill_Name = 'Neo4j');

INSERT INTO STUDENT_SKILL (Student_Id, Skill_Name, Proficiency)
SELECT 'STU001', 'Docker', 'INTERMEDIATE' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM STUDENT_SKILL WHERE Student_Id = 'STU001' AND Skill_Name = 'Docker');

-- 9. PORTAL_USER (All Demo Accounts with BCrypt hash for 'password123', admin has 'admin123')
MERGE INTO PORTAL_USER pu
USING (
    -- Admin (USR005 / admin)
    SELECT 'USR005' AS uid, 'admin' AS uname,
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK' AS phash,
           'ROLE_ADMIN' AS role, 'ADMIN01' AS refid,
           'System Administrator' AS fname, 'admin@vit.ac.in' AS email FROM DUAL UNION ALL
    -- Student 25bce1799 (USR_STU001)
    SELECT 'USR_STU001', '25bce1799',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_STUDENT', 'STU001',
           'Achyut Ranaut', 'achyut.ranaut2025@vitstudent.ac.in' FROM DUAL UNION ALL
    -- Student student1 (USR001)
    SELECT 'USR001', 'student1',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_STUDENT', 'STU001',
           'Achyut Ranaut', 'achyut.ranaut2025@vitstudent.ac.in' FROM DUAL UNION ALL
    -- Student student2 (USR002)
    SELECT 'USR002', 'student2',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_STUDENT', 'STU002',
           'Diya Patel', 'diya.patel@vitstudent.ac.in' FROM DUAL UNION ALL
    -- Recruiter 1 (USR003 / recruiter1 - Microsoft)
    SELECT 'USR003', 'recruiter1',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_RECRUITER', 'COM004',
           'Vikram Singh', 'vikram.singh@microsoft.com' FROM DUAL UNION ALL
    -- Recruiter 2 (USR004 / recruiter2 - TCS)
    SELECT 'USR004', 'recruiter2',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_RECRUITER', 'COM001',
           'Neha Sharma', 'neha.sharma@tcs.com' FROM DUAL UNION ALL
    -- Corporate recruiters for other companies
    SELECT 'USR_COM002', 'infosys_recruiter',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_RECRUITER', 'COM002',
           'Infosys Recruiter', 'talent@infosys.com' FROM DUAL UNION ALL
    SELECT 'USR_COM003', 'wipro_recruiter',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_RECRUITER', 'COM003',
           'Wipro Recruiter', 'careers@wipro.com' FROM DUAL UNION ALL
    SELECT 'USR_COM005', 'amazon_recruiter',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_RECRUITER', 'COM005',
           'Amazon Recruiter', 'campus-in@amazon.com' FROM DUAL UNION ALL
    SELECT 'USR_COM006', 'oracle_recruiter',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_RECRUITER', 'COM006',
           'Oracle Recruiter', 'campus_recruitment@oracle.com' FROM DUAL
) src ON (pu.Username = src.uname)
WHEN MATCHED THEN
    UPDATE SET pu.Password_Hash = src.phash, pu.Role = src.role, pu.Reference_Id = src.refid,
               pu.Full_Name = src.fname, pu.Email = src.email
WHEN NOT MATCHED THEN
    INSERT (User_Id, Username, Password_Hash, Role, Reference_Id, Full_Name, Email)
    VALUES (src.uid, src.uname, src.phash, src.role, src.refid, src.fname, src.email);

-- 10. RECRUITER_COMPANY
MERGE INTO RECRUITER_COMPANY rc
USING (
    SELECT 'USR003' AS uid, 'COM004' AS cid FROM DUAL UNION ALL
    SELECT 'USR003', 'COM001' FROM DUAL UNION ALL
    SELECT 'USR004', 'COM001' FROM DUAL UNION ALL
    SELECT 'USR_COM002', 'COM002' FROM DUAL UNION ALL
    SELECT 'USR_COM003', 'COM003' FROM DUAL UNION ALL
    SELECT 'USR_COM005', 'COM005' FROM DUAL UNION ALL
    SELECT 'USR_COM006', 'COM006' FROM DUAL
) src ON (rc.User_Id = src.uid AND rc.Company_Id = src.cid)
WHEN NOT MATCHED THEN
    INSERT (User_Id, Company_Id) VALUES (src.uid, src.cid);

-- 11. STUDENT_DAILY_ROUTINE_DRIVE & APPLICATION (Sample application for 25bce1799)
MERGE INTO STUDENT_DAILY_ROUTINE_DRIVE sdrd
USING (
    SELECT 'STU001' AS sid, TO_DATE('2026-11-01', 'YYYY-MM-DD') AS adate, 'DRV001' AS did FROM DUAL
) src ON (sdrd.Student_Id = src.sid AND sdrd.Drive_Id = src.did)
WHEN NOT MATCHED THEN
    INSERT (Student_Id, Apply_Date, Drive_Id) VALUES (src.sid, src.adate, src.did);

MERGE INTO APPLICATION a
USING (
    SELECT 'APP001' AS aid, 'STU001' AS sid, TO_DATE('2026-11-01', 'YYYY-MM-DD') AS adate, 'APPLIED' AS st, 'DRV001' AS did FROM DUAL
) src ON (a.Application_Id = src.aid)
WHEN MATCHED THEN
    UPDATE SET a.Status = src.st
WHEN NOT MATCHED THEN
    INSERT (Application_Id, Student_Id, Apply_Date, Status, Drive_Id) VALUES (src.aid, src.sid, src.adate, src.st, src.did);

COMMIT;
PROMPT Demo portal data seeding successfully applied!
