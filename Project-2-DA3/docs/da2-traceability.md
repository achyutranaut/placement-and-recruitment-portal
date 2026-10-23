# DA2 Academic Requirement Traceability Matrix

**Project**: Student Placement and Recruitment Portal  
**Academic Requirement**: "Implement the designed database using SQL and PL/SQL, followed by report submission and project demonstration."  

---

## 1. Traceability Matrix

| Requirement Area | Detailed Feature | Source Code / Artifact | Oracle Object / Mechanism | Demonstration Method |
|---|---|---|---|---|
| **DDL** | `CREATE TABLE` | `database/migrations/V1__create_schema.sql` | 19 Relational Tables | Inspect Flyway schema creation, execute `DESC STUDENT`, `DESC PLACEMENT_DRIVE` |
| **DDL** | `ALTER TABLE` / Constraints | `database/migrations/V2__constraints.sql` | Primary Keys, Foreign Keys, `CHECK` constraints | Test constraint violation: insert invalid level flag or negative fee |
| **DDL** | Indexes | `database/migrations/V3__indexes.sql` | B-tree performance indexes | `EXPLAIN PLAN FOR SELECT * FROM APPLICATION WHERE STATUS = 'APPLIED'` |
| **DDL** | Views | `database/migrations/V4__views.sql` | `V_STUDENT_APPLICATIONS`, `V_OFFER_SUMMARY`, `V_DRIVE_ELIGIBLE_STUDENTS` | `SELECT * FROM V_OFFER_SUMMARY WHERE ROWNUM <= 10;` |
| **DML** | `INSERT` | `database/migrations/V5__sample_data.sql`, Spring Services | Student, Company, Job, Drive, Application rows | New registration via UI, SQL verify row count |
| **DML** | `UPDATE` | StudentService, ApplicationService | Application status update, Profile edit | Recruiter marks applicant status to `SHORTLISTED` |
| **DML** | `DELETE` | PlacementDriveService, BatchService | Soft/Hard deletion of jobs or batches | Admin deletes an unreferenced test batch |
| **DQL** | Joins (Inner, Outer) | `database/migrations/V10__demo_queries.sql`, `ReportRepository` | Multi-table joins (Student + Program + Batch + Application) | Query `V_STUDENT_APPLICATIONS` joining 6 tables |
| **DQL** | Aggregation (`GROUP BY`, `HAVING`) | `database/migrations/V10__demo_queries.sql`, `ReportService` | Count of offers per company, avg CTC > 10 LPA | View Admin Reports chart: Company-wise average CTC |
| **DQL** | Subqueries & Correlated Subqueries | `database/migrations/V10__demo_queries.sql` | Correlated subquery finding students with max marks in batch | Execute query 5 in `V10__demo_queries.sql` |
| **DCL** | Roles, `GRANT`, `REVOKE` | `database/dcl/roles_and_grants.sql` | Roles: `PORTAL_STUDENT`, `PORTAL_RECRUITER`, `PORTAL_ADMIN` | Connect as `PORTAL_STUDENT`, attempt unauthorized table write (denied) |
| **TCL** | `COMMIT`, `SAVEPOINT`, `ROLLBACK`, `ROLLBACK TO SAVEPOINT` | `database/tcl/transaction_scenarios.sql`, `V6__procedures.sql` | Stored procedures with nested transaction control | Run `APPLY_FOR_DRIVE` with induced failure; observe rollback to savepoint |
| **PL/SQL** | Stored Procedures | `database/migrations/V6__procedures.sql`, `*JdbcDao.java` | `REGISTER_STUDENT_PROGRAM`, `APPLY_FOR_DRIVE`, `SCHEDULE_INTERVIEW`, `ISSUE_OFFER` | Invoke procedure from Spring JDBC via frontend button click |
| **PL/SQL** | Stored Functions | `database/migrations/V7__functions.sql`, `ReportJdbcDao.java` | `GET_STUDENT_APPLICATION_COUNT`, `GET_PLACEMENT_RATE`, `GET_AVERAGE_PACKAGE` | Query function result in SQL Developer and observe on Dashboard card |
| **PL/SQL** | Explicit Cursors | `database/migrations/V8__cursors.sql` | Cursor with `%FOUND`, `%NOTFOUND`, `%ROWCOUNT`, Cursor FOR loop | Run `PKG_PLACEMENT_REPORTS.GENERATE_DRIVE_SUMMARY` |
| **PL/SQL** | Exception Handling | `V6__procedures.sql`, `GlobalExceptionHandler.java` | Custom user-defined exceptions: `EX_ALREADY_REGISTERED`, `EX_INELIGIBLE_CGPA` | Attempt applying with CGPA < Min_CGPA; observe 400 Bad Request with custom message |
| **PL/SQL** | Database Triggers | `database/migrations/V9__triggers.sql` | `TRG_OFFER_APPLICATION_STATUS`, `TRG_APPLICATION_AUDIT`, `TRG_ENFORCE_PROGRAM_LEVEL` | Insert offer; verify application status updates to `SELECTED` and audit row written |

---

## 2. Live Viva Demonstration Script (10-Minute Walkthrough)

1. **Schema & Migration Verification (2 mins)**:
   - Run `mvn flyway:info` or inspect Flyway history table `FLYWAY_SCHEMA_HISTORY`.
   - Show table definitions (`DESC STUDENT`, `DESC APPLICATION`, `DESC OFFER_LETTER`).
2. **PL/SQL Procedure & Transaction Verification (3 mins)**:
   - Open terminal / SQL worksheet.
   - Execute `APPLY_FOR_DRIVE` procedure with student ID and drive ID.
   - Demonstrate exception handling and `ROLLBACK TO SAVEPOINT` when daily limit is exceeded.
3. **Trigger & Lifecycle Automation (2 mins)**:
   - Recruiter creates an offer via UI or SQL.
   - Show that `APPLICATION.STATUS` immediately turned to `SELECTED` via `TRG_OFFER_APPLICATION_STATUS`.
   - Query `APPLICATION_AUDIT` showing the automated history entry.
4. **Full-Stack End-to-End Application (3 mins)**:
   - Log in as Student (`student1`), browse active placement drives, apply to a drive.
   - Log in as Recruiter (`recruiter1`), review applicant, schedule interview round.
   - Log in as Admin (`admin`), show live Recharts dashboards, placement percentages, and company salary distributions.
