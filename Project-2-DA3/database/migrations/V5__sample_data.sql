-- ============================================================
-- V5__sample_data.sql
-- Realistic Indian University Campus Placement Dataset
-- (Updated per Locked Decision 1: Program-Scoped Batch_No)
-- ============================================================

-- ------------------------------------------------------------
-- 1. STUDENTS (22 Students)
-- ------------------------------------------------------------
INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU001', 'Aarav Sharma', 'aarav.sharma@univ.edu.in', '12 Brigade Road', 'Bengaluru', 'Karnataka', TO_DATE('2003-04-15', 'YYYY-MM-DD'), 9.25);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU002', 'Diya Patel', 'diya.patel@univ.edu.in', '45 CG Road', 'Ahmedabad', 'Gujarat', TO_DATE('2003-08-20', 'YYYY-MM-DD'), 8.80);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU003', 'Rohan Iyer', 'rohan.iyer@univ.edu.in', '78 Anna Salai', 'Chennai', 'Tamil Nadu', TO_DATE('2002-11-12', 'YYYY-MM-DD'), 8.45);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU004', 'Ananya Gupta', 'ananya.gupta@univ.edu.in', '23 Park Street', 'Kolkata', 'West Bengal', TO_DATE('2003-02-18', 'YYYY-MM-DD'), 9.50);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU005', 'Ishaan Verma', 'ishaan.verma@univ.edu.in', '89 Hazratganj', 'Lucknow', 'Uttar Pradesh', TO_DATE('2003-06-30', 'YYYY-MM-DD'), 7.90);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU006', 'Priya Nair', 'priya.nair@univ.edu.in', '56 MG Road', 'Kochi', 'Kerala', TO_DATE('2002-09-05', 'YYYY-MM-DD'), 8.65);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU007', 'Siddharth Rao', 'siddharth.rao@univ.edu.in', '34 Banjara Hills', 'Hyderabad', 'Telangana', TO_DATE('2003-01-25', 'YYYY-MM-DD'), 8.10);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU008', 'Tanvi Joshi', 'tanvi.joshi@univ.edu.in', '67 FC Road', 'Pune', 'Maharashtra', TO_DATE('2003-07-14', 'YYYY-MM-DD'), 9.15);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU009', 'Aditya Deshmukh', 'aditya.deshmukh@univ.edu.in', '101 Shivaji Nagar', 'Nagpur', 'Maharashtra', TO_DATE('2002-12-01', 'YYYY-MM-DD'), 7.60);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU010', 'Sneha Kulkarni', 'sneha.kulkarni@univ.edu.in', '15 Deccan Gymkhana', 'Pune', 'Maharashtra', TO_DATE('2003-03-22', 'YYYY-MM-DD'), 8.95);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU011', 'Vikram Reddy', 'vikram.reddy@univ.edu.in', '88 Jubilee Hills', 'Hyderabad', 'Telangana', TO_DATE('2003-05-19', 'YYYY-MM-DD'), 8.35);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU012', 'Neha Sengupta', 'neha.sengupta@univ.edu.in', '42 Salt Lake Sector 5', 'Kolkata', 'West Bengal', TO_DATE('2003-10-10', 'YYYY-MM-DD'), 9.40);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU013', 'Rahul Mehra', 'rahul.mehra@univ.edu.in', '19 Connaught Place', 'New Delhi', 'Delhi', TO_DATE('2002-08-16', 'YYYY-MM-DD'), 7.80);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU014', 'Riya Kapoor', 'riya.kapoor@univ.edu.in', '73 Bandra West', 'Mumbai', 'Maharashtra', TO_DATE('2003-11-28', 'YYYY-MM-DD'), 8.70);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU015', 'Arjun Das', 'arjun.das@univ.edu.in', '91 GS Road', 'Guwahati', 'Assam', TO_DATE('2003-04-03', 'YYYY-MM-DD'), 7.45);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU016', 'Pooja Hegde', 'pooja.hegde@univ.edu.in', '27 Hampankatta', 'Mangaluru', 'Karnataka', TO_DATE('2003-06-11', 'YYYY-MM-DD'), 8.55);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU017', 'Varun Choudhury', 'varun.choudhury@univ.edu.in', '31 MI Road', 'Jaipur', 'Rajasthan', TO_DATE('2002-10-24', 'YYYY-MM-DD'), 8.05);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU018', 'Meera Nambiar', 'meera.nambiar@univ.edu.in', '50 Palayam', 'Thiruvananthapuram', 'Kerala', TO_DATE('2003-01-09', 'YYYY-MM-DD'), 9.10);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU019', 'Kunal Malhotra', 'kunal.malhotra@univ.edu.in', '62 Sector 17', 'Chandigarh', 'Chandigarh', TO_DATE('2002-07-07', 'YYYY-MM-DD'), 7.30);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU020', 'Shreya Mukherjee', 'shreya.mukherjee@univ.edu.in', '84 South City', 'Kolkata', 'West Bengal', TO_DATE('2003-09-17', 'YYYY-MM-DD'), 8.90);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU021', 'Karthik Subramanian', 'karthik.subramanian@univ.edu.in', '11 T Nagar', 'Chennai', 'Tamil Nadu', TO_DATE('2002-12-19', 'YYYY-MM-DD'), 9.60);

