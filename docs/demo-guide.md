# Academic Demonstration & Viva Guide (DA2)

This step-by-step walkthrough is designed for project demonstrations, academic evaluations, and viva presentations to prove that the system strictly satisfies all **DA2 syllabus requirements** (DDL, DML, DQL, DCL, TCL, and PL/SQL).

---

## Part 1: Schema Architecture Presentation (DA1 → DA2 Bridge)

1. Open `docs/da1-analysis.md` and `docs/database.md`.
2. **Point out the locked decisions**:
   - Primary key of `BATCH` is composite: `(Program_Id, Batch_No)`.
   - `INTERVIEWER_ROUND` pins each interviewer to a specific round number (`Interviewer_Name PK → Interview_Round_No`).
   - The normalized 13-base-table + 2-junction-table schema is implemented rather than the unnormalized EER specialization.
3. Show the database migration folder `database/migrations/`:
   - `V1__create_schema.sql` (21 normalized tables)
   - `V2__constraints.sql` (PKs, FKs with cascade, Check constraints for flags)
   - `V3__indexes.sql` (B-tree indexes on foreign keys)
   - `V4__views.sql` (4 analytical views)
   - `V6__procedures.sql` (Transactional stored procedures)
   - `V7__functions.sql` (Ref cursor & computational functions)
   - `V8__cursors.sql` (Cursor-driven batch report package)
   - `V9__triggers.sql` (Autonomic database triggers)

---

## Part 2: Interactive Demonstration Script

### Step 1: Open the Application
- Launch the frontend: `http://localhost:3000`
- Click the **Student Demo** quick-chip (`student1` / `password123`) and click **Sign In**.

### Step 2: Student Experience & PL/SQL Invocation
1. **Inspect Profile**:
   - Candidate: **Aarav Sharma** (`STU001`), CGPA: **9.42**.
   - Enrolled Program: `PROG01` (Full Stack Java), Batch `B01`.
2. **Live Eligibility Check**:
   - Show how drives with `Min_CGPA <= 9.42` (e.g. Microsoft IDC, Amazon India) display an emerald **"Apply Now"** button.
   - Show how a high-cutoff drive dynamically informs ineligible students.
3. **1-Click Apply (Executes PL/SQL `APPLY_FOR_DRIVE`)**:
   - Click **"Apply Now (PL/SQL)"** on an open drive (e.g., Oracle IDC or Amazon India).
   - Point out the instant green transaction banner:
     > *"Successfully applied via PL/SQL procedure APPLY_FOR_DRIVE! Application ID: APP007"*
   - Show the **My Applications Tracker** table updating in real time.

### Step 3: Recruiter Experience & Interview Operations
1. Use the **top-right Persona Switcher** to switch to **Recruiter (Microsoft - C001)**.
2. In the **Candidate Application Pipeline** table:
   - Locate Aarav Sharma or another applicant.
   - Click **Shortlist** (state transitions to `SHORTLISTED`).
3. **Schedule Interview (Executes PL/SQL `SCHEDULE_INTERVIEW`)**:
   - Click **Schedule Interview**.
   - Show the **Pinned Interviewer Dropdown**: note that `Dr. Rajesh Sharma` is permanently mapped to Round 1 (OA) per the DA1 `INTERVIEWER_ROUND` rule!
   - Select Delivery Mode (**Online** / **Offline**).
   - Click **Confirm & Schedule**.
4. **Record Evaluation Scores**:
   - Click **Record Scores** on the candidate.
   - Enter OA score (95), GD score (90), HR score (92), Outcome: **PASSED**.
   - Click **Save Evaluation**.

### Step 4: Offer Issuance & Trigger Verification (Crucial Viva Moment!)
1. In the recruiter pipeline, click **Issue Offer**.
2. Enter Annual CTC: **₹ 44.0 LPA**.
3. Click **Confirm & Issue Offer (PL/SQL)**.
4. **Explain what just occurred in the database**:
   - The stored procedure `ISSUE_OFFER` inserted a row into `OFFER_LETTER`.
   - The database trigger `TRG_OFFER_APPLICATION_STATUS` immediately fired in response to `AFTER INSERT ON OFFER_LETTER`.
   - The trigger automatically updated `APPLICATION.Status` to `SELECTED`!
   - The audit trigger `TRG_APPLICATION_AUDIT` recorded the transition into the `APPLICATION_AUDIT` table!
5. Show the recruiter candidate row now displaying the green badge: **"SELECTED (Offer Issued)"**.

### Step 5: Student Formal Offer Letter
1. Switch persona back to **Student (STU001)**.
2. Scroll to the **Official Employment Offer Letters Vault**.
3. View the new offer from Microsoft IDC at **44.0 LPA**.
4. Click **View Formal Offer Letter** to open the printable modal.

### Step 6: Admin Control Panel, Live Audit Trail & DQL Demonstrator
1. Switch persona to **Admin & DBA**.
2. **Review Executive KPIs**:
   - Placement Rate (calculated by PL/SQL `GET_PLACEMENT_RATE`).
   - Average CTC (calculated by PL/SQL `GET_AVERAGE_PACKAGE`).
   - Visual charts: Salary Tier Distribution and Application Funnel.
3. Click the **Trigger Audit Trail** tab:
   - Point out the exact audit row generated during Step 4:
     - Old Status: `INTERVIEWING` ➔ New Status: `SELECTED`
     - Trigger Source: `TRIGGER: TRG_OFFER_APPLICATION_STATUS`
4. Click the **DA2 DQL Runner** tab:
   - Demonstrate the three required complex query types:
     1. **5-Table INNER JOIN**: `STUDENT` ➔ `APPLICATION` ➔ `PLACEMENT_DRIVE` ➔ `JOB_COMPANY` ➔ `COMPANY`.
     2. **GROUP BY with HAVING**: Filters drives with high candidate volume.
     3. **Analytic DENSE_RANK()**: Ranks candidates by CTC package without gaps.

---

## Part 3: Viva Q&A Cheat Sheet

- **Q: Why is `BATCH` primary key composite?**
  *A: `Batch_No` is scoped to each `Program_Id`. A batch numbered `B01` under Full Stack Java is distinct from `B01` under Cloud DevOps. Making `(Program_Id, Batch_No)` the primary key prevents inter-program collisions while preserving BCNF.*
- **Q: Why can't an interviewer belong to multiple rounds?**
  *A: In DA1, the functional dependency `Interviewer_Name → Interview_Round_No` was extracted and proven. `INTERVIEWER_ROUND` has `Interviewer_Name` as its single-column primary key, pinning each interviewer permanently to one round.*
- **Q: How does the application prevent duplicate applications?**
  *A: The PL/SQL stored procedure `APPLY_FOR_DRIVE` queries `STUDENT_DAILY_ROUTINE_DRIVE` and `APPLICATION` before inserting; if a record exists for that `(Student_Id, Drive_Id)` pair, it raises user-defined exception `ORA-20003`.*
- **Q: How does the offer trigger work?**
  *A: `TRG_OFFER_APPLICATION_STATUS` is an `AFTER INSERT ON OFFER_LETTER FOR EACH ROW` trigger. It executes `UPDATE APPLICATION SET Status = 'SELECTED' WHERE Application_Id = :NEW.Application_Id`, ensuring data integrity without depending on client-side code.*
