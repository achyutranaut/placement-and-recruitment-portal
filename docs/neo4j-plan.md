# Neo4j Graph Database Implementation Plan

## Executive Summary
This document defines the comprehensive architecture and implementation plan for extending the VIT Student Placement and Recruitment Portal with **Neo4j 5.x** as an alternative, modern graph database backend under the Spring profile `neo4j`. The portal maintains full dual-backend capability: the existing Oracle 23ai implementation (`oracle` profile) and the Neo4j implementation (`neo4j` profile) serve the exact same React frontend and adhere to identical REST API contracts and business invariants.

---

## 1. Database Touchpoints Analysis

Every relational schema entity, DAO, and query in the existing system is mapped to its Neo4j graph equivalent:

### 1.1 Relational Schema & Migration Artifacts
* **Tables (27 base tables & junctions)**:
  - `STUDENT`, `STUDENT_PHONE`, `STUDENT_SKILL`
  - `PROGRAM`, `REGISTERS`, `BATCH`, `BATCH_PROGRAM`, `ATTENDS`, `BATCH_TITLE_PROGRAM`, `ASSESSMENT`
  - `COMPANY`, `EMAIL_COMPANY`, `COMPANY_PHONE`, `JOB_COMPANY`, `RECRUITER_COMPANY`
  - `PLACEMENT_DRIVE`, `STUDENT_DAILY_ROUTINE_DRIVE`, `DRIVE_ELIGIBILITY`
  - `APPLICATION`, `INTERVIEWER_ROUND`, `INTERVIEW`
  - `OFFER_LETTER`, `APPLICATION_AUDIT`, `PORTAL_USER`, `SQL_EXECUTION_AUDIT`
  - `STUDENT_RESUME`, `RESUME_EVALUATION`
* **PL/SQL Stored Procedures (V6 / V23 / V27)**:
  1. `REGISTER_STUDENT_PROGRAM`: Capacity, idempotency, student-to-program/batch linkage.
  2. `APPLY_FOR_DRIVE`: CGPA check, program eligibility traversal, calendar-day rate limit (max 1 application per student per day).
  3. `SCHEDULE_INTERVIEW`: Multi-round validation, interviewer round binding, status transition to `INTERVIEWING`.
  4. `ISSUE_OFFER`: Readiness validation, interview clearance checking, transition to `OFFERED`.
* **PL/SQL Functions & REF CURSORs (V7 / V27)**:
  1. `GET_STUDENT_APPLICATION_COUNT`
  2. `GET_PLACEMENT_RATE`
  3. `GET_AVERAGE_PACKAGE`
  4. `GET_ELIGIBLE_STUDENTS`
  5. `GET_APPLICATION_READINESS`
* **Triggers & State Enforcements (V9 / V15 / V23 / V27)**:
  1. Offer creation automatically transitions application status to `OFFERED`.
  2. Status updates create append-only audit trail in `APPLICATION_AUDIT`.
  3. Offer acceptance atomically accepts chosen offer, declines all other competing offers for the student, and sets `Student.placementStatus = 'PLACED'`.
  4. Single accepted offer mutex enforcement.
* **Views (V4 / V23)**:
  - `V_STUDENT_APPLICATIONS`, `V_PLACEMENT_DRIVES_OVERVIEW`, `V_OFFER_SUMMARY`, `V_STUDENT_TRAINING_PROFILE`, `V_INTERVIEW_SCHEDULE`.

---

## 2. Proposed Neo4j Graph Data Model

