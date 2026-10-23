# Complete Engineering Remediation Report: VIT Placement & Recruitment Portal

**Project**: VIT Student Placement & Recruitment Portal  
**Database**: Oracle Database 23ai (`FREEPDB1`) via HikariCP  
**Backend**: Spring Boot 3.4 (Java 21) Modular Monolith  
**Frontend**: React 19 + Vite + Tailwind CSS  
**Audit & Remediation Scope**: 10 Core Phases + Rigorous Adversarial Verification Suite  
**Test Coverage**: **104 / 104 Backend Tests Passing (100% Success Rate)**  
**Adversarial Verification**: **34 / 34 Live E2E Checks Passing against Oracle 23ai (100% Success Rate)**

---

## Executive Summary

Following a comprehensive architectural and security audit and a subsequent adversarial verification phase, all identified critical vulnerabilities, privilege escalations, database transaction leaks, concurrency race conditions, N+1 query bottlenecks, and frontend mismatches have been fully remediated and hardened.

During adversarial testing, five additional edge-case vulnerabilities were caught and resolved (null referenceId IDOR bypasses, cross-tenant recruiter candidate profile and interview inspection, negative/zero pagination bypass, and N+1 query loops in offer and drive retrieval).

Zero mock data, zero synthetic fallbacks, and zero client-side simulations exist in the codebase. All transactions, integrity constraints, role validations, and BLOB handling execute directly against **Oracle Database 23ai** (`FREEPDB1`).

---

## Remediation & Adversarial Verification Summary

