# DA1 Database Model Analysis & Relational Normalization Specification

**Project**: Student Placement and Recruitment Portal  
**Document Code**: DA1 Extraction & Specification  
**Target Engine**: Oracle Database 19c / 21c / 23ai  
**Application Architecture**: Spring Boot 3.4+ Modular Monolith + Next.js 16  

---

## 1. Executive Summary & Source of Truth

This document captures the definitive database model extracted from the academic Project Design Assignment 1 (DA1). The model addresses the lifecycle of student campus placements, from skill-building programs and batch assessments to company job postings, placement drives, daily application limits, multi-round interviews, and offer letters.

The relational design was normalized from unnormalized multi-attribute forms through 1NF, 2NF, 3NF, and into Boyce-Codd Normal Form (BCNF). This specification defines all entities, candidate keys, functional dependencies (FDs), normalization steps, flags/ambiguities, and the formal mapping to the DA2 database implementation.

---

## 2. Conceptual & Relational Extraction

### 2.1 Base Entities & Descriptions

| Entity / Relation | Semantic Meaning | Key Business Role |
|---|---|---|
| **STUDENT** | University students seeking placement | Core profile, contact info, DOB (Age derived) |
| **STUDENT_PHONE** | Multi-valued student phone numbers | Contact channels for students |
| **PROGRAM** | Pre-placement training programs | Course duration, fees, difficulty flags |
| **REGISTERS** | Enrollment of students in programs | Registration date tracking |
| **BATCH** | Training batch offerings of programs | Physical room, daily schedule, dates |
| **BATCH_PROGRAM** | BCNF decomposed batch-to-program mapping | Enforces global batch ownership |
| **ATTENDS** | Student batch attendance records | Tracking participation in specific batches |
| **BATCH_TITLE_PROGRAM** | Topic/title module within a batch | Scopes curriculum modules |
| **ASSESSMENT** | Skill assessment tests for batches | Marks, difficulty flags, eligibility rules |
| **EMAIL_COMPANY** | Corporate contact email registry | 1NF/BCNF decomposed company registry |
| **COMPANY** | Recruiting partner organizations | Industry classification, corporate ID |
| **COMPANY_PHONE** | Multi-valued company phone contacts | Corporate communication channels |
| **JOB_COMPANY** | Job designations posted by companies | Roles available for placement drives |
| **PLACEMENT_DRIVE** | Campus recruitment drive events | Minimum CGPA threshold, target job |
| **STUDENT_DAILY_ROUTINE_DRIVE** | Daily application throttling relation | Limits student to at most 1 drive per day |
| **APPLICATION** | Formal job application instance | Current recruitment lifecycle status |
| **INTERVIEWER_ROUND** | Interviewer specialized round mapping | Links interviewer to specific round |
| **INTERVIEW** | Multi-round interview evaluations | OA, GD, HR scores, result, delivery mode |
| **OFFER_LETTER** | Official job offer issued | CTC in LPA, offer date, 1:1 with application |
| **APPLICATION_AUDIT** | Operational audit log (DA2 extension) | Logs status transitions via DB triggers |

---

## 3. Functional Dependencies & Normalization Proofs (1NF → BCNF)

### 3.1 STUDENT
- **Initial Attributes**: `{Student_Id, Name, Email, Street, City, State, DOB, Age, Phone_Numbers}`
- **1NF**: Removed multi-valued `Phone_Numbers` into separate relation `STUDENT_PHONE(Student_Id, Phone)`. Removed derived attribute `Age` (computed dynamically as `TRUNC(MONTHS_BETWEEN(SYSDATE, DOB)/12)`).
- **Candidate Key**: `{Student_Id}`, `{Email}`
- **Primary Key**: `Student_Id`
- **FDs**:
  - $FD_1: \text{Student\_Id} \rightarrow \text{Name, Email, Street, City, State, DOB}$
  - $FD_2: \text{Email} \rightarrow \text{Student\_Id, Name, Street, City, State, DOB}$
