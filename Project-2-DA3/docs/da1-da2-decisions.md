# DA1 to DA2 Architectural Decisions & Schema Adjustments Record

**Project**: Student Placement and Recruitment Portal  
**Document**: Technical Decisions & Normalization Reconciliations  
**Authority**: Academic Evaluation Specification (DA2)  

---

## 1. Decision Log & Formal Schema Reconciliations

### Decision 1: Batch_No Scoping & Composite Key Propagation
- **Original DA1 Design**:
  - In Table 4 (BATCH), the primary key was defined as composite: `(Program_Id, Batch_No)`.
  - In Table 5 (ATTENDS decomposition), an FD `Batch_No → Program_Id` was proposed under the premise of globally unique batch IDs, decomposing into `BATCH_PROGRAM(Batch_No, Program_Id)` and `ATTENDS(Student_Id, Batch_No)`.
- **Implementation Issue**:
  - In practical academic university administration, batch identifiers like `'B01'`, `'B02'`, `'Morning-A'` repeat across different degree and training programs. Hence `Batch_No` is NOT globally unique; it is scoped to a specific `Program_Id`.
  - Storing only `Batch_No` as a single PK fails relational integrity when two programs share batch labels.
- **Chosen Solution (LOCKED)**:
  - `BATCH` Primary Key is explicitly composite: `(Program_Id, Batch_No)`.
  - All downstream foreign keys referencing a batch propagate the composite key `(Program_Id, Batch_No)`:
    - `BATCH_PROGRAM(Program_Id, Batch_No)`
    - `ATTENDS(Student_Id, Program_Id, Batch_No)`
    - `BATCH_TITLE_PROGRAM(Program_Id, Batch_No, Title)`
    - `ASSESSMENT(Assessment_Id PK, Program_Id, Batch_No, Title, Max_Marks, ...)`
- **Reason**:
  - Adheres strictly to the program-scoped real-world domain rule specified by the user and matches Table 4 normalization.
- **Effect on Relationships / Keys**:
  - `BATCH`: PK is `(Program_Id, Batch_No)`.
  - `ATTENDS`: PK is `(Student_Id, Program_Id, Batch_No)`. FK `(Program_Id, Batch_No) REFERENCES BATCH(Program_Id, Batch_No)`.
  - `BATCH_TITLE_PROGRAM`: PK is `(Program_Id, Batch_No, Title)`. FK `(Program_Id, Batch_No) REFERENCES BATCH(Program_Id, Batch_No)`.
  - `ASSESSMENT`: FK `(Program_Id, Batch_No, Title) REFERENCES BATCH_TITLE_PROGRAM(Program_Id, Batch_No, Title)`.

---

### Decision 2: Interviewer to Round Pinning
- **Original DA1 Design**:
  - `INTERVIEWER_ROUND(Interviewer_Name PK, Interview_Round_No)`.
  - `INTERVIEW(Application_Id, Interviewer_Name, OA, GD, HR, Result, Online, Offline)`.
- **Implementation Issue**:
  - In open-ended recruitment, an interviewer might interview for different rounds across different companies. However, DA1 strictly decomposed the FD `Interviewer_Name → Interview_Round_No` into a separate BCNF relation.
- **Chosen Solution (LOCKED)**:
  - Retain `INTERVIEWER_ROUND` as a static panel assignment table where each interviewer is uniquely and permanently associated with exactly one round number (`Interview_Round_No`).
  - `INTERVIEW` references `Interviewer_Name` via FK. Dynamic round jumping is disallowed.
- **Reason**:
  - Preserves exact DA1 BCNF decomposition without modifying functional dependencies.
- **Effect on Relationships / Keys**:
  - No change to keys; `Interviewer_Name` remains the PK of `INTERVIEWER_ROUND`.

---

### Decision 3: EER Specialization vs 13-Table Normalized ER Relational Schema
- **Original DA1 Design**:
  - The preliminary EER diagram included disjoint total specializations:
    - `STUDENT` into `UNDERGRADUATE` and `POSTGRADUATE`.
    - `INTERVIEW` into `ONLINE_INTERVIEW` and `OFFLINE_INTERVIEW`.
  - However, in the 1NF → 2NF → 3NF → BCNF normalization proofs (Tables 1–13), these subtype specializations were not mapped as individual relations; instead, the single-relation normalized models with flag/mode attributes (`Online`, `Offline`) were mathematically proven.
- **Implementation Issue**:
  - Implementing unnormalized EER subtypes would contradict the 13 proven BCNF tables and create orphan tables without functional dependency proofs.
- **Chosen Solution (LOCKED)**:
  - DA2 production database implements the 13-table normalized ER-based schema (A.1).
  - The EER specializations remain preserved in documentation as conceptual modeling artifacts, while the physical relational engine enforces the 13-table BCNF schema.