INSERT INTO STUDENT (Student_Id, Name, Email, Street, City, State, DOB, CGPA)
VALUES ('STU022', 'Divya Menon', 'divya.menon@univ.edu.in', '95 Marine Drive', 'Kochi', 'Kerala', TO_DATE('2003-05-02', 'YYYY-MM-DD'), 8.20);

-- ------------------------------------------------------------
-- 2. STUDENT_PHONE
-- ------------------------------------------------------------
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU001', '+91-9876543210');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU001', '+91-9876543211');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU002', '+91-9876543212');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU003', '+91-9876543213');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU004', '+91-9876543214');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU004', '+91-9876543215');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU005', '+91-9876543216');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU006', '+91-9876543217');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU007', '+91-9876543218');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU008', '+91-9876543219');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU009', '+91-9876543220');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU010', '+91-9876543221');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU011', '+91-9876543222');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU012', '+91-9876543223');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU013', '+91-9876543224');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU014', '+91-9876543225');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU015', '+91-9876543226');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU016', '+91-9876543227');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU017', '+91-9876543228');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU018', '+91-9876543229');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU019', '+91-9876543230');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU020', '+91-9876543231');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU021', '+91-9876543232');
INSERT INTO STUDENT_PHONE (Student_Id, Phone) VALUES ('STU022', '+91-9876543233');

-- ------------------------------------------------------------
-- 3. PROGRAM (5 Programs)
-- ------------------------------------------------------------
INSERT INTO PROGRAM (Program_Id, Program_Name, Fee, Duration, Beginner, Intermediate, Advanced)
VALUES ('PROG01', 'Full Stack Java Enterprise Systems', 15000, '3 Months', 'N', 'Y', 'N');

INSERT INTO PROGRAM (Program_Id, Program_Name, Fee, Duration, Beginner, Intermediate, Advanced)
VALUES ('PROG02', 'Applied Artificial Intelligence & Deep Learning', 22000, '4 Months', 'N', 'N', 'Y');

INSERT INTO PROGRAM (Program_Id, Program_Name, Fee, Duration, Beginner, Intermediate, Advanced)
VALUES ('PROG03', 'Cloud Native DevOps & Kubernetes', 18000, '3 Months', 'N', 'Y', 'N');

INSERT INTO PROGRAM (Program_Id, Program_Name, Fee, Duration, Beginner, Intermediate, Advanced)
VALUES ('PROG04', 'Algorithmic Problem Solving & Competitive Coding', 10000, '2 Months', 'Y', 'N', 'N');

INSERT INTO PROGRAM (Program_Id, Program_Name, Fee, Duration, Beginner, Intermediate, Advanced)
VALUES ('PROG05', 'Cybersecurity Operations & Threat Hunting', 20000, '4 Months', 'N', 'N', 'Y');