- **2NF / 3NF / BCNF Proof**: Every determinant is a candidate key. Relation is in BCNF.

### 3.2 PROGRAM
- **Attributes**: `{Program_Id, Program_Name, Fee, Duration, Beginner, Intermediate, Advanced}`
- **Candidate Key**: `{Program_Id}`
- **Primary Key**: `Program_Id`
- **FDs**:
  - $FD_1: \text{Program\_Id} \rightarrow \text{Program\_Name, Fee, Duration, Beginner, Intermediate, Advanced}$
- **2NF / 3NF / BCNF Proof**: Determining key is superkey. Single-valued level constraint enforced via table check constraints. Relation is in BCNF.

### 3.3 REGISTERS
- **Attributes**: `{Student_Id, Program_Id, Reg_Date}`
- **Candidate Key**: `{Student_Id, Program_Id}`
- **Primary Key**: `(Student_Id, Program_Id)`
- **FDs**:
  - $FD_1: \text{Student\_Id, Program\_Id} \rightarrow \text{Reg\_Date}$
- **2NF / 3NF / BCNF Proof**: Determinant is the entire candidate key. Relation is in BCNF.

### 3.4 BATCH, BATCH_PROGRAM & ATTENDS (Ambiguity 1 Resolution)
- **Original Unnormalized Form**: `ATTENDS(Student_Id, Program_Id, Batch_No, Schedule, Room, Start_Date, End_Date)`
- **FDs Identified**:
  - $FD_1: \text{Batch\_No} \rightarrow \text{Program\_Id, Schedule, Room, Start\_Date, End\_Date}$
  - $FD_2: \text{Student\_Id, Batch\_No} \rightarrow \text{Attendance\_Status}$
- **BCNF Decomposition**:
  1. `BATCH(Batch_No PK, Program_Id FK, Schedule, Room, Start_Date, End_Date)`
  2. `BATCH_PROGRAM(Batch_No PK/FK, Program_Id FK)`
  3. `ATTENDS(Student_Id PK/FK, Batch_No PK/FK)`
- **Proof**: Treating `Batch_No` as globally unique ensures all determinants are candidate keys, satisfying BCNF without multi-attribute key anomalies.

### 3.5 BATCH_TITLE_PROGRAM & ASSESSMENT
- **Attributes**:
  - `BATCH_TITLE_PROGRAM`: `{Batch_No, Title, Program_Id}`
  - `ASSESSMENT`: `{Assessment_Id, Batch_No, Title, Max_Marks, Beginner, Intermediate, Advanced, Eligibility_Criteria}`
- **Candidate Keys**:
  - `BATCH_TITLE_PROGRAM`: `{Batch_No, Title}`
  - `ASSESSMENT`: `{Assessment_Id}`
- **FDs**:
  - $FD_1: \text{Batch\_No, Title} \rightarrow \text{Program\_Id}$
  - $FD_2: \text{Assessment\_Id} \rightarrow \text{Batch\_No, Title, Max\_Marks, Beginner, Intermediate, Advanced, Eligibility\_Criteria}$
- **2NF / 3NF / BCNF Proof**: Determinants are superkeys. In BCNF.

### 3.6 COMPANY, EMAIL_COMPANY & COMPANY_PHONE
- **Decomposition**:
  - `EMAIL_COMPANY(Email PK, Company_Name)`
  - `COMPANY(Company_Id PK, Email FK, Industry)`
  - `COMPANY_PHONE(Company_Id PK/FK, Phone_No PK)`
- **FDs**:
  - $FD_1: \text{Email} \rightarrow \text{Company\_Name}$
  - $FD_2: \text{Company\_Id} \rightarrow \text{Email, Industry}$
- **Proof**: Eliminates transitive dependency $\text{Company\_Id} \rightarrow \text{Email} \rightarrow \text{Company\_Name}$. In BCNF.

### 3.7 JOB_COMPANY & PLACEMENT_DRIVE
- **Relations**:
  - `JOB_COMPANY(Job_Title PK, Company_Id FK)`
  - `PLACEMENT_DRIVE(Drive_Id PK, Job_Title FK, Min_CGPA)`
