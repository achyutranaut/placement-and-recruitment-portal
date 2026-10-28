-- ==============================================================================
-- SEED DEMO PORTAL DATA (Oracle Database 23ai Free)
-- PlaceX Student Placement & Recruitment Portal
-- Populates coherent demonstration dataset consistent across Oracle, Neo4j,
-- SQL/Cypher Workspace, Graph Visualizer, and Analytics.
--
-- Target Dataset:
--   • 25 Students (STU001..STU025 across BCE, BCT, BDS, BEC, BME, BEE, BCV)
--   • 10 Companies (COM001..COM010)
--   • 12 Placement Drives (DRV001..DRV012)
--   • 38 Applications across all valid pipeline stages
--   • 35 Interview records across rounds 1, 2, 3
--   • 9 Released Offers (7 Accepted, 1 Offered, 1 Declined)
--   • 7 Verified Placed Students
--   • Full relational junction tables (DRIVE_ELIGIBILITY, REGISTERS, STUDENT_SKILL)
--
-- 100% Idempotent, safe to re-execute without duplicating records.
-- Fully honors Oracle state machine triggers and constraints.
-- ==============================================================================

SET DEFINE OFF;
SET SERVEROUTPUT ON SIZE UNLIMITED;

PROMPT ====================================================================
PROMPT Initializing PlaceX Demonstration Mock Data Seeding...
PROMPT ====================================================================

-- ------------------------------------------------------------------------------
-- 1. EMAIL_COMPANY & COMPANY (10 Corporate Partners)
-- ------------------------------------------------------------------------------
PROMPT [1/11] Seeding Corporate Partners & Recruitment Contacts...

MERGE INTO EMAIL_COMPANY ec
USING (
    SELECT 'campus@tcs.com' AS em, 'Tata Consultancy Services' AS cname FROM DUAL UNION ALL
    SELECT 'talent@infosys.com', 'Infosys Limited' FROM DUAL UNION ALL
    SELECT 'careers@wipro.com', 'Wipro Enterprises' FROM DUAL UNION ALL
    SELECT 'indiarecruiting@microsoft.com', 'Microsoft India' FROM DUAL UNION ALL
    SELECT 'campus-in@amazon.com', 'Amazon Development Centre' FROM DUAL UNION ALL
    SELECT 'campus_recruitment@oracle.com', 'Oracle India Pvt Ltd' FROM DUAL UNION ALL
    SELECT 'campuscareers@gs.com', 'Goldman Sachs Services' FROM DUAL UNION ALL
    SELECT 'talent_apac@cisco.com', 'Cisco Systems India' FROM DUAL UNION ALL
    SELECT 'hiring@flipkart.com', 'Flipkart Internet Pvt Ltd' FROM DUAL UNION ALL
    SELECT 'earlycareers@zomato.com', 'Zomato Limited' FROM DUAL
) src ON (ec.Email = src.em)
WHEN MATCHED THEN UPDATE SET ec.Company_Name = src.cname
WHEN NOT MATCHED THEN INSERT (Email, Company_Name) VALUES (src.em, src.cname);

MERGE INTO COMPANY c
USING (
    SELECT 'COM001' AS cid, 'campus@tcs.com' AS em, 'IT Services & Consulting' AS ind FROM DUAL UNION ALL
    SELECT 'COM002', 'talent@infosys.com', 'IT Services & Consulting' FROM DUAL UNION ALL
    SELECT 'COM003', 'careers@wipro.com', 'IT Services & Consulting' FROM DUAL UNION ALL
    SELECT 'COM004', 'indiarecruiting@microsoft.com', 'Software Products & Cloud' FROM DUAL UNION ALL
    SELECT 'COM005', 'campus-in@amazon.com', 'E-Commerce & Cloud Computing' FROM DUAL UNION ALL
    SELECT 'COM006', 'campus_recruitment@oracle.com', 'Enterprise Software & Database' FROM DUAL UNION ALL
    SELECT 'COM007', 'campuscareers@gs.com', 'Financial Services & FinTech' FROM DUAL UNION ALL
    SELECT 'COM008', 'talent_apac@cisco.com', 'Networking & Cyber Security' FROM DUAL UNION ALL
    SELECT 'COM009', 'hiring@flipkart.com', 'E-Commerce & Retail Tech' FROM DUAL UNION ALL
    SELECT 'COM010', 'earlycareers@zomato.com', 'FoodTech & Consumer Internet' FROM DUAL
) src ON (c.Company_Id = src.cid)
WHEN MATCHED THEN UPDATE SET c.Email = src.em, c.Industry = src.ind
WHEN NOT MATCHED THEN INSERT (Company_Id, Email, Industry) VALUES (src.cid, src.em, src.ind);

-- COMPANY_PHONE
MERGE INTO COMPANY_PHONE cp
USING (
    SELECT 'COM001' AS cid, '+91-22-67789999' AS ph FROM DUAL UNION ALL
    SELECT 'COM002', '+91-80-28520261' FROM DUAL UNION ALL
    SELECT 'COM003', '+91-80-28440011' FROM DUAL UNION ALL
    SELECT 'COM004', '+91-40-66930000' FROM DUAL UNION ALL
    SELECT 'COM005', '+91-80-41030000' FROM DUAL UNION ALL
    SELECT 'COM006', '+91-80-41084000' FROM DUAL UNION ALL
    SELECT 'COM007', '+91-80-41278000' FROM DUAL UNION ALL
    SELECT 'COM008', '+91-80-44260000' FROM DUAL UNION ALL
    SELECT 'COM009', '+91-80-67981111' FROM DUAL UNION ALL
    SELECT 'COM010', '+91-124-4268000' FROM DUAL
) src ON (cp.Company_Id = src.cid AND cp.Phone_No = src.ph)
WHEN NOT MATCHED THEN INSERT (Company_Id, Phone_No) VALUES (src.cid, src.ph);

-- ------------------------------------------------------------------------------
-- 2. JOB_COMPANY & PLACEMENT_DRIVE (12 Active Drives across 10 Companies)
-- ------------------------------------------------------------------------------
PROMPT [2/11] Seeding Job Catalog & Campus Placement Drives...