-- ------------------------------------------------------------
-- 4. REGISTERS
-- ------------------------------------------------------------
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU001', 'PROG01', TO_DATE('2025-07-01', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU001', 'PROG02', TO_DATE('2025-07-02', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU002', 'PROG01', TO_DATE('2025-07-01', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU003', 'PROG03', TO_DATE('2025-07-03', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU004', 'PROG02', TO_DATE('2025-07-02', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU005', 'PROG04', TO_DATE('2025-07-04', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU006', 'PROG01', TO_DATE('2025-07-01', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU007', 'PROG03', TO_DATE('2025-07-03', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU008', 'PROG02', TO_DATE('2025-07-02', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU009', 'PROG04', TO_DATE('2025-07-04', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU010', 'PROG01', TO_DATE('2025-07-01', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU011', 'PROG03', TO_DATE('2025-07-03', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU012', 'PROG02', TO_DATE('2025-07-02', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU013', 'PROG04', TO_DATE('2025-07-04', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU014', 'PROG01', TO_DATE('2025-07-01', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU016', 'PROG05', TO_DATE('2025-07-05', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU018', 'PROG02', TO_DATE('2025-07-02', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU020', 'PROG01', TO_DATE('2025-07-01', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU021', 'PROG02', TO_DATE('2025-07-02', 'YYYY-MM-DD'));
INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES ('STU022', 'PROG03', TO_DATE('2025-07-03', 'YYYY-MM-DD'));

-- ------------------------------------------------------------
-- 5. BATCH (Program_Id, Batch_No)
-- ------------------------------------------------------------
INSERT INTO BATCH (Program_Id, Batch_No, Schedule, Room, Start_Date, End_Date)
VALUES ('PROG01', 'BAT001', 'Mon-Wed-Fri 09:00-11:00', 'Lab 401', TO_DATE('2025-08-01', 'YYYY-MM-DD'), TO_DATE('2025-10-31', 'YYYY-MM-DD'));

INSERT INTO BATCH (Program_Id, Batch_No, Schedule, Room, Start_Date, End_Date)
VALUES ('PROG02', 'BAT002', 'Tue-Thu-Sat 10:00-12:00', 'AI Lab 201', TO_DATE('2025-08-01', 'YYYY-MM-DD'), TO_DATE('2025-11-30', 'YYYY-MM-DD'));

INSERT INTO BATCH (Program_Id, Batch_No, Schedule, Room, Start_Date, End_Date)
VALUES ('PROG03', 'BAT003', 'Mon-Wed-Fri 14:00-16:00', 'Cloud Lab 105', TO_DATE('2025-08-01', 'YYYY-MM-DD'), TO_DATE('2025-10-31', 'YYYY-MM-DD'));

INSERT INTO BATCH (Program_Id, Batch_No, Schedule, Room, Start_Date, End_Date)
VALUES ('PROG04', 'BAT004', 'Mon-to-Fri 16:30-18:00', 'Seminar Hall B', TO_DATE('2025-08-01', 'YYYY-MM-DD'), TO_DATE('2025-09-30', 'YYYY-MM-DD'));

INSERT INTO BATCH (Program_Id, Batch_No, Schedule, Room, Start_Date, End_Date)
VALUES ('PROG05', 'BAT005', 'Tue-Thu 14:00-17:00', 'Security SOC Lab', TO_DATE('2025-08-01', 'YYYY-MM-DD'), TO_DATE('2025-11-30', 'YYYY-MM-DD'));

-- ------------------------------------------------------------
-- 6. BATCH_PROGRAM (Program_Id, Batch_No)
-- ------------------------------------------------------------
INSERT INTO BATCH_PROGRAM (Program_Id, Batch_No) VALUES ('PROG01', 'BAT001');
INSERT INTO BATCH_PROGRAM (Program_Id, Batch_No) VALUES ('PROG02', 'BAT002');
INSERT INTO BATCH_PROGRAM (Program_Id, Batch_No) VALUES ('PROG03', 'BAT003');
INSERT INTO BATCH_PROGRAM (Program_Id, Batch_No) VALUES ('PROG04', 'BAT004');
INSERT INTO BATCH_PROGRAM (Program_Id, Batch_No) VALUES ('PROG05', 'BAT005');

-- ------------------------------------------------------------
-- 7. ATTENDS (Student_Id, Program_Id, Batch_No)
-- ------------------------------------------------------------
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU001', 'PROG01', 'BAT001');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU002', 'PROG01', 'BAT001');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU006', 'PROG01', 'BAT001');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU010', 'PROG01', 'BAT001');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU014', 'PROG01', 'BAT001');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU020', 'PROG01', 'BAT001');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU001', 'PROG02', 'BAT002');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU004', 'PROG02', 'BAT002');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU008', 'PROG02', 'BAT002');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU012', 'PROG02', 'BAT002');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU018', 'PROG02', 'BAT002');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU021', 'PROG02', 'BAT002');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU003', 'PROG03', 'BAT003');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU007', 'PROG03', 'BAT003');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU011', 'PROG03', 'BAT003');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU022', 'PROG03', 'BAT003');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU005', 'PROG04', 'BAT004');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU009', 'PROG04', 'BAT004');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU013', 'PROG04', 'BAT004');
INSERT INTO ATTENDS (Student_Id, Program_Id, Batch_No) VALUES ('STU016', 'PROG05', 'BAT005');

-- ------------------------------------------------------------
-- 8. BATCH_TITLE_PROGRAM (Program_Id, Batch_No, Title)
-- ------------------------------------------------------------
INSERT INTO BATCH_TITLE_PROGRAM (Program_Id, Batch_No, Title)
VALUES ('PROG01', 'BAT001', 'Spring Boot & Microservice Architecture');

INSERT INTO BATCH_TITLE_PROGRAM (Program_Id, Batch_No, Title)
VALUES ('PROG02', 'BAT002', 'Transformer Models & LLM Fine-tuning');

INSERT INTO BATCH_TITLE_PROGRAM (Program_Id, Batch_No, Title)
VALUES ('PROG03', 'BAT003', 'Dockerization & CI/CD Pipelines');