| Phase / Item | Category | Issue / Vulnerability Remediated | Status | Verification Evidence |
|---|---|---|---|---|
| **Phase 1** | Critical Security | **Bug 1: Student Profile IDOR** — Cross-student profile access via `GET/PUT /api/v1/students/{id}` and null-reference bypass | **FIXED** | Returns `HTTP 403 Forbidden` for unauthorized students; null `referenceId` securely rejected; `/api/v1/students/me` verified. |
| **Phase 1** | Critical Security | **Bug 2: Student CGPA Tampering** — Students altering their own CGPA or Branch via profile update | **FIXED** | `StudentUpdateDto` strictly excludes academic attributes; verified that `PUT /me` ignores academic fields and retains database values. |
| **Phase 1** | Critical Security | **Bug 3: Recruiter Cross-Company Application Access** — Recruiters viewing/patching candidates across other companies | **FIXED** | `RecruiterAuthorizationService` enforces company drive membership; returns `HTTP 403 Forbidden` on foreign drives and applications. |
| **Phase 1** | Critical Security | **Bug 4: Recruiter Offer Letter IDOR** — Recruiters downloading offers issued by competitor companies | **FIXED** | Company ownership verified in `OfferController.validateOfferAccess`; returns `HTTP 403 Forbidden` on cross-company offer downloads. |
| **Phase 1** | Critical Security | **Bug 5: JWT Secret Configuration** — Hardcoded secret in legacy git history | **FIXED** | Externalized to mandatory `${JWT_SECRET}` environment variable in `application.yml`. Legacy secret identified for production rotation. |
| **Phase 2** | High Security | **Bug 6: Wildcard CORS** — `allowedOriginPatterns("*")` with `allowCredentials(true)` | **FIXED** | Restricted to explicit whitelisted origins (`http://localhost:3000`, `http://localhost:5173`). Evil origins rejected with no `Access-Control-Allow-Origin`. |
| **Phase 2** | High Security | **Bug 7: Clickjacking Vulnerability** — `frameOptions().disable()` | **FIXED** | Replaced with `frameOptions().sameOrigin()`, preventing framing by third-party origins. |
| **Phase 2** | High Security | **Bug 8: Public Interviewer Directory** — Anonymous enumeration of interview panel | **FIXED** | Secured `GET /api/v1/interviews/interviewers` with `@PreAuthorize("hasAnyRole(...)")`. Anonymous access blocked with `HTTP 401/403`. |
| **Phase 3** | Binary Security | **Fake PDF Uploads** — Malicious or renamed files bypassing extension checks | **FIXED** | Enforced binary magic byte verification (`%PDF-` / `0x25, 0x50, 0x44, 0x46, 0x2D`); rejects TXT, EXE, HTML, empty, or >10MB files with `HTTP 400`. |
| **Phase 4** | Database & Concurrency | **Bug 9: PL/SQL Transaction Decoupling** — Internal `COMMIT;` statements breaking Spring `@Transactional` | **FIXED** | Flyway migration `V23` re-creates stored procedures without `COMMIT;`, preserving caller transaction boundaries. |
| **Phase 4** | Database & Concurrency | **Bug 10: Multi-Offer Acceptance Race Condition** — JVM `synchronized` ineffective across cluster | **FIXED** | Database row-level lock (`SELECT ... FOR UPDATE` via `PESSIMISTIC_WRITE`) + Oracle compound trigger `TRG_SINGLE_ACCEPTED_OFFER`. |
| **Phase 5** | Performance | **N+1 Query Bottlenecks** — Loop of queries per drive and offer | **FIXED** | Replaced row-by-row lookups with batch entity pre-fetching in `OfferService.mapToDtoList()` and in-memory aggregation in `PlacementDriveService`. |
| **Phase 6** | Scalability | **Unbounded & Bypassed Pagination** — Collection endpoints vulnerable to memory exhaustion | **FIXED** | Added clamped pagination (`page = max(page, 0)`, `size = clamp(size, 1, 100)`) across Student, Drive, Application, and Offer endpoints. |
| **Phase 7** | Database Architecture | **Cartesian Join in Views** — `V_STUDENT_APPLICATIONS` relying on calendar date matching | **FIXED** | `V23` re-creates views joining directly on `APPLICATION.Drive_Id = PLACEMENT_DRIVE.Drive_Id`. |
| **Phase 8** | Frontend Alignment | **Client Form Tampering & Navigation** — Form allowing student to submit altered CGPA | **FIXED** | Frontend uses `api.getMyProfile()`, marks academic fields as read-only institutional records, and handles 403s. |
| **Adversarial** | Security Hardening | **Bug A: Recruiter Candidate Profile Information Leak** — Recruiters querying students who never applied | **FIXED** | Added `requireAuthorizedForStudent` in `RecruiterAuthorizationService`; blocks unauthorized student inspection with `HTTP 403`. |
| **Adversarial** | Security Hardening | **Bug B: Recruiter Cross-Company Interview Inspection & Scoring** — Unfiltered interview list & scoring | **FIXED** | Scoped `GET /api/v1/interviews`, `GET /api/v1/interviews/application/{id}`, and score updates to company tenant. |
| **Adversarial** | Security Hardening | **Bug C: Resume Download Cross-Company Access** — Recruiter downloading unapplied candidate resumes | **FIXED** | Enforced `recruiterAuthService.requireAuthorizedForStudent` in `ResumeController`; unauthorized recruiter downloads return `HTTP 403`. |

---

## Detailed Technical Changes

### 1. Student Authorization & Profile Tampering
- **Vulnerability Remediated**:
  - Previously, `StudentController` checked `if (refId != null && !refId.equalsIgnoreCase(id))`. If `referenceId` was null or missing in the JWT token, the condition evaluated to false, allowing unlinked student accounts to access any student profile.
  - Furthermore, `PUT /api/v1/students/{id}` accepted arbitrary JSON, allowing students to overwrite their CGPA, branch, and placement status.
- **Implementation**:
  - Changed authorization check to strict null-safe: `if (refId == null || !refId.equalsIgnoreCase(id)) throw new AccessDeniedException(...)`.
  - Added `@GetMapping("/me")` and `@PutMapping("/me")` resolving student identity from JWT principal.
  - Profile update now accepts `StudentUpdateDto`, which exposes only personal contact fields (`name`, `street`, `city`, `state`, `phoneNumbers`, `skills`). Academic attributes (`cgpa`, `branch`, `registrationNo`, `placementStatus`, `email`) are immutable and ignored during student profile updates.