MERGE INTO JOB_COMPANY jc
USING (
    SELECT 'Associate Software Engineer' AS jt, 'COM001' AS cid FROM DUAL UNION ALL
    SELECT 'Systems Engineer - Specialist', 'COM002' FROM DUAL UNION ALL
    SELECT 'Project Engineer', 'COM003' FROM DUAL UNION ALL
    SELECT 'Software Development Engineer I', 'COM004' FROM DUAL UNION ALL
    SELECT 'Cloud Solution Architect', 'COM004' FROM DUAL UNION ALL
    SELECT 'Cloud Support Associate', 'COM005' FROM DUAL UNION ALL
    SELECT 'Associate Applications Engineer', 'COM006' FROM DUAL UNION ALL
    SELECT 'Member of Technical Staff', 'COM006' FROM DUAL UNION ALL
    SELECT 'Technology Analyst', 'COM007' FROM DUAL UNION ALL
    SELECT 'Network Consulting Engineer', 'COM008' FROM DUAL UNION ALL
    SELECT 'Software Engineer - Systems', 'COM008' FROM DUAL UNION ALL
    SELECT 'Backend Software Engineer', 'COM009' FROM DUAL UNION ALL
    SELECT 'Software Engineer - Platform', 'COM010' FROM DUAL
) src ON (jc.Job_Title = src.jt)
WHEN MATCHED THEN UPDATE SET jc.Company_Id = src.cid
WHEN NOT MATCHED THEN INSERT (Job_Title, Company_Id) VALUES (src.jt, src.cid);

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
           16.00, TO_DATE('2026-12-01', 'YYYY-MM-DD'), TO_DATE('2026-11-25', 'YYYY-MM-DD'), 20 FROM DUAL UNION ALL
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
           18.00, TO_DATE('2026-12-20', 'YYYY-MM-DD'), TO_DATE('2026-12-10', 'YYYY-MM-DD'), 15 FROM DUAL UNION ALL
    SELECT 'DRV009', 'Project Engineer', 6.50,
           'Deliver agile cloud migration and full stack enterprise software development across financial and telecom domains.',
           'Bengaluru / Pune', 'BCE,BCT,BEC,BEE,BME,BCV',
           'Round 1: Online Aptitude & Coding | Round 2: Technical Interview | Round 3: HR Discussion',
           6.50, TO_DATE('2026-12-22', 'YYYY-MM-DD'), TO_DATE('2026-12-12', 'YYYY-MM-DD'), 40 FROM DUAL UNION ALL
    SELECT 'DRV010', 'Software Engineer - Platform', 7.80,
           'Build real-time hyperlocal logistics, real-time tracking microservices, and high-concurrency order dispatch systems.',
           'Gurugram', 'BCE,BCT,BDS',
           'Round 1: Coding Challenge | Round 2: Architecture & Algorithms | Round 3: Culture Fit',
           14.00, TO_DATE('2026-12-28', 'YYYY-MM-DD'), TO_DATE('2026-12-18', 'YYYY-MM-DD'), 8 FROM DUAL UNION ALL
    SELECT 'DRV011', 'Cloud Solution Architect', 8.20,
           'Architect hybrid cloud solutions, generative AI workload migrations, and mission-critical cloud deployments on Microsoft Azure.',
           'Bengaluru / Hyderabad', 'BCE,BCT,BDS,BEC',
           'Round 1: Cloud & System Architecture | Round 2: Technical Interview | Round 3: Partner Fitment',
           38.00, TO_DATE('2027-01-05', 'YYYY-MM-DD'), TO_DATE('2026-12-24', 'YYYY-MM-DD'), 8 FROM DUAL UNION ALL
    SELECT 'DRV012', 'Member of Technical Staff', 8.50,
           'Develop kernel-level storage systems, distributed cluster caching engines, and next-generation Autonomous Database features.',
           'Bengaluru', 'BCE,BCT,BDS',
           'Round 1: Systems Coding | Round 2: Operating Systems & Storage | Round 3: VP Interview',
           32.00, TO_DATE('2027-01-10', 'YYYY-MM-DD'), TO_DATE('2026-12-28', 'YYYY-MM-DD'), 6 FROM DUAL
) src ON (pd.Drive_Id = src.did)
WHEN MATCHED THEN
    UPDATE SET pd.Job_Title = src.title, pd.Min_CGPA = src.cpa, pd.Job_Description = src.jdesc,
               pd.Location = src.loc, pd.Eligible_Branches = src.br, pd.Selection_Process = src.sel,
               pd.CTC = src.ctc, pd.Drive_Date = src.ddate, pd.Application_Deadline = src.dline, pd.Openings = src.op
WHEN NOT MATCHED THEN
    INSERT (Drive_Id, Job_Title, Min_CGPA, Job_Description, Location, Eligible_Branches, Selection_Process, CTC, Drive_Date, Application_Deadline, Openings)
    VALUES (src.did, src.title, src.cpa, src.jdesc, src.loc, src.br, src.sel, src.ctc, src.ddate, src.dline, src.op);

-- ------------------------------------------------------------------------------
-- 3. DRIVE_ELIGIBILITY JUNCTION (Drives <-> Canonical Programs)
-- ------------------------------------------------------------------------------
PROMPT [3/11] Mapping Placement Drive Academic Eligibility Criteria...

MERGE INTO DRIVE_ELIGIBILITY de
USING (
    -- DRV001 (Microsoft SDE I: BCE, BCT, BDS)
    SELECT 'DRV001' AS did, 'BCE' AS pid FROM DUAL UNION ALL
    SELECT 'DRV001', 'BCT' FROM DUAL UNION ALL
    SELECT 'DRV001', 'BDS' FROM DUAL UNION ALL
    -- DRV002 (TCS: All branches)
    SELECT 'DRV002', 'BCE' FROM DUAL UNION ALL
    SELECT 'DRV002', 'BCT' FROM DUAL UNION ALL
    SELECT 'DRV002', 'BEC' FROM DUAL UNION ALL
    SELECT 'DRV002', 'BEE' FROM DUAL UNION ALL
    SELECT 'DRV002', 'BME' FROM DUAL UNION ALL
    SELECT 'DRV002', 'BCV' FROM DUAL UNION ALL
    -- DRV003 (Infosys: BCE, BCT, BEC)
    SELECT 'DRV003', 'BCE' FROM DUAL UNION ALL
    SELECT 'DRV003', 'BCT' FROM DUAL UNION ALL
    SELECT 'DRV003', 'BEC' FROM DUAL UNION ALL
    -- DRV004 (Amazon: BCE, BCT, BEC, BEE)
    SELECT 'DRV004', 'BCE' FROM DUAL UNION ALL
    SELECT 'DRV004', 'BCT' FROM DUAL UNION ALL
    SELECT 'DRV004', 'BEC' FROM DUAL UNION ALL
    SELECT 'DRV004', 'BEE' FROM DUAL UNION ALL
    -- DRV005 (Oracle Apps: BCE, BCT, BDS)
    SELECT 'DRV005', 'BCE' FROM DUAL UNION ALL
    SELECT 'DRV005', 'BCT' FROM DUAL UNION ALL
    SELECT 'DRV005', 'BDS' FROM DUAL UNION ALL
    -- DRV006 (Goldman Sachs: BCE, BCT, BDS)
    SELECT 'DRV006', 'BCE' FROM DUAL UNION ALL
    SELECT 'DRV006', 'BCT' FROM DUAL UNION ALL
    SELECT 'DRV006', 'BDS' FROM DUAL UNION ALL
    -- DRV007 (Flipkart: BCE, BCT, BDS)
    SELECT 'DRV007', 'BCE' FROM DUAL UNION ALL
    SELECT 'DRV007', 'BCT' FROM DUAL UNION ALL
    SELECT 'DRV007', 'BDS' FROM DUAL UNION ALL
    -- DRV008 (Cisco: BCE, BCT, BEC, BEE)
    SELECT 'DRV008', 'BCE' FROM DUAL UNION ALL
    SELECT 'DRV008', 'BCT' FROM DUAL UNION ALL
    SELECT 'DRV008', 'BEC' FROM DUAL UNION ALL
    SELECT 'DRV008', 'BEE' FROM DUAL UNION ALL
    -- DRV009 (Wipro: All branches)
    SELECT 'DRV009', 'BCE' FROM DUAL UNION ALL
    SELECT 'DRV009', 'BCT' FROM DUAL UNION ALL
    SELECT 'DRV009', 'BEC' FROM DUAL UNION ALL
    SELECT 'DRV009', 'BEE' FROM DUAL UNION ALL
    SELECT 'DRV009', 'BME' FROM DUAL UNION ALL
    SELECT 'DRV009', 'BCV' FROM DUAL UNION ALL
    -- DRV010 (Zomato: BCE, BCT, BDS)
    SELECT 'DRV010', 'BCE' FROM DUAL UNION ALL
    SELECT 'DRV010', 'BCT' FROM DUAL UNION ALL
    SELECT 'DRV010', 'BDS' FROM DUAL UNION ALL
    -- DRV011 (Microsoft Cloud: BCE, BCT, BDS, BEC)
    SELECT 'DRV011', 'BCE' FROM DUAL UNION ALL
    SELECT 'DRV011', 'BCT' FROM DUAL UNION ALL
    SELECT 'DRV011', 'BDS' FROM DUAL UNION ALL
    SELECT 'DRV011', 'BEC' FROM DUAL UNION ALL
    -- DRV012 (Oracle MTS: BCE, BCT, BDS)
    SELECT 'DRV012', 'BCE' FROM DUAL UNION ALL
    SELECT 'DRV012', 'BCT' FROM DUAL UNION ALL
    SELECT 'DRV012', 'BDS' FROM DUAL
) src ON (de.Drive_Id = src.did AND de.Program_Id = src.pid)
WHEN NOT MATCHED THEN INSERT (Drive_Id, Program_Id) VALUES (src.did, src.pid);