### 2.1 Graph Nodes (Labels & Key Properties)
1. `(:Student)`: `studentId` (PK), `name`, `email`, `cgpa`, `street`, `city`, `state`, `dob`, `registrationNo`, `branch`, `placementStatus` ('NOT_PLACED' | 'OFFERED' | 'PLACED'), `phoneNumbers` (list of strings), `skills` (list of strings).
2. `(:Program)`: `programId` (PK), `programName`, `fee`, `duration`, `degreeLevel`, `department`, `specialization`, `regNoRegex`, `regNoSample`, `isOffered`.
3. `(:Batch)`: `batchId` (composite key: `programId + '_' + batchNo`), `batchNo`, `programId`, `schedule`, `room`, `startDate`, `endDate`.
4. `(:Assessment)`: `assessmentId` (PK), `title`, `maxMarks`, `eligibilityCriteria`, `level`.
5. `(:Company)`: `companyId` (PK), `companyName`, `email`, `industry`, `phoneNumbers` (list of strings).
6. `(:JobPosting)`: `jobTitle` (PK), `companyId`.
7. `(:Drive)`: `driveId` (PK), `jobTitle`, `companyId`, `companyName`, `minCgpa`, `driveDate`, `applicationDeadline`, `openings`, `ctc`, `jobDescription`, `location`, `eligibleBranches`, `selectionProcess`.
8. `(:Application)`: `applicationId` (PK), `studentId`, `driveId`, `applyDate`, `status` ('APPLIED' | 'SHORTLISTED' | 'INTERVIEWING' | 'SELECTED' | 'OFFERED' | 'ACCEPTED' | 'DECLINED' | 'REJECTED').
9. `(:Interviewer)`: `interviewerName` (PK), `interviewRoundNo` (permanent pinned round).
10. `(:Interview)`: `interviewId` (PK: `appId + '_' + interviewerName`), `applicationId`, `interviewerName`, `roundNo`, `oa`, `gd`, `hr`, `result` ('PENDING' | 'CLEARED' | 'PASSED' | 'FAILED' | 'REJECTED'), `online`, `offline`.
11. `(:Offer)`: `offerId` (PK), `applicationId`, `ctcLpa`, `offerDate`, `status` ('OFFERED' | 'ACCEPTED' | 'DECLINED' | 'WITHDRAWN'), `acceptedAt`.
12. `(:Resume)`: `resumeId` (PK), `studentId`, `fileName`, `contentType`, `fileSize`, `filePath`, `sha256Hash`, `versionNo`, `isCurrent`, `uploadedAt`.
13. `(:ResumeEvaluation)`: `evaluationId` (PK), `resumeId`, `applicationId`, `recruiterId`, `technicalScore`, `educationScore`, `projectScore`, `experienceScore`, `overallScore`, `comments`, `evaluatedAt`.
14. `(:AuditEvent)`: `auditId` (PK), `applicationId`, `oldStatus`, `newStatus`, `changedAt`, `changedBy`.
15. `(:PortalUser)`: `userId` (PK), `username` (UNIQUE), `passwordHash`, `role`, `referenceId`, `fullName`, `email`, `createdAt`.

### 2.2 Graph Relationships
- `(Student)-[:ENROLLED_IN { regDate }]->(Program)`
- `(Student)-[:ATTENDS]->(Batch)-[:OF_PROGRAM]->(Program)`
- `(Batch)-[:HAS_ASSESSMENT]->(Assessment)`
- `(Company)-[:OFFERS_JOB]->(JobPosting)`
- `(Company)-[:HOSTS_DRIVE]->(Drive)`
- `(Drive)-[:FOR_JOB]->(JobPosting)`
- `(Drive)-[:ELIGIBLE_FOR]->(Program)`
- `(Student)-[:SUBMITTED_APPLICATION]->(Application)-[:FOR_DRIVE]->(Drive)`
- `(Student)-[:APPLIED_ON_DATE { date }]->(Drive)`  *(for instant daily routine 1-per-day collision enforcement)*
- `(Application)-[:HAS_INTERVIEW]->(Interview)-[:CONDUCTED_BY]->(Interviewer)`
- `(Application)-[:RECEIVED_OFFER]->(Offer)`
- `(Student)-[:HAS_RESUME]->(Resume)`
- `(Resume)-[:HAS_EVALUATION]->(ResumeEvaluation)-[:FOR_APPLICATION]->(Application)`
- `(Application)-[:HAS_AUDIT]->(AuditEvent)`
- `(PortalUser)-[:AUTHORIZED_FOR]->(Company)`

### 2.3 Cypher Constraints & Indexes
Defined in `database/neo4j/schema.cypher`:
- Uniqueness: `Student(studentId)`, `Program(programId)`, `Company(companyId)`, `Drive(driveId)`, `Application(applicationId)`, `Interviewer(interviewerName)`, `Offer(offerId)`, `Resume(resumeId)`, `PortalUser(username)`.
- Existence & Indexes: Composite keys, status indexes, CGPA property indexes, drive deadline indexes.

---

## 3. Package Structure & Persistence Abstraction