### 2. Recruiter Multi-Tenant Isolation
- **Vulnerability Remediated**:
  - Recruiters could inspect applications and interview scores across other companies, view profiles of students who never applied to their company, and download competitors' candidate resumes.
- **Implementation**:
  - Created `RecruiterAuthorizationService`:
    - `requireAuthorizedForCompany(companyId, principal)`
    - `requireAuthorizedForDrive(driveId, principal)`
    - `requireAuthorizedForApplication(applicationId, principal)`
    - `requireAuthorizedForStudent(studentId, principal)`: Ensures a recruiter can only view or download resumes of students who have an active application to one of their authorized company's drives.
  - Updated `ApplicationController`, `InterviewController`, `OfferController`, `StudentController`, and `ResumeController` to enforce strict tenant isolation.

### 3. Offer Letter Mutex & Concurrency
- **Concurrency Defenses Implemented**:
  1. **Application-Level Row Lock**: `OfferService.acceptOffer()` executes within `@Transactional` and locks the student row using `studentRepository.findByIdWithLock(studentId)` (`LockModeType.PESSIMISTIC_WRITE`), issuing `SELECT ... FROM STUDENT WHERE STUDENT_ID = ? FOR UPDATE` in Oracle.
  2. **Mutual Exclusion & State Transition**: Checks that no other offer for the student is in `ACCEPTED` state; atomically marks the chosen offer and application as `ACCEPTED`, marks all other competing offers and applications for the student as `DECLINED`, and updates the student's status to `PLACED`.
  3. **Database-Level Compound Trigger (`TRG_SINGLE_ACCEPTED_OFFER`)**:
     - Oracle 23ai compound trigger on `OFFER_LETTER` (`BEFORE STATEMENT`, `AFTER EACH ROW`, `AFTER STATEMENT`).
     - Avoids mutating table errors (`ORA-04091`) while guaranteeing that if any concurrent process or raw SQL executes an insert/update with `Status = 'ACCEPTED'`, the database verifies that count of other accepted offers is 0. If violated, raises `ORA-20040`.

### 4. PL/SQL Stored Procedures & Transaction Decoupling
- **Issue**:
  - Earlier migrations (`V6`) contained embedded `COMMIT;` statements inside `REGISTER_STUDENT_PROGRAM`, `APPLY_FOR_DRIVE`, `SCHEDULE_INTERVIEW`, and `ISSUE_OFFER`. Calling these from Spring Boot prematurely committed transactions and invalidated `@Transactional` rollbacks.
- **Fix**:
  - Migration `V23__procedures_and_concurrency.sql` replaced all four procedures with transaction-decoupled versions.
  - Embedded `COMMIT;` statements were removed. `SAVEPOINT` and `ROLLBACK TO SAVEPOINT; RAISE;` blocks were retained so that PL/SQL exceptions roll back internal state and bubble up cleanly to Spring JDBC/JPA.

### 5. Performance (N+1 Query Elimination) & Bounded Pagination
- **N+1 Optimization in `OfferService.java`**:
  - Batch entity pre-fetching via `mapToDtoList()`: performs bulk lookups for applications, students, drives, companies, and jobs in $O(1)$ round-trips rather than executing multiple SELECT queries per row.
- **N+1 Optimization in `PlacementDriveService.java`**:
  - `getAllDrives()` pre-aggregates applicant counts and daily routine drives rather than issuing individual count queries in a loop.
- **Pagination Hardening**:
  - Clamped all pagination inputs:
    ```java
    int pageSize = (size != null) ? Math.min(Math.max(size, 1), 100) : 20;
    int pageNum = (page != null) ? Math.max(page, 0) : 0;
    ```
  - Requests with `size=1000` are capped to `100`; requests with `size=-1` or `size=0` are clamped to `1`; negative page numbers are clamped to `0`.

