# Neo4j Graph Database Implementation & Evaluation Report

**Academic Context**: VIT Placement & Recruitment Portal  
**Course Requirement**: Modern Database Technology Extension (Non-MongoDB)  
**Database Technology**: Neo4j 5.x Graph Database Engine  
**Runtime Profiles**: Dual-Backend Capability (`oracle` for Oracle 23ai vs `neo4j` for Neo4j)

---

## 1. Why Neo4j for Campus Placement & Recruitment?

Campus placement operations are intrinsically **hyper-connected graphs**, not isolated tabular records:
- Students enroll in specific academic degree programs and attend batches.
- Corporate recruiters register drives that target specific subsets of academic programs.
- Students submit applications to placement drives, traverse sequential multi-round interview stages conducted by pinned round interviewers, receive offers, and trigger atomic mutex cascades across competing offers.
- Historical placement outcomes generate recommendation paths for subsequent cohorts based on program, skills, and academic profile.

### 1.1 Limitations of Relational Databases (Oracle 23ai) in Connected Data
In a pure relational database (Oracle 23ai), navigating deep multi-hop entity relationships requires expensive multi-table inner/outer joins (`JOIN` across 6 to 9 tables: `STUDENT` $\to$ `REGISTERS` $\to$ `PROGRAM` $\to$ `DRIVE_ELIGIBILITY` $\to$ `PLACEMENT_DRIVE` $\to$ `APPLICATION` $\to$ `INTERVIEW` $\to$ `OFFER_LETTER` $\to$ `COMPANY`).
As table cardinality grows into millions of rows:
1. Join queries experience index-lookup overhead and nested-loop / hash join degradations.
2. Recursive or variable-depth path queries (e.g. "Find all companies hiring students through rounds conducted by interviewer X" or "Find similar students placed within $\pm 1.0$ CGPA in the same branch") require complex, computationally heavy self-joins or Common Table Expressions (CTEs).
3. Schema alterations (e.g., introducing dynamic interview stages, hybrid rounds, or custom evaluation panels) require rigid DDL migrations and alter table downtime.

### 1.2 The Graph Database Advantage: Index-Free Adjacency
Neo4j utilizes **Index-Free Adjacency (IFA)**: each node maintains direct memory pointers to its adjacent relationships and neighbor nodes. Traversing relationships in Neo4j is an $O(1)$ pointer-dereference operation per hop, independent of total database size.

### 1.3 Comparison with Alternative DBMS Paradigms

| Database Paradigm | Representative Engines | Best Suited For | Suitability for Campus Placements | Key Limitations for This System |
| :--- | :--- | :--- | :--- | :--- |
| **Graph Database** *(Chosen)* | **Neo4j 5.x** | Highly connected networks, multi-hop traversals, path discovery, recommendation | **Optimal (10/10)**: Natural fit for student-drive-interview-offer-alumni graphs with zero-join traversals. | Requires graph modelling mindset; aggregations over billions of unrelated nodes slower than columnar. |
| **Relational / SQL** | Oracle 23ai, PostgreSQL | Structured transactions, strict tabular ACID, financial ledgers | **Moderate (7/10)**: Strong constraints, but multi-hop traversals degrade with deep join depth. | Combinatorial join penalties on deep paths; rigid relational normalization. |
| **Document Store** | MongoDB, Couchbase | Hierarchical documents, catalog management, rapid prototyping | **Poor (4/10)**: Disallowed by prompt; weak multi-entity cross-referencing and joins. | Embedding creates data duplication; referencing requires multiple round-trips. |
| **Column-Family / Wide-Column** | Apache Cassandra, ScyllaDB | High-throughput write streaming, time-series, append logs | **Poor (3/10)**: No joins, queries must be known before table design. | Inability to perform ad-hoc multi-hop eligibility traversals or graph walks. |
| **Search Engine** | Elasticsearch, OpenSearch | Full-text tokenization, inverted indexing, fuzzy text search | **Complementary (5/10)**: Great for resume text search, but not an authoritative transactional store. | Lack of ACID transactions, referential integrity, and graph traversals. |
| **Vector Database** | Milvus, Pinecone, Qdrant | Embedding vector similarity, semantic search | **Specialized (5/10)**: Excellent for semantic resume-job matching, but cannot manage operational portal state. | Cannot enforce relational invariants (CGPA gates, daily application limits, offer mutex). |
| **Distributed NewSQL** | CockroachDB, TiDB | Distributed horizontal scaling of relational tables | **Overkill (6/10)**: Solves horizontal scale, but retains relational join overheads. | Retains multi-join query complexity without graph traversal benefits. |