```
com.placement.portal
├── neo4j/
│   ├── config/
│   │   ├── Neo4jConfig.java               # Configures Spring Data Neo4j under @Profile("neo4j")
│   │   ├── Neo4jTransactionConfig.java    # Neo4jTransactionManager configuration
│   │   └── ResumeStorageProperties.java   # Local filesystem storage configuration
│   ├── model/                             # @Node and @RelationshipProperties entities
│   │   ├── StudentNode.java
│   │   ├── ProgramNode.java
│   │   ├── BatchNode.java
│   │   ├── AssessmentNode.java
│   │   ├── CompanyNode.java
│   │   ├── DriveNode.java
│   │   ├── ApplicationNode.java
│   │   ├── InterviewNode.java
│   │   ├── InterviewerNode.java
│   │   ├── OfferNode.java
│   │   ├── ResumeNode.java
│   │   ├── ResumeEvaluationNode.java
│   │   ├── AuditEventNode.java
│   │   └── PortalUserNode.java
│   ├── repository/                        # Spring Data Neo4j Repositories with custom Cypher queries
│   │   ├── Neo4jStudentRepository.java
│   │   ├── Neo4jProgramRepository.java
│   │   ├── Neo4jBatchRepository.java
│   │   ├── Neo4jCompanyRepository.java
│   │   ├── Neo4jDriveRepository.java
│   │   ├── Neo4jApplicationRepository.java
│   │   ├── Neo4jInterviewRepository.java
│   │   ├── Neo4jOfferRepository.java
│   │   ├── Neo4jResumeRepository.java
│   │   ├── Neo4jAuditEventRepository.java
│   │   └── Neo4jUserRepository.java
│   ├── service/                           # Domain service implementations (@Profile("neo4j"))
│   │   ├── Neo4jStudentService.java
│   │   ├── Neo4jProgramService.java
│   │   ├── Neo4jCompanyService.java
│   │   ├── Neo4jPlacementDriveService.java
│   │   ├── Neo4jApplicationService.java
│   │   ├── Neo4jInterviewService.java
│   │   ├── Neo4jOfferService.java
│   │   ├── Neo4jResumeService.java
│   │   ├── Neo4jReportService.java
│   │   ├── Neo4jAdminService.java
│   │   └── Neo4jGraphAnalyticsService.java  # 4 Showcase graph traversal queries
│   ├── seed/
│   │   └── Neo4jDataMigrationRunner.java  # Reads Oracle/seed fixtures and loads into Neo4j
│   └── web/
│       ├── Neo4jCypherConsoleController.java # Admin Cypher console & schema explorer
│       └── Neo4jGraphViewController.java     # REST API for interactive frontend graph visualizer
```

---

## 4. Ported Logic & Transactional Invariants
1. `REGISTER_STUDENT_PROGRAM`: Implemented via declarative `@Transactional` Spring Service and Cypher match/merge logic.
2. `APPLY_FOR_DRIVE`: Atomic Cypher check for minimum CGPA, traversal `(d:Drive)-[:ELIGIBLE_FOR]->(:Program)<-[:ENROLLED_IN]-(s:Student)`, and validation of no existing `APPLIED_ON_DATE` on the same calendar day.
3. `SCHEDULE_INTERVIEW`: Validates application state, links `Interviewer`, advances application status to `INTERVIEWING`.
4. `ISSUE_OFFER`: Verifies mandatory round clearance and stage validation, generates `OfferNode`, changes status to `OFFERED`, logs audit.
5. `ACCEPT_OFFER`: Atomic acceptance of offer, declines all competing offers, sets application status to `ACCEPTED`, sets student placement status to `PLACED`.
6. Analytical Queries: `GET_PLACEMENT_RATE`, `GET_AVERAGE_PACKAGE`, and the 5 system views ported to native Cypher aggregations.
7. 4 Advanced Graph Queries:
   - Traversal-based eligibility path for candidate.
   - Complete multi-hop placement journey: `(Student)->(Application)->(Interview)->(Offer)->(Company)`.
   - Program-to-Corporate talent pipeline mapping.
   - Collaborative filtering recommendation: "Students with similar profile who got placed".

---

## 5. Implementation Phases
* **Phase 1**: Infrastructure setup (`docker-compose.neo4j.yml`, backend dependencies, `application-neo4j.yml`, `run_portal.sh`).
* **Phase 2**: Neo4j entities, schema Cypher script, constraints & indexes, and Spring Data Neo4j repositories.
* **Phase 3**: Service and persistence implementation under `@Profile("neo4j")`.
* **Phase 4**: Migration and seed runner (`neo4j-seed`) with verification report.
* **Phase 5**: Frontend Cypher Console (`/admin/sql-compiler` mode detection) and interactive placement graph visualizer.
* **Phase 6**: Testcontainers integration tests & validation.
* **Phase 7**: Comprehensive submission documentation (`docs/neo4j-implementation.md`).