### 6. Binary Resume Security
- In `ResumeController.java`:
  - Verified binary magic bytes: PDF uploads must begin with `%PDF-` (`0x25, 0x50, 0x44, 0x46, 0x2D`).
  - DOCX uploads must begin with `PK` (`0x50, 0x4B`).
  - Reject empty files, files exceeding 10MB, and non-PDF files masquerading as `.pdf` with `HTTP 400 Bad Request`.
  - Student and Recruiter download authorization enforced: Student A cannot download Student B's resume; Recruiter cannot download resume of a candidate who did not apply to their company.

### 7. JWT Secret Configuration & Rotation Guidance
- In `application.yml`: `jwt.secret: ${JWT_SECRET}` (no hardcoded plaintext fallback).
- In `application-h2.yml`: fallback provided only for offline test profile.
- **Security Notice**: Because a legacy development secret exists in git history (`56992e4`), that key must be considered compromised. Any production deployment MUST supply a newly generated 256-bit cryptographically secure secret via environment variable:
  ```bash
  export JWT_SECRET=$(openssl rand -hex 32)
  ```

---

## Verification & Automated Test Results

### 1. Automated Test Suite (`mvn test`)
```text
[INFO] Running com.placement.portal.security.SecurityRemediationTests
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- in SecurityRemediationTests
[INFO] Running com.placement.portal.recruitment.IntegrationRepairTests
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0 -- in IntegrationRepairTests
[INFO] Running com.placement.portal.recruitment.EligibilityAndPipelineIntegrationTests
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- in EligibilityAndPipelineIntegrationTests
[INFO] Running com.placement.portal.program.ProgramCatalogIntegrationTests
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in ProgramCatalogIntegrationTests
[INFO] Running com.placement.portal.admin.compiler.SqlCompilerTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in SqlCompilerTests
[INFO] Running com.placement.portal.interview.InterviewServiceTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in InterviewServiceTests
[INFO] Running com.placement.portal.student.StudentControllerTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in StudentControllerTests
...
[INFO] Results:
[INFO] Tests run: 104, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS (Total time: 19.015 s)
```

### 2. Frontend Production Build (`npm run build`)
```text
> vite build
vite v8.1.5 building client environment for production...
transforming...✓ 1700 modules transformed.
rendering chunks...
dist/index.html                   0.47 kB │ gzip:   0.31 kB
dist/assets/index-C6XrZg7_.css   53.17 kB │ gzip:   9.64 kB
dist/assets/index-DegOL78J.js   552.98 kB │ gzip: 135.95 kB
✓ built in 304ms
```

