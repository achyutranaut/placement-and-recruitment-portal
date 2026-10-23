-- ============================================================
-- V1__create_schema.sql
-- Base DDL Tables for Student Placement and Recruitment Portal
-- Exact BCNF Normalized Schema extracted from DA1 Model
-- (Updated per Locked Decision 1: Program-Scoped Batch_No)
-- ============================================================

-- 1. STUDENT
CREATE TABLE STUDENT (
    Student_Id   VARCHAR2(20)   NOT NULL,
    Name         VARCHAR2(100)  NOT NULL,
    Email        VARCHAR2(100)  NOT NULL,
    Street       VARCHAR2(150),
    City         VARCHAR2(50),
    State        VARCHAR2(50),
    DOB          DATE           NOT NULL,
    CGPA         NUMBER(4,2)    DEFAULT 7.00 NOT NULL,
    Branch       VARCHAR2(100)
);

-- 2. STUDENT_PHONE
CREATE TABLE STUDENT_PHONE (
    Student_Id   VARCHAR2(20)   NOT NULL,
    Phone        VARCHAR2(20)   NOT NULL
);

-- 3. PROGRAM
CREATE TABLE PROGRAM (
    Program_Id   VARCHAR2(20)   NOT NULL,
    Program_Name VARCHAR2(100)  NOT NULL,
    Fee          NUMBER(10,2)   NOT NULL,
    Duration     VARCHAR2(50)   NOT NULL,
    Beginner     CHAR(1)        DEFAULT 'N' NOT NULL,
    Intermediate CHAR(1)        DEFAULT 'N' NOT NULL,
    Advanced     CHAR(1)        DEFAULT 'N' NOT NULL
);

-- 4. REGISTERS
CREATE TABLE REGISTERS (
    Student_Id   VARCHAR2(20)   NOT NULL,
    Program_Id   VARCHAR2(20)   NOT NULL,
    Reg_Date     DATE           DEFAULT SYSDATE NOT NULL
);

-- 5. BATCH (Composite PK: Program_Id, Batch_No)
CREATE TABLE BATCH (
    Program_Id   VARCHAR2(20)   NOT NULL,
    Batch_No     VARCHAR2(20)   NOT NULL,
    Schedule     VARCHAR2(100)  NOT NULL,
    Room         VARCHAR2(50)   NOT NULL,
    Start_Date   DATE           NOT NULL,
    End_Date     DATE           NOT NULL
);

-- 6. BATCH_PROGRAM
CREATE TABLE BATCH_PROGRAM (
    Program_Id   VARCHAR2(20)   NOT NULL,
    Batch_No     VARCHAR2(20)   NOT NULL
);

-- 7. ATTENDS (Composite PK: Student_Id, Program_Id, Batch_No)
CREATE TABLE ATTENDS (
    Student_Id   VARCHAR2(20)   NOT NULL,
    Program_Id   VARCHAR2(20)   NOT NULL,
    Batch_No     VARCHAR2(20)   NOT NULL
);

-- 8. BATCH_TITLE_PROGRAM (Composite PK: Program_Id, Batch_No, Title)
CREATE TABLE BATCH_TITLE_PROGRAM (
    Program_Id   VARCHAR2(20)   NOT NULL,
    Batch_No     VARCHAR2(20)   NOT NULL,
    Title        VARCHAR2(100)  NOT NULL
);

-- 9. ASSESSMENT (FK to BATCH_TITLE_PROGRAM: Program_Id, Batch_No, Title)
CREATE TABLE ASSESSMENT (
    Assessment_Id       VARCHAR2(20)   NOT NULL,
    Program_Id          VARCHAR2(20)   NOT NULL,
    Batch_No            VARCHAR2(20)   NOT NULL,
    Title               VARCHAR2(100)  NOT NULL,
    Max_Marks           NUMBER(5,2)    NOT NULL,
    Beginner            CHAR(1)        DEFAULT 'N' NOT NULL,
    Intermediate        CHAR(1)        DEFAULT 'N' NOT NULL,
    Advanced            CHAR(1)        DEFAULT 'N' NOT NULL,
    Eligibility_Criteria VARCHAR2(200)
);