-- ------------------------------------------------------------------------------
-- 4. INTERVIEWER_ROUND (9 University & Corporate Evaluation Experts)
-- ------------------------------------------------------------------------------
PROMPT [4/11] Seeding Interview Evaluation Panel & Rounds...

MERGE INTO INTERVIEWER_ROUND ir
USING (
    SELECT 'Dr. Rajesh Sundaram' AS iname, 1 AS rno FROM DUAL UNION ALL
    SELECT 'Priya Ramanathan', 1 FROM DUAL UNION ALL
    SELECT 'Vikram Malhotra', 1 FROM DUAL UNION ALL
    SELECT 'Vikramaditya Bose', 2 FROM DUAL UNION ALL
    SELECT 'Ananya Chatterjee', 2 FROM DUAL UNION ALL
    SELECT 'Sangeeta Rao', 2 FROM DUAL UNION ALL
    SELECT 'Suresh Venkataraman', 3 FROM DUAL UNION ALL
    SELECT 'Kavita Menon', 3 FROM DUAL UNION ALL
    SELECT 'Arvind Swamy', 3 FROM DUAL
) src ON (ir.Interviewer_Name = src.iname)
WHEN MATCHED THEN UPDATE SET ir.Interview_Round_No = src.rno
WHEN NOT MATCHED THEN INSERT (Interviewer_Name, Interview_Round_No) VALUES (src.iname, src.rno);

-- ------------------------------------------------------------------------------
-- 5. STUDENTS (25 Candidates across Engineering & Sciences)
-- ------------------------------------------------------------------------------
PROMPT [5/11] Seeding 25 Student Personas with Academic Records...

MERGE INTO STUDENT s
USING (
    SELECT 'STU001' AS sid, 'Achyut Ranaut' AS name, 'achyut.ranaut2025@vitstudent.ac.in' AS email,
           'VIT Chennai Vandalur – Kelambakkam Road' AS street, 'Chennai' AS city, 'Tamil Nadu' AS state,
           TO_DATE('2003-04-15', 'YYYY-MM-DD') AS dob, 9.25 AS cgpa, 'Computer Science & Engineering' AS branch,
           '25BCE1799' AS reg, 'BCE' AS pid FROM DUAL UNION ALL
    SELECT 'STU002', 'Diya Patel', 'diya.patel@vitstudent.ac.in',
           '45 CG Road', 'Ahmedabad', 'Gujarat',
           TO_DATE('2003-08-20', 'YYYY-MM-DD'), 8.80, 'Information Technology',
           '21BCT1002', 'BCT' FROM DUAL UNION ALL
    SELECT 'STU003', 'Rohan Iyer', 'achyut.ranaut@vitstudent.ac.in',
           '78 Anna Salai', 'Chennai', 'Tamil Nadu',
           TO_DATE('2002-11-12', 'YYYY-MM-DD'), 8.45, 'Computer Science & Engineering',
           '25BCE1790', 'BCE' FROM DUAL UNION ALL
    SELECT 'STU004', 'Ananya Gupta', 'ananya.gupta@vitstudent.ac.in',
           '23 Park Street', 'Kolkata', 'West Bengal',
           TO_DATE('2003-02-18', 'YYYY-MM-DD'), 9.50, 'Data Science',
           '21BDS1004', 'BDS' FROM DUAL UNION ALL
    SELECT 'STU005', 'Ishaan Verma', 'ishaan.verma@vitstudent.ac.in',
           '89 Hazratganj', 'Lucknow', 'Uttar Pradesh',
           TO_DATE('2003-06-30', 'YYYY-MM-DD'), 7.90, 'Electronics & Communication',
           '21BEC1005', 'BEC' FROM DUAL UNION ALL
    SELECT 'STU006', 'Priya Nair', 'priya.nair.demo@vitstudent.ac.in',
           '56 MG Road', 'Kochi', 'Kerala',
           TO_DATE('2003-09-05', 'YYYY-MM-DD'), 8.65, 'Computer Science & Engineering',
           '21BCE1006', 'BCE' FROM DUAL UNION ALL
    SELECT 'STU007', 'Siddharth Rao', 'siddharth.rao.demo@vitstudent.ac.in',
           '34 Banjara Hills', 'Hyderabad', 'Telangana',
           TO_DATE('2003-01-25', 'YYYY-MM-DD'), 8.10, 'Computer Science & Engineering',
           '21BCE1007', 'BCE' FROM DUAL UNION ALL
    SELECT 'STU008', 'Tanvi Joshi', 'tanvi.joshi.demo@vitstudent.ac.in',
           '67 FC Road', 'Pune', 'Maharashtra',
           TO_DATE('2003-07-14', 'YYYY-MM-DD'), 9.15, 'Data Science',
           '21BDS1008', 'BDS' FROM DUAL UNION ALL
    SELECT 'STU009', 'Aditya Deshmukh', 'aditya.deshmukh.demo@vitstudent.ac.in',
           '101 Shivaji Nagar', 'Nagpur', 'Maharashtra',
           TO_DATE('2002-12-01', 'YYYY-MM-DD'), 7.60, 'Mechanical Engineering',
           '21BME1009', 'BME' FROM DUAL UNION ALL
    SELECT 'STU010', 'Sneha Kulkarni', 'sneha.kulkarni.demo@vitstudent.ac.in',
           '15 Deccan Gymkhana', 'Pune', 'Maharashtra',
           TO_DATE('2003-03-22', 'YYYY-MM-DD'), 8.95, 'Information Technology',
           '21BCT1010', 'BCT' FROM DUAL UNION ALL
    SELECT 'STU011', 'Vikram Reddy', 'vikram.reddy.demo@vitstudent.ac.in',
           '88 Jubilee Hills', 'Hyderabad', 'Telangana',
           TO_DATE('2003-05-19', 'YYYY-MM-DD'), 8.35, 'Electronics & Communication',
           '21BEC1011', 'BEC' FROM DUAL UNION ALL
    SELECT 'STU012', 'Neha Sengupta', 'neha.sengupta.demo@vitstudent.ac.in',
           '42 Salt Lake Sector 5', 'Kolkata', 'West Bengal',
           TO_DATE('2003-10-10', 'YYYY-MM-DD'), 9.40, 'Computer Science & Engineering',
           '21BCE1012', 'BCE' FROM DUAL UNION ALL
    SELECT 'STU013', 'Rahul Mehra', 'rahul.mehra.demo@vitstudent.ac.in',
           '19 Connaught Place', 'New Delhi', 'Delhi',
           TO_DATE('2002-08-16', 'YYYY-MM-DD'), 7.80, 'Electrical & Electronics Engineering',
           '21BEE1013', 'BEE' FROM DUAL UNION ALL
    SELECT 'STU014', 'Riya Kapoor', 'riya.kapoor.demo@vitstudent.ac.in',
           '73 Bandra West', 'Mumbai', 'Maharashtra',
           TO_DATE('2003-11-28', 'YYYY-MM-DD'), 8.70, 'Information Technology',
           '21BCT1014', 'BCT' FROM DUAL UNION ALL
    SELECT 'STU015', 'Arjun Das', 'arjun.das.demo@vitstudent.ac.in',
           '91 GS Road', 'Guwahati', 'Assam',
           TO_DATE('2003-04-03', 'YYYY-MM-DD'), 7.45, 'Civil Engineering',
           '21BCV1015', 'BCV' FROM DUAL UNION ALL
    SELECT 'STU016', 'Pooja Hegde', 'pooja.hegde.demo@vitstudent.ac.in',
           '27 Hampankatta', 'Mangaluru', 'Karnataka',
           TO_DATE('2003-06-11', 'YYYY-MM-DD'), 8.55, 'Computer Science & Engineering',
           '21BCE1016', 'BCE' FROM DUAL UNION ALL
    SELECT 'STU017', 'Varun Choudhury', 'varun.choudhury.demo@vitstudent.ac.in',
           '31 MI Road', 'Jaipur', 'Rajasthan',
           TO_DATE('2002-10-24', 'YYYY-MM-DD'), 8.05, 'Mechanical Engineering',
           '21BME1017', 'BME' FROM DUAL UNION ALL
    SELECT 'STU018', 'Meera Nambiar', 'meera.nambiar.demo@vitstudent.ac.in',
           '50 Palayam', 'Thiruvananthapuram', 'Kerala',
           TO_DATE('2003-01-09', 'YYYY-MM-DD'), 9.10, 'Data Science',
           '21BDS1018', 'BDS' FROM DUAL UNION ALL
    SELECT 'STU019', 'Kunal Malhotra', 'kunal.malhotra.demo@vitstudent.ac.in',
           '62 Sector 17', 'Chandigarh', 'Chandigarh',
           TO_DATE('2002-07-07', 'YYYY-MM-DD'), 7.30, 'Electrical & Electronics Engineering',
           '21BEE1019', 'BEE' FROM DUAL UNION ALL
    SELECT 'STU020', 'Shreya Mukherjee', 'shreya.mukherjee.demo@vitstudent.ac.in',
           '84 South City', 'Kolkata', 'West Bengal',
           TO_DATE('2003-09-17', 'YYYY-MM-DD'), 8.90, 'Information Technology',
           '21BCT1020', 'BCT' FROM DUAL UNION ALL
    SELECT 'STU021', 'Karthik Subramanian', 'karthik.subramanian.demo@vitstudent.ac.in',
           '11 T Nagar', 'Chennai', 'Tamil Nadu',
           TO_DATE('2002-12-19', 'YYYY-MM-DD'), 9.60, 'Computer Science & Engineering',
           '21BCE1021', 'BCE' FROM DUAL UNION ALL
    SELECT 'STU022', 'Divya Menon', 'divya.menon.demo@vitstudent.ac.in',
           '95 Marine Drive', 'Kochi', 'Kerala',
           TO_DATE('2003-05-02', 'YYYY-MM-DD'), 8.20, 'Electronics & Communication',
           '21BEC1022', 'BEC' FROM DUAL UNION ALL
    -- Eligible candidate with NO submitted applications
    SELECT 'STU023', 'Harish Kumar', 'harish.kumar.demo@vitstudent.ac.in',
           '14 Race Course Road', 'Coimbatore', 'Tamil Nadu',
           TO_DATE('2003-03-10', 'YYYY-MM-DD'), 7.15, 'Civil Engineering',
           '21BCV1023', 'BCV' FROM DUAL UNION ALL
    -- Ineligible candidates (CGPA below minimum cutoff < 6.50)
    SELECT 'STU024', 'Saurabh Pandey', 'saurabh.pandey.demo@vitstudent.ac.in',
           '77 Civil Lines', 'Allahabad', 'Uttar Pradesh',
           TO_DATE('2002-11-20', 'YYYY-MM-DD'), 5.90, 'Mechanical Engineering',
           '21BME1024', 'BME' FROM DUAL UNION ALL
    SELECT 'STU025', 'Ritu Bhatt', 'ritu.bhatt.demo@vitstudent.ac.in',
           '12 Mall Road', 'Shimla', 'Himachal Pradesh',
           TO_DATE('2003-04-18', 'YYYY-MM-DD'), 5.80, 'Electrical & Electronics Engineering',
           '21BEE1025', 'BEE' FROM DUAL
) src ON (s.Student_Id = src.sid)
WHEN MATCHED THEN
    UPDATE SET s.Name = src.name, s.Email = src.email, s.Street = src.street, s.City = src.city,
               s.State = src.state, s.DOB = src.dob, s.CGPA = src.cgpa, s.Branch = src.branch,
               s.Registration_No = src.reg, s.Program_Id = src.pid