- **FDs**:
  - $FD_1: \text{Job\_Title} \rightarrow \text{Company\_Id}$
  - $FD_2: \text{Drive\_Id} \rightarrow \text{Job\_Title, Min\_CGPA}$
- **Proof**: In BCNF.

### 3.8 STUDENT_DAILY_ROUTINE_DRIVE & APPLICATION
- **Relations**:
  - `STUDENT_DAILY_ROUTINE_DRIVE(Student_Id PK/FK, Apply_Date PK, Drive_Id FK)`
  - `APPLICATION(Application_Id PK, Student_Id FK, Apply_Date FK, Status)`
- **Business Constraint**: A student can submit at most one placement drive application per calendar day (`Student_Id, Apply_Date` candidate key).
- **FDs**:
  - $FD_1: \text{Student\_Id, Apply\_Date} \rightarrow \text{Drive\_Id}$
  - $FD_2: \text{Application\_Id} \rightarrow \text{Student\_Id, Apply\_Date, Status}$
- **Proof**: In BCNF.

### 3.9 INTERVIEWER_ROUND & INTERVIEW
- **Relations**:
  - `INTERVIEWER_ROUND(Interviewer_Name PK, Interview_Round_No)`
  - `INTERVIEW(Application_Id PK/FK, Interviewer_Name PK/FK, OA, GD, HR, Result, Online, Offline)`
- **FDs**:
  - $FD_1: \text{Interviewer\_Name} \rightarrow \text{Interview\_Round\_No}$
  - $FD_2: \text{Application\_Id, Interviewer\_Name} \rightarrow \text{OA, GD, HR, Result, Online, Offline}$
- **Proof**: In BCNF.

### 3.10 OFFER_LETTER
- **Relation**: `OFFER_LETTER(Offer_Id PK, Application_Id (1:1) FK, Offer_Date, CTC_LPA)`
- **FDs**:
  - $FD_1: \text{Offer\_Id} \rightarrow \text{Application\_Id, Offer\_Date, CTC\_LPA}$
  - $FD_2: \text{Application\_Id} \rightarrow \text{Offer\_Id, Offer\_Date, CTC\_LPA}$
- **Proof**: 1:1 relationship with `APPLICATION`. In BCNF.

---

## 4. Ambiguities Flagged & Academic Resolutions

| Ambiguity Item | DA1 Conflict / Observation | Chosen Resolution (DA2) | Rationale |
|---|---|---|---|
| **1. Batch_No Scope** | Table 4 used `(Program_Id, Batch_No)` PK, but Table 5 ATTENDS decomposition used `Batch_No -> Program_Id`. | **Option A**: Globally unique `Batch_No`. `BATCH` PK is `Batch_No`, `Program_Id` is FK. | Strictly preserves the BCNF decomposition `ATTENDS(Student_Id, Batch_No)` without re-introducing transitive anomalies. |
| **2. Interviewer Round Pinning** | `INTERVIEWER_ROUND` pins interviewer permanently to a single round number globally. | Preserved as `INTERVIEWER_ROUND(Interviewer_Name PK, Interview_Round_No)`. | Conforms to strict DA1 3NF/BCNF proof for panel role separation. |
| **3. EER Subtypes** | Page 3–4 EER has Undergrad/Postgrad and Online/Offline subtypes never normalized in Tables 1–13. | Implement the 13-base-table normalized BCNF schema. | Tables 1–13 are the authoritative mathematical BCNF models. Mode flags (`Online`, `Offline`) handle subtype mechanics cleanly. |
| **4. Difficulty Levels** | `Beginner`, `Intermediate`, `Advanced` stored as 3 separate flag columns. | Modeled as `CHAR(1)` columns with CHECK constraints: `IN ('Y', 'N')` and exactly one flag is `'Y'`. | Ensures atomic storage and relational compliance with DA1 attribute lists. |

---

## 5. Master DA1 → Implementation Mapping Table

