-- ============================================================
-- V11__student_resume_and_evaluation.sql
-- DA2 Schema Extension: Student Resume (Oracle BLOB),
-- Resume Evaluation, Student Skills, and Drive Details
-- ============================================================

-- 1. STUDENT_SKILL (Student technical and professional skills)
CREATE TABLE STUDENT_SKILL (
    Student_Id   VARCHAR2(20)  NOT NULL,
    Skill_Name   VARCHAR2(100) NOT NULL,
    Proficiency  VARCHAR2(30)  DEFAULT 'Intermediate' NOT NULL,
    CONSTRAINT PK_STUDENT_SKILL PRIMARY KEY (Student_Id, Skill_Name),
    CONSTRAINT FK_SKILL_STUDENT FOREIGN KEY (Student_Id) REFERENCES STUDENT(Student_Id) ON DELETE CASCADE
);

-- 2. STUDENT_RESUME (Oracle BLOB/SecureFiles binary resume storage)
CREATE TABLE STUDENT_RESUME (
    Resume_Id    VARCHAR2(50)  NOT NULL,
    Student_Id   VARCHAR2(20)  NOT NULL,
    File_Name    VARCHAR2(255) NOT NULL,
    Content_Type VARCHAR2(100) NOT NULL,
    File_Size    NUMBER(12)    NOT NULL,
    Resume_Data  BLOB          NOT NULL,
    Uploaded_At  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    Version_No   NUMBER(3)     DEFAULT 1 NOT NULL,
    Is_Current   CHAR(1)       DEFAULT 'Y' NOT NULL,
    CONSTRAINT PK_STUDENT_RESUME PRIMARY KEY (Resume_Id),
    CONSTRAINT FK_RESUME_STUDENT FOREIGN KEY (Student_Id) REFERENCES STUDENT(Student_Id) ON DELETE CASCADE,
    CONSTRAINT CHK_RESUME_IS_CURRENT CHECK (Is_Current IN ('Y', 'N'))
);

-- Index for fast lookup of student resumes
CREATE INDEX IDX_RESUME_STUDENT ON STUDENT_RESUME(Student_Id, Is_Current);

-- 3. RESUME_EVALUATION (Structured recruiter candidate evaluation)
CREATE TABLE RESUME_EVALUATION (
    Evaluation_Id    VARCHAR2(50)  NOT NULL,
    Resume_Id        VARCHAR2(50)  NOT NULL,
    Application_Id   VARCHAR2(20)  NOT NULL,
    Recruiter_Id     VARCHAR2(50)  NOT NULL,
    Technical_Score  NUMBER(3)     DEFAULT 0 NOT NULL,
    Education_Score  NUMBER(3)     DEFAULT 0 NOT NULL,
    Project_Score    NUMBER(3)     DEFAULT 0 NOT NULL,
    Experience_Score NUMBER(3)     DEFAULT 0 NOT NULL,
    Overall_Score    NUMBER(3)     DEFAULT 0 NOT NULL,
    Comments         VARCHAR2(1000),
    Evaluated_At     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT PK_RESUME_EVALUATION PRIMARY KEY (Evaluation_Id),
    CONSTRAINT FK_EVAL_RESUME FOREIGN KEY (Resume_Id) REFERENCES STUDENT_RESUME(Resume_Id) ON DELETE CASCADE,
    CONSTRAINT FK_EVAL_APP FOREIGN KEY (Application_Id) REFERENCES APPLICATION(Application_Id) ON DELETE CASCADE,
    CONSTRAINT CHK_TECH_SCORE CHECK (Technical_Score BETWEEN 0 AND 100),
    CONSTRAINT CHK_EDU_SCORE CHECK (Education_Score BETWEEN 0 AND 100),
    CONSTRAINT CHK_PROJ_SCORE CHECK (Project_Score BETWEEN 0 AND 100),
    CONSTRAINT CHK_EXP_SCORE CHECK (Experience_Score BETWEEN 0 AND 100),
    CONSTRAINT CHK_OVERALL_SCORE CHECK (Overall_Score BETWEEN 0 AND 100)
);

-- Index for application evaluation lookup
CREATE INDEX IDX_EVAL_APP ON RESUME_EVALUATION(Application_Id);

-- 4. Enrich PLACEMENT_DRIVE with detailed recruitment criteria
ALTER TABLE PLACEMENT_DRIVE ADD (
    Job_Description    VARCHAR2(1000),
    Location           VARCHAR2(100) DEFAULT 'Bengaluru / Hyderabad' NOT NULL,
    Eligible_Branches  VARCHAR2(200) DEFAULT 'CSE, IT, ECE, Data Science' NOT NULL,
    Selection_Process  VARCHAR2(500) DEFAULT '1. Online Assessment | 2. Technical Interview | 3. HR Interview' NOT NULL
);