WHEN NOT MATCHED THEN
    INSERT (Student_Id, Name, Email, Street, City, State, DOB, CGPA, Branch, Registration_No, Program_Id, Placement_Status)
    VALUES (src.sid, src.name, src.email, src.street, src.city, src.state, src.dob, src.cgpa, src.branch, src.reg, src.pid, 'NOT_PLACED');

-- ------------------------------------------------------------------------------
-- 6. REGISTERS & STUDENT_PHONE & STUDENT_SKILL
-- ------------------------------------------------------------------------------
PROMPT [6/11] Linking Academic Registrations, Phones, and Skills...

MERGE INTO REGISTERS r
USING (
    SELECT Student_Id, Program_Id FROM STUDENT
) s ON (r.Student_Id = s.Student_Id AND r.Program_Id = s.Program_Id)
WHEN NOT MATCHED THEN
    INSERT (Student_Id, Program_Id, Reg_Date) VALUES (s.Student_Id, s.Program_Id, TO_DATE('2024-07-15', 'YYYY-MM-DD'));

MERGE INTO STUDENT_PHONE sp
USING (
    SELECT 'STU001' AS sid, '+91-8219631530' AS ph FROM DUAL UNION ALL
    SELECT 'STU002', '+91-9876543212' FROM DUAL UNION ALL
    SELECT 'STU003', '+91-9876543213' FROM DUAL UNION ALL
    SELECT 'STU004', '+91-9876543214' FROM DUAL UNION ALL
    SELECT 'STU005', '+91-9876543215' FROM DUAL UNION ALL
    SELECT 'STU006', '+91-9876543216' FROM DUAL UNION ALL
    SELECT 'STU007', '+91-9876543217' FROM DUAL UNION ALL
    SELECT 'STU008', '+91-9876543218' FROM DUAL UNION ALL
    SELECT 'STU009', '+91-9876543219' FROM DUAL UNION ALL
    SELECT 'STU010', '+91-9876543220' FROM DUAL UNION ALL
    SELECT 'STU011', '+91-9876543221' FROM DUAL UNION ALL
    SELECT 'STU012', '+91-9876543222' FROM DUAL UNION ALL
    SELECT 'STU013', '+91-9876543223' FROM DUAL UNION ALL
    SELECT 'STU014', '+91-9876543224' FROM DUAL UNION ALL
    SELECT 'STU015', '+91-9876543225' FROM DUAL UNION ALL
    SELECT 'STU016', '+91-9876543226' FROM DUAL UNION ALL
    SELECT 'STU017', '+91-9876543227' FROM DUAL UNION ALL
    SELECT 'STU018', '+91-9876543228' FROM DUAL UNION ALL
    SELECT 'STU019', '+91-9876543229' FROM DUAL UNION ALL
    SELECT 'STU020', '+91-9876543230' FROM DUAL UNION ALL
    SELECT 'STU021', '+91-9876543231' FROM DUAL UNION ALL
    SELECT 'STU022', '+91-9876543232' FROM DUAL UNION ALL
    SELECT 'STU023', '+91-9876543233' FROM DUAL UNION ALL
    SELECT 'STU024', '+91-9876543234' FROM DUAL UNION ALL
    SELECT 'STU025', '+91-9876543235' FROM DUAL
) src ON (sp.Student_Id = src.sid AND sp.Phone = src.ph)
WHEN NOT MATCHED THEN INSERT (Student_Id, Phone) VALUES (src.sid, src.ph);

