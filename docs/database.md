# Database Architecture & PL/SQL Engine (DA2)

This document provides a comprehensive technical reference for the **Student Placement and Recruitment Portal** database implemented for the DA2 academic requirement using **Oracle SQL and PL/SQL**.

---

## 1. Schema Overview

The schema strictly implements the **BCNF normalized relational model** designed and proven in DA1, augmented with security and audit relations:

```
+-----------------------------------------------------------------------------+
|                               RELATIONAL SCHEMA                             |
+-----------------------------------------------------------------------------+
 1. STUDENT (Student_Id PK, Name, Email, Street, City, State, DOB)
 2. STUDENT_PHONE (Student_Id PK/FK, Phone PK)
 3. PROGRAM (Program_Id PK, Program_Name, Fee, Duration, Beginner, Intermediate, Advanced)
 4. REGISTERS (Student_Id PK/FK, Program_Id PK/FK, Reg_Date)
 5. BATCH (Program_Id PK/FK, Batch_No PK, Schedule, Room, Start_Date, End_Date)
 6. BATCH_PROGRAM (Program_Id PK/FK, Batch_No PK/FK)
 7. ATTENDS (Student_Id PK/FK, Program_Id PK/FK, Batch_No PK/FK)
 8. BATCH_TITLE_PROGRAM (Program_Id PK/FK, Batch_No PK/FK, Title PK)
 9. ASSESSMENT (Assessment_Id PK, Program_Id FK, Batch_No FK, Title FK, Max_Marks,
               Beginner, Intermediate, Advanced, Eligibility_Criteria)
10. EMAIL_COMPANY (Email PK, Company_Name, Company_Id FK)
11. COMPANY (Company_Id PK, Industry)
12. COMPANY_PHONE (Company_Id PK/FK, Phone_No PK)
13. JOB_COMPANY (Job_Title PK, Company_Id FK)
14. PLACEMENT_DRIVE (Drive_Id PK, Job_Title FK, Min_CGPA)
15. STUDENT_DAILY_ROUTINE_DRIVE (Student_Id PK/FK, Apply_Date PK, Drive_Id FK)
16. APPLICATION (Application_Id PK, Student_Id FK, Apply_Date FK, Status)
17. INTERVIEWER_ROUND (Interviewer_Name PK, Interview_Round_No)
18. INTERVIEW (Application_Id PK/FK, Interviewer_Name PK/FK, OA, GD, HR, Result, Online, Offline)
19. OFFER_LETTER (Offer_Id PK, Application_Id (1:1) FK, Offer_Date, CTC_LPA)
20. APPLICATION_AUDIT (Audit_Id PK, Application_Id, Old_Status, New_Status, Changed_At, Changed_By)
21. PORTAL_USER (User_Id PK, Username UNIQUE, Password_Hash, Role, Reference_Id, Created_At)
```

---

## 2. Locked Academic Database Decisions

1. **Composite Batch Identifier**:
   - `BATCH` primary key is composite: `(Program_Id, Batch_No)`.
   - `Batch_No` is scoped to its parent `Program_Id`.
   - All foreign keys referencing a batch propagate both columns: `(Program_Id, Batch_No)`.
2. **Pinned Interviewer Round (`INTERVIEWER_ROUND`)**:
   - Primary key is `Interviewer_Name`, mapping permanently to `Interview_Round_No` (FD: `Interviewer_Name → Interview_Round_No`).
   - An interviewer cannot dynamically belong to multiple rounds.
3. **Flag Storage & Integrity**:
   - Boolean-style flags (`Beginner`, `Intermediate`, `Advanced`, `Online`, `Offline`) are stored as `CHAR(1)` with `CHECK (col IN ('Y', 'N'))`.
   - Mutex triggers ensure mutually exclusive level settings where required.

---

## 3. Database Migration Scripts

Located in `database/migrations/`:

| Script | Purpose | Key Artifacts |
|--------|---------|---------------|
| `V1__create_schema.sql` | Table creation with Oracle types | 21 base tables, Oracle `VARCHAR2`, `NUMBER`, `DATE` |
| `V2__constraints.sql` | Integrity constraints | Primary keys, foreign keys, unique constraints, check constraints |
| `V3__indexes.sql` | Query performance optimization | B-tree indexes on foreign keys, `STATUS`, `APPLY_DATE` |
| `V4__views.sql` | Analytical and reporting views | `V_STUDENT_APPLICATIONS`, `V_PLACEMENT_DRIVES_OVERVIEW`, `V_OFFER_SUMMARY`, `V_ELIGIBLE_STUDENTS` |
| `V5__sample_data.sql` | Realistic academic seed data | 22 students, 5 programs, 10 companies, drives, applications, offers |
| `V6__procedures.sql` | Transactional stored procedures | `REGISTER_STUDENT_PROGRAM`, `APPLY_FOR_DRIVE`, `SCHEDULE_INTERVIEW`, `ISSUE_OFFER` |
| `V7__functions.sql` | Computational functions & Ref Cursors | `GET_STUDENT_APPLICATION_COUNT`, `GET_PLACEMENT_RATE`, `GET_AVERAGE_PACKAGE`, `GET_ELIGIBLE_STUDENTS` |
| `V8__cursors.sql` | Batch processing package | `PKG_PLACEMENT_REPORTS` (explicit cursors, `%FOUND`, `%ROWCOUNT`) |
| `V9__triggers.sql` | Autonomic business rules | `TRG_OFFER_APPLICATION_STATUS`, `TRG_APPLICATION_AUDIT`, mutex triggers |
| `V10__demo_queries.sql` | Complex DQL demonstrations | Multi-table joins, aggregations with `HAVING`, `DENSE_RANK` |

---

## 4. Stored Procedures & Business Logic

### `REGISTER_STUDENT_PROGRAM`
- **Arguments**: `p_student_id`, `p_program_id`, `p_batch_no`, `p_reg_date`.
- **Validation**: Checks student existence, program existence, and composite batch existence `(Program_Id, Batch_No)`.
- **Transaction**: Sets `SAVEPOINT sp_before_reg`. Inserts into `REGISTERS` and `ATTENDS`. Rolls back to savepoint on exception.

### `APPLY_FOR_DRIVE`
- **Arguments**: `p_student_id`, `p_drive_id`, `p_apply_date`, `p_app_id` (OUT).
- **Validation**:
  - Checks if candidate's CGPA satisfies `Min_CGPA` of `PLACEMENT_DRIVE`. Raises `ORA-20002` if ineligible.
  - Checks if candidate has already applied to this drive. Raises `ORA-20003` on duplicate application.
- **Action**: Inserts record into `STUDENT_DAILY_ROUTINE_DRIVE` and `APPLICATION` with status `'APPLIED'`.

### `SCHEDULE_INTERVIEW`
- **Arguments**: `p_application_id`, `p_interviewer_name`, `p_oa`, `p_gd`, `p_hr`, `p_result`, `p_online`, `p_offline`.
- **Validation**:
  - Verifies application exists and is in `'APPLIED'`, `'SHORTLISTED'`, or `'INTERVIEWING'` status.
  - Verifies `p_interviewer_name` exists in `INTERVIEWER_ROUND`.
- **Action**: Inserts into `INTERVIEW` and updates `APPLICATION.Status = 'INTERVIEWING'`.

### `ISSUE_OFFER`
- **Arguments**: `p_application_id`, `p_offer_date`, `p_ctc_lpa`, `p_offer_id` (OUT).
- **Validation**: Ensures CTC is positive and application does not already have an offer (1:1 relationship).
- **Action**: Inserts into `OFFER_LETTER`.
- **Trigger Side Effect**: Database trigger `TRG_OFFER_APPLICATION_STATUS` automatically updates `APPLICATION.Status = 'SELECTED'` and `TRG_APPLICATION_AUDIT` records the transition.

---

## 5. Triggers & Autonomic Enforcement

### `TRG_OFFER_APPLICATION_STATUS`
- **Type**: `AFTER INSERT ON OFFER_LETTER FOR EACH ROW`
- **Logic**: Executes `UPDATE APPLICATION SET Status = 'SELECTED' WHERE Application_Id = :NEW.Application_Id`.

### `TRG_APPLICATION_AUDIT`
- **Type**: `AFTER UPDATE OF Status ON APPLICATION FOR EACH ROW`
- **Logic**: Inserts a history record into `APPLICATION_AUDIT` capturing `:OLD.Status`, `:NEW.Status`, `SYSDATE`, and `USER`.

---

## 6. Security (DCL) & Transactions (TCL)

- **DCL Scripts (`database/dcl/roles_and_grants.sql`)**:
  - `ROLE_PORTAL_STUDENT`: `SELECT` on drives, programs, and own applications.
  - `ROLE_PORTAL_RECRUITER`: `SELECT` on applicants, `INSERT` on interviews and offers.
  - `ROLE_PORTAL_ADMIN`: Full DDL/DML access and execution rights on PL/SQL packages.
- **TCL Scripts (`database/tcl/transaction_scenarios.sql`)**:
  - Demonstrates `COMMIT`, `SAVEPOINT sp_drive_apply`, and partial `ROLLBACK TO SAVEPOINT` during application failures.