-- 10. EMAIL_COMPANY
CREATE TABLE EMAIL_COMPANY (
    Email        VARCHAR2(100)  NOT NULL,
    Company_Name VARCHAR2(100)  NOT NULL
);

-- 11. COMPANY
CREATE TABLE COMPANY (
    Company_Id   VARCHAR2(20)   NOT NULL,
    Email        VARCHAR2(100)  NOT NULL,
    Industry     VARCHAR2(100)  NOT NULL
);

-- 12. COMPANY_PHONE
CREATE TABLE COMPANY_PHONE (
    Company_Id   VARCHAR2(20)   NOT NULL,
    Phone_No     VARCHAR2(20)   NOT NULL
);

-- 13. JOB_COMPANY
CREATE TABLE JOB_COMPANY (
    Job_Title    VARCHAR2(100)  NOT NULL,
    Company_Id   VARCHAR2(20)   NOT NULL
);

-- 14. PLACEMENT_DRIVE
CREATE TABLE PLACEMENT_DRIVE (
    Drive_Id     VARCHAR2(20)   NOT NULL,
    Job_Title    VARCHAR2(100)  NOT NULL,
    Min_CGPA     NUMBER(4,2)    NOT NULL
);

-- 15. STUDENT_DAILY_ROUTINE_DRIVE
CREATE TABLE STUDENT_DAILY_ROUTINE_DRIVE (
    Student_Id   VARCHAR2(20)   NOT NULL,
    Apply_Date   DATE           NOT NULL,
    Drive_Id     VARCHAR2(20)   NOT NULL
);

-- 16. APPLICATION
CREATE TABLE APPLICATION (
    Application_Id VARCHAR2(20) NOT NULL,
    Student_Id     VARCHAR2(20) NOT NULL,
    Apply_Date     DATE         NOT NULL,
    Status         VARCHAR2(30) DEFAULT 'APPLIED' NOT NULL
);

-- 17. INTERVIEWER_ROUND
CREATE TABLE INTERVIEWER_ROUND (
    Interviewer_Name   VARCHAR2(100) NOT NULL,
    Interview_Round_No NUMBER(2)     NOT NULL
);

-- 18. INTERVIEW
CREATE TABLE INTERVIEW (
    Application_Id   VARCHAR2(20)  NOT NULL,
    Interviewer_Name VARCHAR2(100) NOT NULL,
    OA               NUMBER(5,2),
    GD               NUMBER(5,2),
    HR               NUMBER(5,2),
    Result           VARCHAR2(30)  DEFAULT 'PENDING' NOT NULL,
    "ONLINE"         CHAR(1)       DEFAULT 'N' NOT NULL,
    "OFFLINE"        CHAR(1)       DEFAULT 'N' NOT NULL
);

-- 19. OFFER_LETTER
CREATE TABLE OFFER_LETTER (
    Offer_Id       VARCHAR2(20) NOT NULL,
    Application_Id VARCHAR2(20) NOT NULL,
    Offer_Date     DATE         DEFAULT SYSDATE NOT NULL,
    CTC_LPA        NUMBER(6,2)  NOT NULL,
    Status         VARCHAR2(20) DEFAULT 'OFFERED' NOT NULL,
    Accepted_At    TIMESTAMP
);

-- 20. APPLICATION_AUDIT (Audit Trail for Triggers & Demonstrations)
CREATE TABLE APPLICATION_AUDIT (
    Audit_Id       NUMBER GENERATED ALWAYS AS IDENTITY,
    Application_Id VARCHAR2(20)  NOT NULL,
    Old_Status     VARCHAR2(30),
    New_Status     VARCHAR2(30)  NOT NULL,
    Changed_At     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    Changed_By     VARCHAR2(50)  DEFAULT 'SYSTEM' NOT NULL
);

-- 21. PORTAL_USER (Authentication & RBAC)
CREATE TABLE PORTAL_USER (
    User_Id       VARCHAR2(50)   NOT NULL,
    Username      VARCHAR2(50)   NOT NULL,
    Password_Hash VARCHAR2(255)  NOT NULL,
    Role          VARCHAR2(30)   NOT NULL,
    Reference_Id  VARCHAR2(50),
    Created_At    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL
);