-- STUDENT_SKILL
MERGE INTO STUDENT_SKILL ss
USING (
    SELECT 'STU001' AS sid, 'Java' AS sk, 'ADVANCED' AS pr FROM DUAL UNION ALL
    SELECT 'STU001', 'Spring Boot', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU001', 'React', 'INTERMEDIATE' FROM DUAL UNION ALL
    SELECT 'STU001', 'Neo4j', 'INTERMEDIATE' FROM DUAL UNION ALL
    SELECT 'STU001', 'Docker', 'INTERMEDIATE' FROM DUAL UNION ALL
    SELECT 'STU002', 'AWS', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU002', 'Python', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU002', 'Kubernetes', 'INTERMEDIATE' FROM DUAL UNION ALL
    SELECT 'STU003', 'C++', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU003', 'Algorithms', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU004', 'Machine Learning', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU004', 'Python', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU004', 'PyTorch', 'INTERMEDIATE' FROM DUAL UNION ALL
    SELECT 'STU005', 'Embedded Systems', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU005', 'IoT', 'INTERMEDIATE' FROM DUAL UNION ALL
    SELECT 'STU006', 'Network Security', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU006', 'Cisco Packet Tracer', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU007', 'Java', 'INTERMEDIATE' FROM DUAL UNION ALL
    SELECT 'STU007', 'SQL', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU008', 'Data Analytics', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU008', 'Apache Spark', 'INTERMEDIATE' FROM DUAL UNION ALL
    SELECT 'STU009', 'AutoCAD', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU009', 'MATLAB', 'INTERMEDIATE' FROM DUAL UNION ALL
    SELECT 'STU010', 'Oracle SQL', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU010', 'PL/SQL', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU011', 'Microcontrollers', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU012', 'System Design', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU012', 'Go', 'INTERMEDIATE' FROM DUAL UNION ALL
    SELECT 'STU018', 'NLP', 'ADVANCED' FROM DUAL UNION ALL
    SELECT 'STU021', 'Distributed Systems', 'ADVANCED' FROM DUAL
) src ON (ss.Student_Id = src.sid AND ss.Skill_Name = src.sk)
WHEN MATCHED THEN UPDATE SET ss.Proficiency = src.pr
WHEN NOT MATCHED THEN INSERT (Student_Id, Skill_Name, Proficiency) VALUES (src.sid, src.sk, src.pr);

-- ------------------------------------------------------------------------------
-- 7. PORTAL_USER & RECRUITER_COMPANY (RBAC Authentication Personas)
-- Password for all demo accounts is 'password123', admin is 'admin123'
-- ------------------------------------------------------------------------------
PROMPT [7/11] Configuring Authentication Accounts & Recruiter Permissions...