---

## 2. ER-to-Graph Mapping Architecture

### 2.1 Entity & Relationship Transformation Table

| Oracle 23ai Relational Structure | Neo4j Graph Model Element | Implementation in Neo4j |
| :--- | :--- | :--- |
| `STUDENT` | `(:Student)` Node | Node with properties (`studentId`, `name`, `email`, `cgpa`, `placementStatus`, `registrationNo`, etc.). |
| `STUDENT_PHONE`, `STUDENT_SKILL` | Node Properties | Stored directly as Neo4j native List properties: `phoneNumbers: List<String>`, `skills: List<String>`. |
| `PROGRAM` | `(:Program)` Node | Node with `programId`, `programName`, `degreeLevel`, `department`, etc. |
| `REGISTERS` (Junction table) | `[:ENROLLED_IN]` Edge | `(Student)-[:ENROLLED_IN]->(Program)` directed relationship. |
| `BATCH` + `BATCH_PROGRAM` | `(:Batch)` Node + Edge | `(:Batch)` connected via `(Batch)-[:OF_PROGRAM]->(Program)` and `(Student)-[:ATTENDS]->(Batch)`. |
| `COMPANY` + `EMAIL_COMPANY` | `(:Company)` Node | Node with properties (`companyId`, `companyName`, `industry`, `email`). |
| `PLACEMENT_DRIVE` | `(:Drive)` Node | Node with `driveId`, `jobTitle`, `minCgpa`, `applicationDeadline`, `ctc`, etc. |
| `HOSTS` (Company $\to$ Drive FK) | `[:HOSTS_DRIVE]` Edge | `(Company)-[:HOSTS_DRIVE]->(Drive)` directed relationship. |
| `DRIVE_ELIGIBILITY` (Junction) | `[:ELIGIBLE_FOR]` Edge | Direct edge `(Drive)-[:ELIGIBLE_FOR]->(Program)`. Eliminates relational junction table join. |
| `APPLICATION` | `(:Application)` Node | Connects student to drive: `(Student)-[:SUBMITTED_APPLICATION]->(Application)-[:FOR_DRIVE]->(Drive)`. |
| `STUDENT_DAILY_ROUTINE_DRIVE` | `applyDate` property + Cypher check | Enforced via transactional Cypher query counting applications by student and calendar date. |
| `INTERVIEWER_ROUND` | `(:Interviewer)` Node | Permanent interviewer node with pinned round number `interviewRoundNo`. |
| `INTERVIEW` | `(:Interview)` Node + Edges | `(Application)-[:HAS_INTERVIEW]->(Interview)-[:CONDUCTED_BY]->(Interviewer)`. |
| `OFFER_LETTER` | `(:Offer)` Node + Edge | `(Application)-[:RECEIVED_OFFER]->(Offer)`. |
| `APPLICATION_AUDIT` | `(:AuditEvent)` Node + Edge | Immutable audit trail: `(Application)-[:HAS_AUDIT]->(AuditEvent)`. |
| `PORTAL_USER` | `(:PortalUser)` Node | Auth principal node: `(:PortalUser {username, passwordHash, role, referenceId})`. |
| `RECRUITER_COMPANY` | `[:AUTHORIZED_FOR]` Edge | `(PortalUser)-[:AUTHORIZED_FOR]->(Company)`. |
| `STUDENT_RESUME` | `(:Resume)` Node + File Storage | Binary stored on local filesystem (`./data/resumes/`), metadata + SHA-256 hash stored on `(:Resume)` node. |

---

### 2.2 PL/SQL Procedures & Triggers Migration Table