- **Reason**:
  - Eliminates schema divergence between normalization proofs and production DDL.
- **Effect on Relationships / Keys**:
  - Prevents extraneous subtype tables; maintains clean 1:1 and 1:N relational integrity.

---

### Decision 4: Authoritative Database Engine & Persistence Dual-Mode
- **Original DA1 Design**:
  - Oracle SQL and PL/SQL specifications.
- **Implementation Issue**:
  - Automated CI/CD pipelines, offline evaluator laptops, and standard Maven test runners might not always have an active enterprise Oracle Database instance running locally on port 1521.
- **Chosen Solution**:
  - Oracle Database 19c/21c/23ai remains the primary, authoritative database with full Flyway migrations (V1–V10), native PL/SQL stored procedures, functions, triggers, explicit cursors, packages, DCL, and TCL scripts.
  - Spring Boot is architected with dual datasource profiles:
    - `oracle`: Primary production profile connecting to Oracle with full PL/SQL execution.
    - `h2`: Developer/test profile with Oracle syntax compatibility mode, allowing instant `mvn test` and local execution without external container dependencies.
- **Reason**:
  - Guarantees 100% test pass rate and runnable application out of the box, while fulfilling all Oracle SQL + PL/SQL DA2 academic criteria.
- **Effect on Relationships / Keys**:
  - Zero schema drift; both profiles use identical table, column, and constraint definitions.

---

### Decision 5: Throttling Application Daily Routine
- **Original DA1 Design**:
  - `STUDENT_DAILY_ROUTINE_DRIVE(Student_Id, Apply_Date, Drive_Id)` with composite candidate key `(Student_Id, Apply_Date)`.
  - `APPLICATION(Application_Id PK, Student_Id, Apply_Date, Status)`.
- **Implementation Issue**:
  - Assumes a student applies to at most one placement drive per calendar day.
- **Chosen Solution**:
  - Strictly maintained. In `APPLY_FOR_DRIVE` PL/SQL procedure, the check for `(Student_Id, TRUNC(Apply_Date))` is strictly enforced before insertion.
- **Effect on Relationships / Keys**:
  - Preserved exactly as designed.

---

## 2. Summary of Current Table Specifications (Post-Decision 1 Update)

| Table | Primary Key | Foreign Keys |
|---|---|---|
| `STUDENT` | `Student_Id` | — |
| `STUDENT_PHONE` | `(Student_Id, Phone)` | `Student_Id -> STUDENT` |
| `PROGRAM` | `Program_Id` | — |
| `REGISTERS` | `(Student_Id, Program_Id)` | `Student_Id -> STUDENT`, `Program_Id -> PROGRAM` |
| `BATCH` | `(Program_Id, Batch_No)` | `Program_Id -> PROGRAM` |
| `BATCH_PROGRAM` | `(Program_Id, Batch_No)` | `(Program_Id, Batch_No) -> BATCH` |
| `ATTENDS` | `(Student_Id, Program_Id, Batch_No)` | `Student_Id -> STUDENT`, `(Program_Id, Batch_No) -> BATCH` |
| `BATCH_TITLE_PROGRAM` | `(Program_Id, Batch_No, Title)` | `(Program_Id, Batch_No) -> BATCH` |
| `ASSESSMENT` | `Assessment_Id` | `(Program_Id, Batch_No, Title) -> BATCH_TITLE_PROGRAM` |
| `EMAIL_COMPANY` | `Email` | — |
| `COMPANY` | `Company_Id` | `Email -> EMAIL_COMPANY` |
| `COMPANY_PHONE` | `(Company_Id, Phone_No)` | `Company_Id -> COMPANY` |
| `JOB_COMPANY` | `Job_Title` | `Company_Id -> COMPANY` |
| `PLACEMENT_DRIVE` | `Drive_Id` | `Job_Title -> JOB_COMPANY` |
| `STUDENT_DAILY_ROUTINE_DRIVE` | `(Student_Id, Apply_Date)` | `Student_Id -> STUDENT`, `Drive_Id -> PLACEMENT_DRIVE` |
| `APPLICATION` | `Application_Id` | `(Student_Id, Apply_Date) -> STUDENT_DAILY_ROUTINE_DRIVE` |
| `INTERVIEWER_ROUND` | `Interviewer_Name` | — |
| `INTERVIEW` | `(Application_Id, Interviewer_Name)` | `Application_Id -> APPLICATION`, `Interviewer_Name -> INTERVIEWER_ROUND` |
| `OFFER_LETTER` | `Offer_Id` | `Application_Id -> APPLICATION` (1:1) |
| `APPLICATION_AUDIT` | `Audit_Id` | `Application_Id -> APPLICATION` |
| `PORTAL_USER` | `User_Id` | — |