MERGE INTO PORTAL_USER pu
USING (
    -- Root Admin (USR005 / admin)
    SELECT 'USR005' AS u_id, 'admin' AS uname,
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK' AS phash,
           'ROLE_ADMIN' AS role, 'ADMIN01' AS refid,
           'System Administrator' AS fname, 'admin@vit.ac.in' AS email FROM DUAL UNION ALL
    -- Student 25bce1799 (STU001)
    SELECT 'USR_STU001', '25bce1799',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_STUDENT', 'STU001',
           'Achyut Ranaut', 'achyut.ranaut2025@vitstudent.ac.in' FROM DUAL UNION ALL
    SELECT 'USR001', 'student1',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_STUDENT', 'STU001',
           'Achyut Ranaut', 'achyut.ranaut2025@vitstudent.ac.in' FROM DUAL UNION ALL
    SELECT 'USR002', 'student2',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_STUDENT', 'STU002',
           'Diya Patel', 'diya.patel@vitstudent.ac.in' FROM DUAL UNION ALL
    -- Recruiters
    SELECT 'USR003', 'recruiter1',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_RECRUITER', 'COM004',
           'Vikram Singh', 'vikram.singh@microsoft.com' FROM DUAL UNION ALL
    SELECT 'USR004', 'recruiter2',
           '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK',
           'ROLE_RECRUITER', 'COM001',
           'Neha Sharma', 'neha.sharma@tcs.com' FROM DUAL UNION ALL
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
    VALUES (src.u_id, src.uname, src.phash, src.role, src.refid, src.fname, src.email);

MERGE INTO RECRUITER_COMPANY rc
USING (
    SELECT 'USR003' AS u_id, 'COM004' AS c_id FROM DUAL UNION ALL
    SELECT 'USR003', 'COM001' FROM DUAL UNION ALL
    SELECT 'USR004', 'COM001' FROM DUAL UNION ALL
    SELECT 'USR_COM002', 'COM002' FROM DUAL UNION ALL
    SELECT 'USR_COM003', 'COM003' FROM DUAL UNION ALL
    SELECT 'USR_COM005', 'COM005' FROM DUAL UNION ALL
    SELECT 'USR_COM006', 'COM006' FROM DUAL
) src ON (rc.User_Id = src.u_id AND rc.Company_Id = src.c_id)
WHEN NOT MATCHED THEN INSERT (User_Id, Company_Id) VALUES (src.u_id, src.c_id);

-- ------------------------------------------------------------------------------
-- 8. STUDENT_DAILY_ROUTINE_DRIVE (38 Applications Across 12 Drives)
-- Unique (Student_Id, Apply_Date) key guarantee
-- ------------------------------------------------------------------------------
PROMPT [8/11] Registering Student Drive Applications & Scheduling Routines...

MERGE INTO STUDENT_DAILY_ROUTINE_DRIVE sdr
USING (
    -- Placed / Offered Applications
    SELECT 'STU001' AS sid, TO_DATE('2026-11-01', 'YYYY-MM-DD') AS adate, 'DRV001' AS did FROM DUAL UNION ALL
    SELECT 'STU002', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV004' FROM DUAL UNION ALL
    SELECT 'STU002', TO_DATE('2026-11-03', 'YYYY-MM-DD'), 'DRV003' FROM DUAL UNION ALL
    SELECT 'STU004', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV006' FROM DUAL UNION ALL
    SELECT 'STU006', TO_DATE('2026-11-03', 'YYYY-MM-DD'), 'DRV008' FROM DUAL UNION ALL
    SELECT 'STU008', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV007' FROM DUAL UNION ALL
    SELECT 'STU009', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV002' FROM DUAL UNION ALL
    SELECT 'STU010', TO_DATE('2026-11-03', 'YYYY-MM-DD'), 'DRV005' FROM DUAL UNION ALL
    SELECT 'STU011', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV010' FROM DUAL UNION ALL
    SELECT 'STU018', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV012' FROM DUAL UNION ALL
    -- Interviewing Applications
    SELECT 'STU012', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV001' FROM DUAL UNION ALL
    SELECT 'STU013', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV002' FROM DUAL UNION ALL
    SELECT 'STU014', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV005' FROM DUAL UNION ALL
    SELECT 'STU015', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV009' FROM DUAL UNION ALL
    SELECT 'STU016', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV007' FROM DUAL UNION ALL
    SELECT 'STU017', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV002' FROM DUAL UNION ALL
    -- Shortlisted Applications
    SELECT 'STU005', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV008' FROM DUAL UNION ALL
    SELECT 'STU019', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV003' FROM DUAL UNION ALL
    SELECT 'STU020', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV008' FROM DUAL UNION ALL
    SELECT 'STU021', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV011' FROM DUAL UNION ALL
    SELECT 'STU022', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV004' FROM DUAL UNION ALL
    -- Applied Stage Applications
    SELECT 'STU007', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV001' FROM DUAL UNION ALL
    SELECT 'STU007', TO_DATE('2026-11-03', 'YYYY-MM-DD'), 'DRV006' FROM DUAL UNION ALL
    SELECT 'STU012', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV011' FROM DUAL UNION ALL
    SELECT 'STU013', TO_DATE('2026-11-03', 'YYYY-MM-DD'), 'DRV004' FROM DUAL UNION ALL
    SELECT 'STU014', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV006' FROM DUAL UNION ALL
    SELECT 'STU015', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV002' FROM DUAL UNION ALL
    SELECT 'STU016', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV012' FROM DUAL UNION ALL
    SELECT 'STU017', TO_DATE('2026-11-03', 'YYYY-MM-DD'), 'DRV009' FROM DUAL UNION ALL
    SELECT 'STU019', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV008' FROM DUAL UNION ALL
    SELECT 'STU020', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV010' FROM DUAL UNION ALL
    SELECT 'STU021', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV007' FROM DUAL UNION ALL
    SELECT 'STU022', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV003' FROM DUAL UNION ALL
    SELECT 'STU003', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV001' FROM DUAL UNION ALL
    SELECT 'STU003', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV002' FROM DUAL UNION ALL
    SELECT 'STU006', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV001' FROM DUAL UNION ALL
    -- Rejected Stage Applications
    SELECT 'STU005', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV004' FROM DUAL UNION ALL
    SELECT 'STU013', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV008' FROM DUAL
) src ON (sdr.Student_Id = src.sid AND sdr.Apply_Date = src.adate)
WHEN MATCHED THEN UPDATE SET sdr.Drive_Id = src.did
WHEN NOT MATCHED THEN INSERT (Student_Id, Apply_Date, Drive_Id) VALUES (src.sid, src.adate, src.did);

-- ------------------------------------------------------------------------------
-- 9. AUTHORITATIVE PL/SQL PIPELINE STATE MACHINE EXECUTION
-- Progresses applications from APPLIED -> SHORTLISTED -> INTERVIEWING ->
-- (interviews logged) -> SELECTED -> (offers released) -> ACCEPTED/DECLINED
-- ------------------------------------------------------------------------------
PROMPT [9/11] Progressing Applications through Placement Pipeline State Machine...

DECLARE
    v_cnt NUMBER;

    PROCEDURE register_app(
        p_app_id IN VARCHAR2,
        p_stu_id IN VARCHAR2,
        p_date   IN DATE,
        p_drv_id IN VARCHAR2
    ) IS
    BEGIN
        SELECT COUNT(*) INTO v_cnt FROM APPLICATION WHERE Application_Id = p_app_id;
        IF v_cnt = 0 THEN
            INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status, Drive_Id)
            VALUES (p_app_id, p_stu_id, p_date, 'APPLIED', p_drv_id);
        END IF;
    END;

    PROCEDURE add_interview(
        p_app_id      IN VARCHAR2,
        p_interviewer IN VARCHAR2,
        p_oa          IN NUMBER,
        p_gd          IN NUMBER,
        p_hr          IN NUMBER,
        p_result      IN VARCHAR2
    ) IS
    BEGIN
        SELECT COUNT(*) INTO v_cnt FROM INTERVIEW WHERE Application_Id = p_app_id AND Interviewer_Name = p_interviewer;
        IF v_cnt = 0 THEN
            INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
            VALUES (p_app_id, p_interviewer, p_oa, p_gd, p_hr, p_result, 'Y', 'N');
        ELSE
            UPDATE INTERVIEW
            SET OA = p_oa, GD = p_gd, HR = p_hr, Result = p_result
            WHERE Application_Id = p_app_id AND Interviewer_Name = p_interviewer;
        END IF;
    END;