| Oracle PL/SQL Object | Business Invariant / Logic | Neo4j Implementation Location | Enforcement Mechanism |
| :--- | :--- | :--- | :--- |
| `PROCEDURE REGISTER_STUDENT_PROGRAM` | Idempotent student enrollment, program capacity validation | `Neo4jStudentService.registerStudentToProgram` | Spring `@Transactional` service method with Cypher `MERGE (s)-[:ENROLLED_IN]->(p)`. |
| `PROCEDURE APPLY_FOR_DRIVE` | 1-per-day rule, CGPA threshold, program eligibility, deadline | `Neo4jApplicationService.applyForDrive` | Atomic transaction checking `count(a.applyDate = today)`, graph path `(Drive)-[:ELIGIBLE_FOR]->(Program)`, and CGPA gate. |
| `PROCEDURE SCHEDULE_INTERVIEW` | Sequential round enforcement, pinned interviewer assignment | `Neo4jInterviewService.scheduleInterview` | Transactional service enforcing round ordering, creating `(:Interview)` and `[:CONDUCTED_BY]`. |
| `PROCEDURE ISSUE_OFFER` | Verifies candidate cleared all required rounds before offer | `Neo4jOfferService.issueOffer` | Queries `[:HAS_INTERVIEW]` clearance status; creates `(:Offer)` and transitions application to `OFFERED`. |
| `TRIGGER TRG_OFFER_APPLICATION_STATUS` | Transitions application to `OFFERED` on offer creation | `Neo4jOfferService.issueOffer` | Explicit domain service logic inside same ACID transaction. |
| `TRIGGER TRG_APPLICATION_AUDIT` | Writes immutable before/after audit log on status change | `Neo4jApplicationService` / `Neo4jOfferService` | Creates `(:AuditEvent)` node linked via `[:HAS_AUDIT]` to Application. |
| `TRIGGER TRG_OFFER_MUTEX_ACCEPTANCE` | Single accepted offer mutex; declines competing offers; sets student `PLACED` | `Neo4jOfferService.acceptOffer` | Cypher transaction atomically sets offer `ACCEPTED`, marks competing offers `DECLINED`, and sets student `placementStatus = 'PLACED'`. |
| `FUNCTION GET_PLACEMENT_RATE` | Ratio of placed students to total eligible students | `Neo4jReportService.getPlacementRate` | Cypher aggregation: `MATCH (s:Student) RETURN count(CASE WHEN s.placementStatus = 'PLACED' THEN 1 END) * 100.0 / count(s)`. |
| `FUNCTION GET_AVERAGE_PACKAGE` | Mean CTC across all accepted student offers | `Neo4jReportService.getAveragePackage` | Cypher aggregation: `MATCH (o:Offer {status: 'ACCEPTED'}) RETURN avg(o.ctcLpa)`. |

---

## 3. Side-by-Side Query Comparisons: Relational SQL vs Cypher

### Query 1: Find Eligible Placement Drives for a Student
*Business Requirement*: Determine all open placement drives that match the student's degree program, where the student's CGPA meets or exceeds the drive's cutoff, and the deadline has not expired.

#### Oracle 23ai Relational SQL (5-Way Join):
```sql
SELECT d.Drive_Id, d.Job_Title, c.Company_Name, d.CTC, d.Min_CGPA, d.Application_Deadline
FROM PLACEMENT_DRIVE d
JOIN COMPANY c ON d.Company_Id = c.Company_Id
JOIN DRIVE_ELIGIBILITY de ON d.Drive_Id = de.Drive_Id
JOIN REGISTERS r ON de.Program_Id = r.Program_Id
JOIN STUDENT s ON r.Student_Id = s.Student_Id
WHERE s.Student_Id = :studentId
  AND s.CGPA >= d.Min_CGPA
  AND d.Application_Deadline >= TRUNC(SYSDATE)
ORDER BY d.CTC DESC;
```

#### Neo4j Cypher (Zero-Join Path Pattern Traversal):
```cypher
MATCH (s:Student {studentId: $studentId})-[:ENROLLED_IN]->(p:Program)<-[:ELIGIBLE_FOR]-(d:Drive)
WHERE s.cgpa >= d.minCgpa
  AND d.applicationDeadline >= date()
MATCH (c:Company)-[:HOSTS_DRIVE]->(d)
RETURN d.driveId AS driveId,
       d.jobTitle AS jobTitle,
       c.companyName AS companyName,
       d.ctc AS ctc,
       d.minCgpa AS minCgpa,
       d.applicationDeadline AS deadline,
       [s.name, p.programName, d.jobTitle, c.companyName] AS traversalPath
ORDER BY d.ctc DESC;
```
*Advantage*: Cypher expresses the pattern directly through relationship arrows (`(s)-[:ENROLLED_IN]->(p)<-[:ELIGIBLE_FOR]-(d)`). No artificial foreign key columns, junction tables, or join predicates are needed.

---

### Query 2: Full Placement Journey of a Student
*Business Requirement*: Trace the complete recruitment history for a student: from application submission to interview rounds, evaluators, scores, and finalized job offers.