INSERT INTO BATCH_TITLE_PROGRAM (Program_Id, Batch_No, Title)
VALUES ('PROG04', 'BAT004', 'Dynamic Programming & Graph Theory');

INSERT INTO BATCH_TITLE_PROGRAM (Program_Id, Batch_No, Title)
VALUES ('PROG05', 'BAT005', 'SIEM Monitoring & Penetration Testing');

-- ------------------------------------------------------------
-- 9. ASSESSMENT (Assessment_Id, Program_Id, Batch_No, Title, Max_Marks, Flags, Eligibility_Criteria)
-- ------------------------------------------------------------
INSERT INTO ASSESSMENT (Assessment_Id, Program_Id, Batch_No, Title, Max_Marks, Beginner, Intermediate, Advanced, Eligibility_Criteria)
VALUES ('ASM001', 'PROG01', 'BAT001', 'Spring Boot & Microservice Architecture', 100, 'N', 'Y', 'N', 'Attendance >= 80% and CGPA >= 7.0');

INSERT INTO ASSESSMENT (Assessment_Id, Program_Id, Batch_No, Title, Max_Marks, Beginner, Intermediate, Advanced, Eligibility_Criteria)
VALUES ('ASM002', 'PROG02', 'BAT002', 'Transformer Models & LLM Fine-tuning', 100, 'N', 'N', 'Y', 'Attendance >= 85% and CGPA >= 8.0');

INSERT INTO ASSESSMENT (Assessment_Id, Program_Id, Batch_No, Title, Max_Marks, Beginner, Intermediate, Advanced, Eligibility_Criteria)
VALUES ('ASM003', 'PROG03', 'BAT003', 'Dockerization & CI/CD Pipelines', 100, 'N', 'Y', 'N', 'Attendance >= 75% and CGPA >= 7.0');

INSERT INTO ASSESSMENT (Assessment_Id, Program_Id, Batch_No, Title, Max_Marks, Beginner, Intermediate, Advanced, Eligibility_Criteria)
VALUES ('ASM004', 'PROG04', 'BAT004', 'Dynamic Programming & Graph Theory', 100, 'Y', 'N', 'N', 'Attendance >= 70% and CGPA >= 6.5');

INSERT INTO ASSESSMENT (Assessment_Id, Program_Id, Batch_No, Title, Max_Marks, Beginner, Intermediate, Advanced, Eligibility_Criteria)
VALUES ('ASM005', 'PROG05', 'BAT005', 'SIEM Monitoring & Penetration Testing', 100, 'N', 'N', 'Y', 'Attendance >= 85% and CGPA >= 7.5');

-- ------------------------------------------------------------
-- 10. EMAIL_COMPANY & 11. COMPANY (10 Companies)
-- ------------------------------------------------------------
INSERT INTO EMAIL_COMPANY (Email, Company_Name) VALUES ('campus@tcs.com', 'Tata Consultancy Services');
INSERT INTO COMPANY (Company_Id, Email, Industry) VALUES ('COM001', 'campus@tcs.com', 'IT Services & Consulting');

INSERT INTO EMAIL_COMPANY (Email, Company_Name) VALUES ('talent@infosys.com', 'Infosys Limited');
INSERT INTO COMPANY (Company_Id, Email, Industry) VALUES ('COM002', 'talent@infosys.com', 'IT Services & Consulting');

INSERT INTO EMAIL_COMPANY (Email, Company_Name) VALUES ('careers@wipro.com', 'Wipro Enterprises');
INSERT INTO COMPANY (Company_Id, Email, Industry) VALUES ('COM003', 'careers@wipro.com', 'IT Services & Consulting');

INSERT INTO EMAIL_COMPANY (Email, Company_Name) VALUES ('indiarecruiting@microsoft.com', 'Microsoft India');
INSERT INTO COMPANY (Company_Id, Email, Industry) VALUES ('COM004', 'indiarecruiting@microsoft.com', 'Software Products & Cloud');

INSERT INTO EMAIL_COMPANY (Email, Company_Name) VALUES ('campus-in@amazon.com', 'Amazon Development Centre');
INSERT INTO COMPANY (Company_Id, Email, Industry) VALUES ('COM005', 'campus-in@amazon.com', 'E-Commerce & Cloud Computing');

INSERT INTO EMAIL_COMPANY (Email, Company_Name) VALUES ('campus_recruitment@oracle.com', 'Oracle India Pvt Ltd');
INSERT INTO COMPANY (Company_Id, Email, Industry) VALUES ('COM006', 'campus_recruitment@oracle.com', 'Enterprise Software & Database');