| DA1 Entity | Oracle Table | Primary Key | Foreign Keys | Key Attributes | Target JPA Entity |
|---|---|---|---|---|---|
| `STUDENT` | `STUDENT` | `Student_Id` | — | `Name`, `Email`, `Street`, `City`, `State`, `DOB` | `Student` |
| `STUDENT_PHONE` | `STUDENT_PHONE` | `(Student_Id, Phone)` | `Student_Id -> STUDENT` | `Phone` | `StudentPhone` |
| `PROGRAM` | `PROGRAM` | `Program_Id` | — | `Program_Name`, `Fee`, `Duration`, Flags | `Program` |
| `REGISTERS` | `REGISTERS` | `(Student_Id, Program_Id)` | `Student_Id`, `Program_Id` | `Reg_Date` | `ProgramRegistration` |
| `BATCH` | `BATCH` | `Batch_No` | `Program_Id -> PROGRAM` | `Schedule`, `Room`, `Start_Date`, `End_Date` | `Batch` |
| `BATCH_PROGRAM` | `BATCH_PROGRAM` | `Batch_No` | `Program_Id -> PROGRAM` | `Batch_No`, `Program_Id` | `BatchProgram` |
| `ATTENDS` | `ATTENDS` | `(Student_Id, Batch_No)` | `Student_Id`, `Batch_No` | Attendance link | `BatchAttendance` |
| `BATCH_TITLE_PROGRAM` | `BATCH_TITLE_PROGRAM` | `(Batch_No, Title)` | `Batch_No`, `Program_Id` | `Title` | `BatchTitleProgram` |
| `ASSESSMENT` | `ASSESSMENT` | `Assessment_Id` | `(Batch_No, Title)` | `Max_Marks`, Flags, `Eligibility_Criteria` | `Assessment` |
| `EMAIL_COMPANY` | `EMAIL_COMPANY` | `Email` | — | `Company_Name` | `CompanyEmail` |
| `COMPANY` | `COMPANY` | `Company_Id` | `Email -> EMAIL_COMPANY` | `Industry` | `Company` |
| `COMPANY_PHONE` | `COMPANY_PHONE` | `(Company_Id, Phone_No)` | `Company_Id -> COMPANY` | `Phone_No` | `CompanyPhone` |
| `JOB_COMPANY` | `JOB_COMPANY` | `Job_Title` | `Company_Id -> COMPANY` | `Job_Title` | `JobPosting` |
| `PLACEMENT_DRIVE` | `PLACEMENT_DRIVE` | `Drive_Id` | `Job_Title -> JOB_COMPANY` | `Min_CGPA` | `PlacementDrive` |
| `STUDENT_DAILY_ROUTINE_DRIVE` | `STUDENT_DAILY_ROUTINE_DRIVE` | `(Student_Id, Apply_Date)` | `Student_Id`, `Drive_Id` | Throttled application slot | `StudentDailyRoutineDrive` |
| `APPLICATION` | `APPLICATION` | `Application_Id` | `(Student_Id, Apply_Date)` | `Status` | `Application` |
| `INTERVIEWER_ROUND` | `INTERVIEWER_ROUND` | `Interviewer_Name` | — | `Interview_Round_No` | `InterviewerRound` |
| `INTERVIEW` | `INTERVIEW` | `(Application_Id, Interviewer_Name)` | `Application_Id`, `Interviewer_Name` | `OA`, `GD`, `HR`, `Result`, `Online`, `Offline` | `Interview` |
| `OFFER_LETTER` | `OFFER_LETTER` | `Offer_Id` | `Application_Id -> APPLICATION` (1:1) | `Offer_Date`, `CTC_LPA` | `OfferLetter` |
| — | `APPLICATION_AUDIT` | `Audit_Id` | `Application_Id` | `Old_Status`, `New_Status`, `Changed_At` | `ApplicationAudit` |
| — | `PORTAL_USER` | `User_Id` | — | `Username`, `Password_Hash`, `Role` | `PortalUser` |