#### Oracle 23ai Relational SQL (6 Tables, Outer Joins, Subqueries):
```sql
SELECT s.Student_Id, s.Name, p.Program_Name, a.Application_Id, d.Job_Title, c.Company_Name,
       o.Offer_Id, o.CTC, o.Status AS Offer_Status,
       i.Round_No, i.Interviewer_Name, i.Result AS Interview_Result
FROM STUDENT s
LEFT JOIN REGISTERS reg ON s.Student_Id = reg.Student_Id
LEFT JOIN PROGRAM p ON reg.Program_Id = p.Program_Id
JOIN APPLICATION a ON s.Student_Id = a.Student_Id
JOIN PLACEMENT_DRIVE d ON a.Drive_Id = d.Drive_Id
LEFT JOIN COMPANY c ON d.Company_Id = c.Company_Id
LEFT JOIN INTERVIEW i ON a.Application_Id = i.Application_Id
LEFT JOIN OFFER_LETTER o ON a.Application_Id = o.Application_Id
WHERE s.Student_Id = :studentId
ORDER BY a.Apply_Date DESC, i.Round_No ASC;
```

#### Neo4j Cypher (Expressive Graph Path Traversal):
```cypher
MATCH (s:Student {studentId: $studentId})-[:SUBMITTED_APPLICATION]->(a:Application)-[:FOR_DRIVE]->(d:Drive)
OPTIONAL MATCH (s)-[:ENROLLED_IN]->(p:Program)
OPTIONAL MATCH (c:Company)-[:HOSTS_DRIVE]->(d)
OPTIONAL MATCH (a)-[:RECEIVED_OFFER]->(o:Offer)
OPTIONAL MATCH (a)-[:HAS_INTERVIEW]->(iv:Interview)-[:CONDUCTED_BY]->(interviewer:Interviewer)
RETURN s, p, a, d, c, o, collect(iv) AS interviews
ORDER BY a.applyDate DESC;
```

---

### Query 3: "Similar Students Placed" Recommendation Engine
*Business Requirement*: Recommend past placement trajectories by finding peer students who graduated from the **same academic program**, maintained a **similar academic CGPA ($\pm 1.5$ points)**, and successfully secured placement offers.

#### Oracle 23ai Relational SQL:
```sql
SELECT peer.Student_Id, peer.Name, peer.CGPA, c.Company_Name, d.Job_Title, o.CTC,
       ROUND(ABS(peer.CGPA - target.CGPA), 2) AS CGPA_Difference
FROM STUDENT target
JOIN REGISTERS r_target ON target.Student_Id = r_target.Student_Id
JOIN REGISTERS r_peer ON r_target.Program_Id = r_peer.Program_Id
JOIN STUDENT peer ON r_peer.Student_Id = peer.Student_Id
JOIN APPLICATION a ON peer.Student_Id = a.Student_Id
JOIN OFFER_LETTER o ON a.Application_Id = o.Application_Id
JOIN PLACEMENT_DRIVE d ON a.Drive_Id = d.Drive_Id
JOIN COMPANY c ON d.Company_Id = c.Company_Id
WHERE target.Student_Id = :studentId
  AND peer.Student_Id <> target.Student_Id
  AND ABS(peer.CGPA - target.CGPA) <= 1.5
  AND o.Status = 'ACCEPTED'
ORDER BY CGPA_Difference ASC, o.CTC DESC
FETCH FIRST 10 ROWS ONLY;
```

#### Neo4j Cypher (Multi-Hop Collaborative Peer Discovery):
```cypher
MATCH (target:Student {studentId: $studentId})-[:ENROLLED_IN]->(p:Program)<-[:ENROLLED_IN]-(peer:Student)
WHERE peer.studentId <> target.studentId
  AND abs(toFloat(peer.cgpa) - toFloat(target.cgpa)) <= 1.5
MATCH (peer)-[:SUBMITTED_APPLICATION]->(a:Application)-[:RECEIVED_OFFER]->(o:Offer {status: 'ACCEPTED'})
MATCH (a)-[:FOR_DRIVE]->(d:Drive)<-[:HOSTS_DRIVE]-(c:Company)
RETURN peer.studentId AS studentId,
       peer.name AS studentName,
       peer.cgpa AS cgpa,
       c.companyName AS companyName,
       d.jobTitle AS jobTitle,
       o.ctcLpa AS ctcLpa,
       round(abs(toFloat(peer.cgpa) - toFloat(target.cgpa)) * 100.0) / 100.0 AS diff
ORDER BY diff ASC, o.ctcLpa DESC
LIMIT 10;
```