INSERT INTO EMAIL_COMPANY (Email, Company_Name) VALUES ('campuscareers@gs.com', 'Goldman Sachs Services');
INSERT INTO COMPANY (Company_Id, Email, Industry) VALUES ('COM007', 'campuscareers@gs.com', 'Financial Services & FinTech');

INSERT INTO EMAIL_COMPANY (Email, Company_Name) VALUES ('talent_apac@cisco.com', 'Cisco Systems India');
INSERT INTO COMPANY (Company_Id, Email, Industry) VALUES ('COM008', 'talent_apac@cisco.com', 'Networking & Cyber Security');

INSERT INTO EMAIL_COMPANY (Email, Company_Name) VALUES ('hiring@flipkart.com', 'Flipkart Internet Pvt Ltd');
INSERT INTO COMPANY (Company_Id, Email, Industry) VALUES ('COM009', 'hiring@flipkart.com', 'E-Commerce & Retail Tech');

INSERT INTO EMAIL_COMPANY (Email, Company_Name) VALUES ('earlycareers@zomato.com', 'Zomato Limited');
INSERT INTO COMPANY (Company_Id, Email, Industry) VALUES ('COM010', 'earlycareers@zomato.com', 'FoodTech & Consumer Internet');

-- ------------------------------------------------------------
-- 12. COMPANY_PHONE
-- ------------------------------------------------------------
INSERT INTO COMPANY_PHONE (Company_Id, Phone_No) VALUES ('COM001', '+91-22-67789999');
INSERT INTO COMPANY_PHONE (Company_Id, Phone_No) VALUES ('COM002', '+91-80-28520261');
INSERT INTO COMPANY_PHONE (Company_Id, Phone_No) VALUES ('COM003', '+91-80-28440011');
INSERT INTO COMPANY_PHONE (Company_Id, Phone_No) VALUES ('COM004', '+91-40-66930000');
INSERT INTO COMPANY_PHONE (Company_Id, Phone_No) VALUES ('COM005', '+91-80-41030000');
INSERT INTO COMPANY_PHONE (Company_Id, Phone_No) VALUES ('COM006', '+91-80-41084000');
INSERT INTO COMPANY_PHONE (Company_Id, Phone_No) VALUES ('COM007', '+91-80-41278000');
INSERT INTO COMPANY_PHONE (Company_Id, Phone_No) VALUES ('COM008', '+91-80-44260000');
INSERT INTO COMPANY_PHONE (Company_Id, Phone_No) VALUES ('COM009', '+91-80-67981111');
INSERT INTO COMPANY_PHONE (Company_Id, Phone_No) VALUES ('COM010', '+91-124-4268000');

-- ------------------------------------------------------------
-- 13. JOB_COMPANY
-- ------------------------------------------------------------
INSERT INTO JOB_COMPANY (Job_Title, Company_Id) VALUES ('Associate Software Engineer', 'COM001');
INSERT INTO JOB_COMPANY (Job_Title, Company_Id) VALUES ('Systems Engineer - Specialist', 'COM002');
INSERT INTO JOB_COMPANY (Job_Title, Company_Id) VALUES ('Project Engineer', 'COM003');
INSERT INTO JOB_COMPANY (Job_Title, Company_Id) VALUES ('Software Development Engineer I', 'COM004');
INSERT INTO JOB_COMPANY (Job_Title, Company_Id) VALUES ('Cloud Support Associate', 'COM005');
INSERT INTO JOB_COMPANY (Job_Title, Company_Id) VALUES ('Associate Applications Engineer', 'COM006');
INSERT INTO JOB_COMPANY (Job_Title, Company_Id) VALUES ('Technology Analyst', 'COM007');
INSERT INTO JOB_COMPANY (Job_Title, Company_Id) VALUES ('Network Consulting Engineer', 'COM008');
INSERT INTO JOB_COMPANY (Job_Title, Company_Id) VALUES ('Backend Software Engineer', 'COM009');
INSERT INTO JOB_COMPANY (Job_Title, Company_Id) VALUES ('Software Engineer - Platform', 'COM010');

-- ------------------------------------------------------------
-- 14. PLACEMENT_DRIVE
-- ------------------------------------------------------------
INSERT INTO PLACEMENT_DRIVE (Drive_Id, Job_Title, Min_CGPA) VALUES ('DRV001', 'Software Development Engineer I', 8.50);
INSERT INTO PLACEMENT_DRIVE (Drive_Id, Job_Title, Min_CGPA) VALUES ('DRV002', 'Associate Software Engineer', 6.50);
INSERT INTO PLACEMENT_DRIVE (Drive_Id, Job_Title, Min_CGPA) VALUES ('DRV003', 'Systems Engineer - Specialist', 7.00);
INSERT INTO PLACEMENT_DRIVE (Drive_Id, Job_Title, Min_CGPA) VALUES ('DRV004', 'Cloud Support Associate', 7.50);
INSERT INTO PLACEMENT_DRIVE (Drive_Id, Job_Title, Min_CGPA) VALUES ('DRV005', 'Associate Applications Engineer', 8.00);
INSERT INTO PLACEMENT_DRIVE (Drive_Id, Job_Title, Min_CGPA) VALUES ('DRV006', 'Technology Analyst', 8.50);
INSERT INTO PLACEMENT_DRIVE (Drive_Id, Job_Title, Min_CGPA) VALUES ('DRV007', 'Backend Software Engineer', 8.00);
INSERT INTO PLACEMENT_DRIVE (Drive_Id, Job_Title, Min_CGPA) VALUES ('DRV008', 'Network Consulting Engineer', 7.50);