BEGIN
    -- 1. Insert Base Applications (as APPLIED initially to pass insert trigger)
    register_app('APP001', 'STU001', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV001');
    register_app('APP002', 'STU002', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV004');
    register_app('APP003', 'STU002', TO_DATE('2026-11-03', 'YYYY-MM-DD'), 'DRV003');
    register_app('APP004', 'STU004', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV006');
    register_app('APP005', 'STU005', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV008');
    register_app('APP006', 'STU006', TO_DATE('2026-11-03', 'YYYY-MM-DD'), 'DRV008');
    register_app('APP007', 'STU007', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV001');
    register_app('APP008', 'STU008', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV007');
    register_app('APP009', 'STU009', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV002');
    register_app('APP010', 'STU010', TO_DATE('2026-11-03', 'YYYY-MM-DD'), 'DRV005');
    register_app('APP011', 'STU011', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV010');
    register_app('APP012', 'STU012', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV001');
    register_app('APP013', 'STU013', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV002');
    register_app('APP014', 'STU014', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV005');
    register_app('APP015', 'STU015', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV009');
    register_app('APP016', 'STU016', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV007');
    register_app('APP017', 'STU017', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV002');
    register_app('APP018', 'STU018', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV012');
    register_app('APP019', 'STU019', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV003');
    register_app('APP020', 'STU020', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV008');
    register_app('APP021', 'STU021', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV011');
    register_app('APP022', 'STU022', TO_DATE('2026-11-02', 'YYYY-MM-DD'), 'DRV004');
    register_app('APP023', 'STU007', TO_DATE('2026-11-03', 'YYYY-MM-DD'), 'DRV006');
    register_app('APP024', 'STU012', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV011');
    register_app('APP025', 'STU013', TO_DATE('2026-11-03', 'YYYY-MM-DD'), 'DRV004');
    register_app('APP026', 'STU014', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV006');
    register_app('APP027', 'STU015', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV002');
    register_app('APP028', 'STU016', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV012');
    register_app('APP029', 'STU017', TO_DATE('2026-11-03', 'YYYY-MM-DD'), 'DRV009');
    register_app('APP030', 'STU019', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV008');
    register_app('APP031', 'STU020', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV010');
    register_app('APP032', 'STU021', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV007');
    register_app('APP033', 'STU022', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV003');
    register_app('APP034', 'STU003', TO_DATE('2026-11-01', 'YYYY-MM-DD'), 'DRV001');
    register_app('APP035', 'STU003', TO_DATE('2026-11-04', 'YYYY-MM-DD'), 'DRV002');
    register_app('APP036', 'STU006', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV001');
    register_app('APP037', 'STU005', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV004');
    register_app('APP038', 'STU013', TO_DATE('2026-11-05', 'YYYY-MM-DD'), 'DRV008');

    -- 2. Progress Shortlisted & Interviewing Apps
    UPDATE APPLICATION SET Status = 'SHORTLISTED'
    WHERE Application_Id IN ('APP001', 'APP002', 'APP003', 'APP004', 'APP005', 'APP006',
                             'APP008', 'APP009', 'APP010', 'APP011', 'APP012', 'APP013',
                             'APP014', 'APP015', 'APP016', 'APP017', 'APP018', 'APP019',
                             'APP020', 'APP021', 'APP022', 'APP037', 'APP038')
      AND Status = 'APPLIED';

    UPDATE APPLICATION SET Status = 'INTERVIEWING'
    WHERE Application_Id IN ('APP001', 'APP002', 'APP003', 'APP004', 'APP006',
                             'APP008', 'APP009', 'APP010', 'APP011', 'APP012',
                             'APP013', 'APP014', 'APP015', 'APP016', 'APP017',
                             'APP018', 'APP037', 'APP038')
      AND Status = 'SHORTLISTED';

    -- 3. Insert Interview Evaluations
    -- APP001 (Microsoft)
    add_interview('APP001', 'Dr. Rajesh Sundaram', 95, NULL, NULL, 'CLEARED');
    add_interview('APP001', 'Vikramaditya Bose', NULL, 92, NULL, 'CLEARED');
    add_interview('APP001', 'Suresh Venkataraman', NULL, NULL, 96, 'CLEARED');

    -- APP002 (Amazon)
    add_interview('APP002', 'Vikram Malhotra', 90, NULL, NULL, 'CLEARED');
    add_interview('APP002', 'Sangeeta Rao', NULL, 88, NULL, 'CLEARED');
    add_interview('APP002', 'Arvind Swamy', NULL, NULL, 92, 'CLEARED');

    -- APP003 (Infosys)
    add_interview('APP003', 'Dr. Rajesh Sundaram', 86, NULL, NULL, 'CLEARED');
    add_interview('APP003', 'Vikramaditya Bose', NULL, 85, NULL, 'CLEARED');
    add_interview('APP003', 'Suresh Venkataraman', NULL, NULL, 88, 'CLEARED');

    -- APP004 (Goldman Sachs)
    add_interview('APP004', 'Priya Ramanathan', 94, NULL, NULL, 'CLEARED');
    add_interview('APP004', 'Ananya Chatterjee', NULL, 91, NULL, 'CLEARED');
    add_interview('APP004', 'Kavita Menon', NULL, NULL, 95, 'CLEARED');

    -- APP006 (Cisco)
    add_interview('APP006', 'Priya Ramanathan', 88, NULL, NULL, 'CLEARED');
    add_interview('APP006', 'Ananya Chatterjee', NULL, 89, NULL, 'CLEARED');
    add_interview('APP006', 'Kavita Menon', NULL, NULL, 90, 'CLEARED');

    -- APP008 (Flipkart)
    add_interview('APP008', 'Vikram Malhotra', 93, NULL, NULL, 'CLEARED');
    add_interview('APP008', 'Sangeeta Rao', NULL, 90, NULL, 'CLEARED');
    add_interview('APP008', 'Arvind Swamy', NULL, NULL, 94, 'CLEARED');

    -- APP009 (TCS)
    add_interview('APP009', 'Priya Ramanathan', 82, NULL, NULL, 'CLEARED');
    add_interview('APP009', 'Ananya Chatterjee', NULL, 84, NULL, 'CLEARED');
    add_interview('APP009', 'Kavita Menon', NULL, NULL, 86, 'CLEARED');

    -- APP010 (Oracle Apps)
    add_interview('APP010', 'Dr. Rajesh Sundaram', 89, NULL, NULL, 'CLEARED');
    add_interview('APP010', 'Vikramaditya Bose', NULL, 90, NULL, 'CLEARED');
    add_interview('APP010', 'Suresh Venkataraman', NULL, NULL, 93, 'CLEARED');

    -- APP011 (Zomato)
    add_interview('APP011', 'Vikram Malhotra', 86, NULL, NULL, 'CLEARED');
    add_interview('APP011', 'Sangeeta Rao', NULL, 87, NULL, 'CLEARED');
    add_interview('APP011', 'Arvind Swamy', NULL, NULL, 89, 'CLEARED');

    -- APP018 (Oracle MTS)
    add_interview('APP018', 'Dr. Rajesh Sundaram', 96, NULL, NULL, 'CLEARED');
    add_interview('APP018', 'Vikramaditya Bose', NULL, 95, NULL, 'CLEARED');
    add_interview('APP018', 'Suresh Venkataraman', NULL, NULL, 98, 'CLEARED');

    -- Intermediate Interviewing (ongoing)
    add_interview('APP012', 'Dr. Rajesh Sundaram', 91, NULL, NULL, 'CLEARED');
    add_interview('APP012', 'Vikramaditya Bose', NULL, 87, NULL, 'PENDING');
    add_interview('APP013', 'Priya Ramanathan', 85, NULL, NULL, 'CLEARED');
    add_interview('APP014', 'Dr. Rajesh Sundaram', 88, NULL, NULL, 'CLEARED');
    add_interview('APP015', 'Vikram Malhotra', 80, NULL, NULL, 'CLEARED');
    add_interview('APP016', 'Vikram Malhotra', 89, NULL, NULL, 'CLEARED');
    add_interview('APP017', 'Priya Ramanathan', 81, NULL, NULL, 'CLEARED');

    -- Rejected in interviews (Result must be 'REJECTED' per CHK_INTERVIEW_RESULT)
    add_interview('APP037', 'Vikram Malhotra', 55, NULL, NULL, 'REJECTED');
    add_interview('APP038', 'Priya Ramanathan', 58, NULL, NULL, 'REJECTED');

    -- 4. Mark Rejections
    UPDATE APPLICATION SET Status = 'REJECTED' WHERE Application_Id = 'APP037' AND Status != 'REJECTED';
    UPDATE APPLICATION SET Status = 'REJECTED' WHERE Application_Id = 'APP038' AND Status != 'REJECTED';

    -- 5. Transition Fully Cleared Apps to SELECTED
    UPDATE APPLICATION SET Status = 'SELECTED'
    WHERE Application_Id IN ('APP001', 'APP002', 'APP003', 'APP004', 'APP006',
                             'APP008', 'APP009', 'APP010', 'APP011', 'APP018')
      AND Status = 'INTERVIEWING';

END;
/

-- ------------------------------------------------------------------------------
-- 10. OFFER_LETTER & STUDENT PLACEMENT FINALIZATION
-- ------------------------------------------------------------------------------
PROMPT [10/11] Issuing & Finalizing Employment Offers (Single Mutex Compliant)...

DECLARE
    v_cnt NUMBER;

    PROCEDURE create_offer(
        p_off_id IN VARCHAR2,
        p_app_id IN VARCHAR2,
        p_ctc    IN NUMBER,
        p_date   IN DATE
    ) IS
    BEGIN
        SELECT COUNT(*) INTO v_cnt FROM OFFER_LETTER WHERE Offer_Id = p_off_id;
        IF v_cnt = 0 THEN
            INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA, Status)
            VALUES (p_off_id, p_app_id, p_date, p_ctc, 'OFFERED');
        END IF;
    END;

BEGIN
    -- Super Dream Offers (>= 20 LPA)
    create_offer('OFF001', 'APP001', 45.00, TO_DATE('2026-11-08', 'YYYY-MM-DD')); -- Microsoft
    create_offer('OFF002', 'APP004', 28.00, TO_DATE('2026-11-07', 'YYYY-MM-DD')); -- Goldman Sachs
    create_offer('OFF003', 'APP008', 26.00, TO_DATE('2026-11-06', 'YYYY-MM-DD')); -- Flipkart
    create_offer('OFF004', 'APP010', 22.00, TO_DATE('2026-11-05', 'YYYY-MM-DD')); -- Oracle Apps

    -- Dream Offers (10 - 20 LPA)
    create_offer('OFF005', 'APP006', 18.00, TO_DATE('2026-11-04', 'YYYY-MM-DD')); -- Cisco
    create_offer('OFF006', 'APP002', 16.00, TO_DATE('2026-11-03', 'YYYY-MM-DD')); -- Amazon
    create_offer('OFF009', 'APP011', 14.00, TO_DATE('2026-11-04', 'YYYY-MM-DD')); -- Zomato (Pending Offered)

    -- Regular Offers (< 10 LPA)
    create_offer('OFF007', 'APP003', 9.50, TO_DATE('2026-11-03', 'YYYY-MM-DD'));  -- Infosys (Declined by Diya)
    create_offer('OFF008', 'APP009', 7.50, TO_DATE('2026-11-02', 'YYYY-MM-DD'));  -- TCS

    -- Finalize Accepted Offers (7 Placed Students)
    -- STU001 (Achyut Ranaut / Microsoft)
    UPDATE OFFER_LETTER SET Status = 'ACCEPTED', Accepted_At = TO_TIMESTAMP('2026-11-09 10:30:00', 'YYYY-MM-DD HH24:MI:SS') WHERE Offer_Id = 'OFF001';
    UPDATE APPLICATION SET Status = 'ACCEPTED' WHERE Application_Id = 'APP001';
    UPDATE STUDENT SET Placement_Status = 'PLACED' WHERE Student_Id = 'STU001';

    -- STU004 (Ananya Gupta / Goldman Sachs)
    UPDATE OFFER_LETTER SET Status = 'ACCEPTED', Accepted_At = TO_TIMESTAMP('2026-11-08 11:15:00', 'YYYY-MM-DD HH24:MI:SS') WHERE Offer_Id = 'OFF002';
    UPDATE APPLICATION SET Status = 'ACCEPTED' WHERE Application_Id = 'APP004';
    UPDATE STUDENT SET Placement_Status = 'PLACED' WHERE Student_Id = 'STU004';

    -- STU008 (Tanvi Joshi / Flipkart)
    UPDATE OFFER_LETTER SET Status = 'ACCEPTED', Accepted_At = TO_TIMESTAMP('2026-11-07 14:00:00', 'YYYY-MM-DD HH24:MI:SS') WHERE Offer_Id = 'OFF003';
    UPDATE APPLICATION SET Status = 'ACCEPTED' WHERE Application_Id = 'APP008';
    UPDATE STUDENT SET Placement_Status = 'PLACED' WHERE Student_Id = 'STU008';

    -- STU010 (Sneha Kulkarni / Oracle)
    UPDATE OFFER_LETTER SET Status = 'ACCEPTED', Accepted_At = TO_TIMESTAMP('2026-11-06 16:45:00', 'YYYY-MM-DD HH24:MI:SS') WHERE Offer_Id = 'OFF004';
    UPDATE APPLICATION SET Status = 'ACCEPTED' WHERE Application_Id = 'APP010';
    UPDATE STUDENT SET Placement_Status = 'PLACED' WHERE Student_Id = 'STU010';

    -- STU006 (Priya Nair / Cisco)
    UPDATE OFFER_LETTER SET Status = 'ACCEPTED', Accepted_At = TO_TIMESTAMP('2026-11-05 09:30:00', 'YYYY-MM-DD HH24:MI:SS') WHERE Offer_Id = 'OFF005';
    UPDATE APPLICATION SET Status = 'ACCEPTED' WHERE Application_Id = 'APP006';
    UPDATE STUDENT SET Placement_Status = 'PLACED' WHERE Student_Id = 'STU006';

    -- STU002 (Diya Patel / Amazon - accepted OFF006, declined OFF007 Infosys)
    UPDATE OFFER_LETTER SET Status = 'ACCEPTED', Accepted_At = TO_TIMESTAMP('2026-11-04 12:00:00', 'YYYY-MM-DD HH24:MI:SS') WHERE Offer_Id = 'OFF006';
    UPDATE APPLICATION SET Status = 'ACCEPTED' WHERE Application_Id = 'APP002';
    UPDATE OFFER_LETTER SET Status = 'DECLINED' WHERE Offer_Id = 'OFF007';
    UPDATE APPLICATION SET Status = 'DECLINED' WHERE Application_Id = 'APP003';
    UPDATE STUDENT SET Placement_Status = 'PLACED' WHERE Student_Id = 'STU002';

    -- STU009 (Aditya Deshmukh / TCS)
    UPDATE OFFER_LETTER SET Status = 'ACCEPTED', Accepted_At = TO_TIMESTAMP('2026-11-03 15:20:00', 'YYYY-MM-DD HH24:MI:SS') WHERE Offer_Id = 'OFF008';
    UPDATE APPLICATION SET Status = 'ACCEPTED' WHERE Application_Id = 'APP009';
    UPDATE STUDENT SET Placement_Status = 'PLACED' WHERE Student_Id = 'STU009';

    -- STU011 (Vikram Reddy / Zomato - Released Offer Awaiting Decision)
    UPDATE STUDENT SET Placement_Status = 'OFFERED' WHERE Student_Id = 'STU011';

END;
/

COMMIT;

-- ------------------------------------------------------------------------------
-- 11. AUDIT & SUMMARY VERIFICATION REPORT
-- ------------------------------------------------------------------------------
PROMPT [11/11] Verifying Persisted Demonstration Dataset Metrics...

SET LINESIZE 200;
SET PAGESIZE 100;

PROMPT
PROMPT -------------------------------------------------------------------
PROMPT PlaceX Mock Data Seeding Summary:
PROMPT -------------------------------------------------------------------

SELECT 'Total Registered Students' AS Metric, COUNT(*) AS Record_Count FROM STUDENT
UNION ALL
SELECT 'Eligible Candidates (CGPA >= 6.50)', COUNT(*) FROM STUDENT WHERE CGPA >= 6.50
UNION ALL
SELECT 'Ineligible Candidates (CGPA < 6.50)', COUNT(*) FROM STUDENT WHERE CGPA < 6.50
UNION ALL
SELECT 'Verified Placed Students', COUNT(*) FROM STUDENT WHERE Placement_Status = 'PLACED'
UNION ALL
SELECT 'Candidates with Released Offers', COUNT(*) FROM STUDENT WHERE Placement_Status = 'OFFERED'
UNION ALL
SELECT 'Partner Companies', COUNT(*) FROM COMPANY
UNION ALL
SELECT 'Campus Placement Drives', COUNT(*) FROM PLACEMENT_DRIVE
UNION ALL
SELECT 'Drive Eligibility Rules', COUNT(*) FROM DRIVE_ELIGIBILITY
UNION ALL
SELECT 'Total Applications Submitted', COUNT(*) FROM APPLICATION
UNION ALL
SELECT 'Interviews Conducted', COUNT(*) FROM INTERVIEW
UNION ALL
SELECT 'Released Offer Letters', COUNT(*) FROM OFFER_LETTER
UNION ALL
SELECT 'Offers Accepted', COUNT(*) FROM OFFER_LETTER WHERE Status = 'ACCEPTED'
UNION ALL
SELECT 'Offers Pending Decision', COUNT(*) FROM OFFER_LETTER WHERE Status = 'OFFERED'
UNION ALL
SELECT 'Competing Offers Declined', COUNT(*) FROM OFFER_LETTER WHERE Status = 'DECLINED';

PROMPT
PROMPT Placement Pipeline Stage Distribution:
SELECT Status, COUNT(*) AS Total FROM APPLICATION GROUP BY Status ORDER BY Total DESC;

PROMPT
PROMPT ====================================================================
PROMPT Demo portal data seeding successfully applied!
PROMPT ====================================================================