---

## 4. Honest Technical Trade-offs & Limitations

While Neo4j provides extraordinary query clarity and traversal performance for recruitment pipelines, an objective engineering assessment reveals important trade-offs:

1. **Loss of In-Database Procedural Engines (PL/SQL)**:
   - In Oracle, stored procedures (`APPLY_FOR_DRIVE`, `SCHEDULE_INTERVIEW`) execute compiled procedural bytecode directly inside the database kernel with zero network latency.
   - In Neo4j, while APOC (Awesome Procedures on Cypher) provides utility functions, complex multi-step procedural workflows (such as validating interview round prerequisites followed by conditional offer releases) are orchestrated in the Spring Boot application service layer inside `@Transactional` boundaries.
2. **External Binary / Large Object Storage**:
   - Oracle database natively manages student resume PDFs using `BLOB` datatypes within secure tablespaces, benefiting from relational ACID backups and database-level encryption.
   - Neo4j is optimized for graph topologies and small property values; storing multi-megabyte binary PDFs directly as node properties causes severe JVM Heap and PageCache memory fragmentation. Resumes are consequently stored on the local filesystem (`./data/resumes/`) with SHA-256 hashes, MIME types, and file paths tracked on the `(:Resume)` node.
3. **Global Scan & Aggregate Performance**:
   - For analytical queries requiring full table scans over disconnected entities (e.g., computing standard deviation of salary across all records without graph filtering), relational columnar stores or distributed SQL engines can outperform graph traversals.
4. **Consistency Enforcement Shift**:
   - Foreign key cascading deletes and composite primary key enforcement in Oracle are built directly into the relational engine. In Neo4j, graph constraints ensure property uniqueness and node existence, while composite relationship rules (e.g., ensuring a student only attends batches belonging to their registered program) require validation at the domain layer.

---

## 5. Setup & Operational Verification Instructions

### 5.1 Quick Start via Script
To launch the entire platform with Neo4j as the active backend:
```bash
./run_portal.sh --db neo4j
```
This automatically:
1. Verifies Docker Desktop is running.
2. Starts the `portal-neo4j` container (Neo4j 5.26.0 Community + APOC) on ports `7474` (HTTP) and `7687` (Bolt).
3. Boots Spring Boot with `-Dspring-boot.run.profiles=neo4j` on port `8080`.
4. Boots the Vite React frontend on port `3000`.

To revert to Oracle 23ai backend:
```bash
./run_portal.sh
```

### 5.2 Verification of Services & Ports
- **Frontend App**: `http://localhost:3000`
- **Spring Boot Actuator Health**: `http://localhost:8080/actuator/health`
- **Neo4j Browser Console**: `http://localhost:7474` (User: `neo4j`, Password: `placement2026`)
- **Admin Cypher Console**: `http://localhost:3000/admin/sql-compiler` (automatically switches to Cypher execution when running under Neo4j profile)
- **Interactive Graph Visualization**: `http://localhost:3000/student/graph` or `http://localhost:3000/admin/graph`

### 5.3 Automated Integration Test Verification
Run the comprehensive test suite verifying all 9 ported invariants and showcase queries:
```bash
cd backend
mvn test -Dtest=Neo4jBackendIntegrationTests
```
*Result*: `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0` (BUILD SUCCESS).

---

## 6. Viva Demonstration Script

During the project evaluation / viva:
1. **Show Profile Decoupling**:
   - Demonstrate `run_portal.sh --db neo4j` versus `./run_portal.sh` (Oracle).
   - Point out that the React frontend code is 100% identical; both backends serve the exact same REST API contract.
2. **Demonstrate Cypher Console**:
   - Log in as Placement Admin (`admin` / `admin123`).
   - Navigate to `/admin/sql-compiler`.
   - Point out the dynamic banner indicating `Active Database Engine: Neo4j 5.26.0 (Graph Database)` and run Cypher queries directly in the browser.
3. **Demonstrate Interactive Graph Visualization**:
   - Log in as Student `25bce1799` / `password123`.
   - Click "Graph View" (`/student/graph`).
   - Render the student's multi-hop placement journey, eligible drives network, and similar student recommendations with interactive zoom and drag physics powered by `vis-network`.
4. **Demonstrate Business Rules**:
   - Show that CGPA thresholds and 1-application-per-day rules are strictly enforced inside graph transactions.
   - Show atomic offer acceptance: accepting an offer declines competing offers and marks the student `PLACED`.