-- ------------------------------------------------------------
-- 15. STUDENT_DAILY_ROUTINE_DRIVE & 16. APPLICATION (35 Applications)
-- ------------------------------------------------------------
-- Day 1: 2025-09-01
INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU001', TO_DATE('2025-09-01', 'YYYY-MM-DD'), 'DRV001');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP001', 'STU001', TO_DATE('2025-09-01', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU004', TO_DATE('2025-09-01', 'YYYY-MM-DD'), 'DRV001');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP002', 'STU004', TO_DATE('2025-09-01', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU008', TO_DATE('2025-09-01', 'YYYY-MM-DD'), 'DRV001');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP003', 'STU008', TO_DATE('2025-09-01', 'YYYY-MM-DD'), 'INTERVIEWING');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU012', TO_DATE('2025-09-01', 'YYYY-MM-DD'), 'DRV001');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP004', 'STU012', TO_DATE('2025-09-01', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU021', TO_DATE('2025-09-01', 'YYYY-MM-DD'), 'DRV001');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP005', 'STU021', TO_DATE('2025-09-01', 'YYYY-MM-DD'), 'SELECTED');

-- Day 2: 2025-09-02
INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU002', TO_DATE('2025-09-02', 'YYYY-MM-DD'), 'DRV002');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP006', 'STU002', TO_DATE('2025-09-02', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU005', TO_DATE('2025-09-02', 'YYYY-MM-DD'), 'DRV002');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP007', 'STU005', TO_DATE('2025-09-02', 'YYYY-MM-DD'), 'SHORTLISTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU009', TO_DATE('2025-09-02', 'YYYY-MM-DD'), 'DRV002');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP008', 'STU009', TO_DATE('2025-09-02', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU013', TO_DATE('2025-09-02', 'YYYY-MM-DD'), 'DRV002');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP009', 'STU013', TO_DATE('2025-09-02', 'YYYY-MM-DD'), 'APPLIED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU015', TO_DATE('2025-09-02', 'YYYY-MM-DD'), 'DRV002');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP010', 'STU015', TO_DATE('2025-09-02', 'YYYY-MM-DD'), 'SHORTLISTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU019', TO_DATE('2025-09-02', 'YYYY-MM-DD'), 'DRV002');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP011', 'STU019', TO_DATE('2025-09-02', 'YYYY-MM-DD'), 'REJECTED');

-- Day 3: 2025-09-03
INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU003', TO_DATE('2025-09-03', 'YYYY-MM-DD'), 'DRV003');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP012', 'STU003', TO_DATE('2025-09-03', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU006', TO_DATE('2025-09-03', 'YYYY-MM-DD'), 'DRV003');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP013', 'STU006', TO_DATE('2025-09-03', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU007', TO_DATE('2025-09-03', 'YYYY-MM-DD'), 'DRV003');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP014', 'STU007', TO_DATE('2025-09-03', 'YYYY-MM-DD'), 'INTERVIEWING');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU010', TO_DATE('2025-09-03', 'YYYY-MM-DD'), 'DRV003');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP015', 'STU010', TO_DATE('2025-09-03', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU011', TO_DATE('2025-09-03', 'YYYY-MM-DD'), 'DRV003');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP016', 'STU011', TO_DATE('2025-09-03', 'YYYY-MM-DD'), 'SHORTLISTED');

-- Day 4: 2025-09-04
INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU001', TO_DATE('2025-09-04', 'YYYY-MM-DD'), 'DRV005');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP017', 'STU001', TO_DATE('2025-09-04', 'YYYY-MM-DD'), 'SHORTLISTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU002', TO_DATE('2025-09-04', 'YYYY-MM-DD'), 'DRV005');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP018', 'STU002', TO_DATE('2025-09-04', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU008', TO_DATE('2025-09-04', 'YYYY-MM-DD'), 'DRV005');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP019', 'STU008', TO_DATE('2025-09-04', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU014', TO_DATE('2025-09-04', 'YYYY-MM-DD'), 'DRV005');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP020', 'STU014', TO_DATE('2025-09-04', 'YYYY-MM-DD'), 'INTERVIEWING');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU018', TO_DATE('2025-09-04', 'YYYY-MM-DD'), 'DRV005');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP021', 'STU018', TO_DATE('2025-09-04', 'YYYY-MM-DD'), 'SELECTED');