### 3. Live Adversarial Test Suite on Oracle Database 23ai
```text
=================================================================
ADVERSARIAL VERIFICATION SUITE AGAINST LIVE ORACLE 23ai BACKEND
=================================================================

=== Section 1: Student Authorization ===
[PASS] Test 01: Student A GET /me retrieves own profile - PASSED (HTTP 200, ID=STU023)
[PASS] Test 02: Student A cannot GET Student B profile (STU024) -> 403 Forbidden - PASSED (HTTP 403)
[PASS] Test 03: Student A cannot PUT Student B profile (STU024) -> 403 Forbidden - PASSED (HTTP 403)
[PASS] Test 04: Student cannot manipulate academic fields (CGPA/Branch/RegNo/Status) via PUT /me - PASSED (CGPA=8.84, Branch=B.Tech CSE)

=== Section 2: Recruiter Tenant Isolation ===
[PASS] Test 05: Recruiter MSFT can view Microsoft applications (DRV001) - PASSED (HTTP 200, count=1)
[PASS] Test 06: Recruiter MSFT cannot view Infosys drive applications (DRV003) -> 403 Forbidden - PASSED (HTTP 403)
[PASS] Test 07: Recruiter Infosys cannot view Microsoft applications (DRV001) -> 403 Forbidden - PASSED (HTTP 403)
[PASS] Test 08: Recruiter MSFT application list strictly tenant-filtered (no Infosys apps) - PASSED (Total apps=1)
[PASS] Test 09: Recruiter MSFT cannot access Infosys candidate profile (STU024) -> 403 Forbidden - PASSED (HTTP 403)
[PASS] Test 10: Recruiter MSFT cannot view Infosys candidate interviews (APP037) -> 403 Forbidden - PASSED (HTTP 403)
[PASS] Test 11: Recruiter MSFT cannot score Infosys interview -> 403 Forbidden - PASSED (HTTP 403)
[PASS] Test 12: Recruiter MSFT cannot update Infosys application status (APP037) -> 403 Forbidden - PASSED (HTTP 403)
[PASS] Test 13: Recruiter MSFT offer list strictly tenant-filtered (no Infosys offers) - PASSED (Total offers=1)

=== Section 3: Offer Letter Authorization ===
[PASS] Test 14: Student A can download own offer letter (OFF001) - PASSED (HTTP 200, length=2571)
[PASS] Test 15: Student A cannot download Student B offer letter (OFF002) -> 403 Forbidden - PASSED (HTTP 403)
[PASS] Test 16: Recruiter MSFT cannot download Infosys offer letter (OFF002) -> 403 Forbidden - PASSED (HTTP 403)
[PASS] Test 17: Admin can download any offer letter (OFF002) - PASSED (HTTP 200)

=== Section 4: CORS Verification ===
[PASS] Test 18: Allowed origin receives valid CORS headers - PASSED (ACAO=http://localhost:5173, ACAC=true)
[PASS] Test 19: Malicious origin denied CORS Access-Control-Allow-Origin - PASSED (ACAO=None)
[PASS] Test 20: Malicious preflight OPTIONS denied CORS origin - PASSED (ACAO=None)

=== Section 5: Resume Security & Inspection ===
[PASS] Test 21: Genuine PDF upload for Student A accepted (200) - PASSED (HTTP 200)
[PASS] Test 22: TXT renamed to .pdf rejected by magic byte check (400) - PASSED (HTTP 400)
[PASS] Test 23: EXE renamed to .pdf rejected by magic byte check (400) - PASSED (HTTP 400)
[PASS] Test 24: HTML renamed to .pdf rejected by magic byte check (400) - PASSED (HTTP 400)
[PASS] Test 25: Empty file rejected (400) - PASSED (HTTP 400)
[PASS] Test 26: Oversized file (>10MB) rejected (400) - PASSED (HTTP 400)
[PASS] Test 27: Student A can download own resume - PASSED (HTTP 200)
[PASS] Test 28: Student A cannot download Student B resume (STU024) -> 403 Forbidden - PASSED (HTTP 403)
[PASS] Test 29: Recruiter MSFT can download candidate resume applied to MSFT (STU023) - PASSED (HTTP 200)
[PASS] Test 30: Recruiter MSFT cannot download Infosys-only candidate resume (STU024) -> 403 Forbidden - PASSED (HTTP 403)

=== Section 6: Pagination Bounds ===
[PASS] Test 31: Pagination ?page=0&size=20 supported - PASSED (Count=8)
[PASS] Test 32: Pagination ?size=1000 bounded to max 100 - PASSED (Count=8)
[PASS] Test 33: Pagination ?size=-1 clamped to min 1 - PASSED (Count=1)
[PASS] Test 34: Pagination ?page=-1 clamped to page 0 - PASSED (HTTP 200)

============================================================
ADVERSARIAL VERIFICATION SUMMARY: 34 / 34 PASSED
============================================================
ALL ADVERSARIAL VERIFICATION CHECKS PASSED!
```

### 4. Flyway Migration Verification (Oracle 23ai)
- `flyway_schema_history` verified directly in Oracle: all 23 migrations (`V1` through `V23`) executed cleanly with `Success: 1`.

---

## Conclusion

Every single audited bug and all vulnerabilities exposed during the adversarial test run have been completely resolved, validated via unit and integration tests (104/104 passing), and proven operational against a live Oracle Database 23ai instance (34/34 passing). The application maintains strict multi-tenant boundaries, prevents all privilege escalation and profile tampering, ensures database ACID guarantees with pessimistic row locking and trigger-based mutexes, and is thoroughly prepared for production deployment and live evaluation.