-- Day 5: 2025-09-05
INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU003', TO_DATE('2025-09-05', 'YYYY-MM-DD'), 'DRV004');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP022', 'STU003', TO_DATE('2025-09-05', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU006', TO_DATE('2025-09-05', 'YYYY-MM-DD'), 'DRV004');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP023', 'STU006', TO_DATE('2025-09-05', 'YYYY-MM-DD'), 'SHORTLISTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU007', TO_DATE('2025-09-05', 'YYYY-MM-DD'), 'DRV004');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP024', 'STU007', TO_DATE('2025-09-05', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU011', TO_DATE('2025-09-05', 'YYYY-MM-DD'), 'DRV004');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP025', 'STU011', TO_DATE('2025-09-05', 'YYYY-MM-DD'), 'APPLIED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU017', TO_DATE('2025-09-05', 'YYYY-MM-DD'), 'DRV004');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP026', 'STU017', TO_DATE('2025-09-05', 'YYYY-MM-DD'), 'SELECTED');

-- Day 6: 2025-09-06
INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU004', TO_DATE('2025-09-06', 'YYYY-MM-DD'), 'DRV006');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP027', 'STU004', TO_DATE('2025-09-06', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU012', TO_DATE('2025-09-06', 'YYYY-MM-DD'), 'DRV006');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP028', 'STU012', TO_DATE('2025-09-06', 'YYYY-MM-DD'), 'INTERVIEWING');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU021', TO_DATE('2025-09-06', 'YYYY-MM-DD'), 'DRV006');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP029', 'STU021', TO_DATE('2025-09-06', 'YYYY-MM-DD'), 'SHORTLISTED');

-- Day 7: 2025-09-07
INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU010', TO_DATE('2025-09-07', 'YYYY-MM-DD'), 'DRV007');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP030', 'STU010', TO_DATE('2025-09-07', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU016', TO_DATE('2025-09-07', 'YYYY-MM-DD'), 'DRV007');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP031', 'STU016', TO_DATE('2025-09-07', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU020', TO_DATE('2025-09-07', 'YYYY-MM-DD'), 'DRV007');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP032', 'STU020', TO_DATE('2025-09-07', 'YYYY-MM-DD'), 'APPLIED');

-- Day 8: 2025-09-08
INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU005', TO_DATE('2025-09-08', 'YYYY-MM-DD'), 'DRV008');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP033', 'STU005', TO_DATE('2025-09-08', 'YYYY-MM-DD'), 'SELECTED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU011', TO_DATE('2025-09-08', 'YYYY-MM-DD'), 'DRV008');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP034', 'STU011', TO_DATE('2025-09-08', 'YYYY-MM-DD'), 'APPLIED');

INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES ('STU022', TO_DATE('2025-09-08', 'YYYY-MM-DD'), 'DRV008');
INSERT INTO APPLICATION (Application_Id, Student_Id, Apply_Date, Status) VALUES ('APP035', 'STU022', TO_DATE('2025-09-08', 'YYYY-MM-DD'), 'SHORTLISTED');

-- ------------------------------------------------------------
-- 17. INTERVIEWER_ROUND
-- ------------------------------------------------------------
INSERT INTO INTERVIEWER_ROUND (Interviewer_Name, Interview_Round_No) VALUES ('Vikram Malhotra', 1);
INSERT INTO INTERVIEWER_ROUND (Interviewer_Name, Interview_Round_No) VALUES ('Neeraj Chopra', 1);
INSERT INTO INTERVIEWER_ROUND (Interviewer_Name, Interview_Round_No) VALUES ('Sangeeta Rao', 2);
INSERT INTO INTERVIEWER_ROUND (Interviewer_Name, Interview_Round_No) VALUES ('Rajeshwari Iyer', 2);
INSERT INTO INTERVIEWER_ROUND (Interviewer_Name, Interview_Round_No) VALUES ('Arvind Swamy', 3);
INSERT INTO INTERVIEWER_ROUND (Interviewer_Name, Interview_Round_No) VALUES ('Meenakshi Sundaram', 3);

-- ------------------------------------------------------------
-- 18. INTERVIEW
-- ------------------------------------------------------------
INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP001', 'Vikram Malhotra', 95.0, 88.0, 92.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP002', 'Neeraj Chopra', 92.0, 90.0, 95.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP003', 'Vikram Malhotra', 78.0, 80.0, NULL, 'PENDING', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP004', 'Neeraj Chopra', 94.0, 89.0, 91.0, 'CLEARED', 'N', 'Y');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP005', 'Vikram Malhotra', 98.0, 95.0, 96.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP006', 'Sangeeta Rao', 85.0, 82.0, 88.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP008', 'Rajeshwari Iyer', 88.0, 84.0, 86.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP012', 'Sangeeta Rao', 87.0, 85.0, 90.0, 'CLEARED', 'N', 'Y');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP013', 'Rajeshwari Iyer', 90.0, 86.0, 89.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP015', 'Sangeeta Rao', 92.0, 88.0, 93.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP018', 'Arvind Swamy', 91.0, 90.0, 92.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP019', 'Meenakshi Sundaram', 93.0, 89.0, 94.0, 'CLEARED', 'N', 'Y');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP021', 'Arvind Swamy', 95.0, 92.0, 93.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP022', 'Arvind Swamy', 89.0, 85.0, 87.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP024', 'Meenakshi Sundaram', 88.0, 86.0, 88.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP026', 'Arvind Swamy', 87.0, 84.0, 85.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP027', 'Meenakshi Sundaram', 96.0, 94.0, 95.0, 'CLEARED', 'N', 'Y');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP030', 'Arvind Swamy', 93.0, 90.0, 91.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP031', 'Meenakshi Sundaram', 92.0, 88.0, 90.0, 'CLEARED', 'Y', 'N');

INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, OA, GD, HR, Result, "ONLINE", "OFFLINE")
VALUES ('APP033', 'Arvind Swamy', 86.0, 85.0, 88.0, 'CLEARED', 'Y', 'N');

-- ------------------------------------------------------------
-- 19. OFFER_LETTER (14 Offers)
-- ------------------------------------------------------------
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF001', 'APP001', TO_DATE('2025-09-15', 'YYYY-MM-DD'), 43.50);
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF002', 'APP002', TO_DATE('2025-09-15', 'YYYY-MM-DD'), 43.50);
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF003', 'APP004', TO_DATE('2025-09-15', 'YYYY-MM-DD'), 43.50);
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF004', 'APP005', TO_DATE('2025-09-15', 'YYYY-MM-DD'), 44.00);
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF005', 'APP006', TO_DATE('2025-09-16', 'YYYY-MM-DD'), 7.20);
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF006', 'APP008', TO_DATE('2025-09-16', 'YYYY-MM-DD'), 7.20);
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF007', 'APP012', TO_DATE('2025-09-18', 'YYYY-MM-DD'), 9.50);
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF008', 'APP013', TO_DATE('2025-09-18', 'YYYY-MM-DD'), 9.50);
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF009', 'APP015', TO_DATE('2025-09-18', 'YYYY-MM-DD'), 9.50);
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF010', 'APP018', TO_DATE('2025-09-20', 'YYYY-MM-DD'), 18.00);
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF011', 'APP019', TO_DATE('2025-09-20', 'YYYY-MM-DD'), 18.00);
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF012', 'APP021', TO_DATE('2025-09-20', 'YYYY-MM-DD'), 18.00);
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF013', 'APP022', TO_DATE('2025-09-22', 'YYYY-MM-DD'), 16.50);
INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA) VALUES ('OFF014', 'APP024', TO_DATE('2025-09-22', 'YYYY-MM-DD'), 16.50);

-- ------------------------------------------------------------
-- 20. PORTAL_USER (Student, Recruiter, Admin)
-- Passwords below are standard bcrypt hash for 'password123'
-- ------------------------------------------------------------
INSERT INTO PORTAL_USER (User_Id, Username, Password_Hash, Role, Reference_Id)
VALUES ('USR001', 'student1', '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK', 'ROLE_STUDENT', 'STU001');

INSERT INTO PORTAL_USER (User_Id, Username, Password_Hash, Role, Reference_Id)
VALUES ('USR002', 'student2', '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK', 'ROLE_STUDENT', 'STU002');

INSERT INTO PORTAL_USER (User_Id, Username, Password_Hash, Role, Reference_Id)
VALUES ('USR003', 'recruiter1', '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK', 'ROLE_RECRUITER', 'COM004');

INSERT INTO PORTAL_USER (User_Id, Username, Password_Hash, Role, Reference_Id)
VALUES ('USR004', 'recruiter2', '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK', 'ROLE_RECRUITER', 'COM001');

INSERT INTO PORTAL_USER (User_Id, Username, Password_Hash, Role, Reference_Id)
VALUES ('USR005', 'admin', '$2a$10$wT4c17.5i4Zom2v/h07yUOi/eJc9k3m0Eslk4s4YfD9M0qYQ7c6wK', 'ROLE_ADMIN', 'ADMIN01');

COMMIT;
