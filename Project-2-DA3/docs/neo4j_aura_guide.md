# Oracle Database 23ai & Neo4j Aura Coexistence & Verification Guide

## 1. Architecture Overview

The **Student Placement and Recruitment Portal** operates on a **dual-engine coexistence architecture**:

1. **Oracle Database 23ai Free (Primary OLTP)**:
   - **Authoritative Source of Truth** for all transactional operations: student registration, company profiles, campus placement drives, job applications, interview rounds, and offer letter generation.
   - Enforces referential integrity, check constraints, foreign keys, and ACID transactions.
   - Guarded by server-side read-only SQL validation (`SqlReadOnlyValidator`) in the administrative query editor.

2. **Neo4j Aura Cloud (Graph Projection Engine)**:
   - Operates as a **read-optimized, multi-hop graph projection**.
   - Traverses complex placement relationships: Student $\rightarrow$ Application $\rightarrow$ PlacementDrive $\leftarrow$ Company, multi-round interview funnels, and eligibility matching.
   - Communicates with Neo4j Aura via the official `neo4j-java-driver` over secure TLS (`neo4j+s://`).
   - Coexists in the **same application deployment** without replacing the Oracle configuration or requiring a separate profile.

3. **Graceful Fallback & Independence**:
   - Controlled via the `portal.neo4j.enabled` configuration toggle (default `false`).
   - If Neo4j Aura is disabled or unavailable due to cloud network connectivity, **all Oracle-backed portal operations continue functioning seamlessly**.
   - No PII, passwords, password hashes, or sensitive tokens are ever projected into Neo4j nodes.

---

## 2. Configuration & Environment Variables

All Neo4j Aura credentials must be supplied via environment variables or Spring configuration properties. **Never hardcode or commit credentials.**

| Environment Variable | Spring Property | Default | Description |
| :--- | :--- | :--- | :--- |
| `PORTAL_NEO4J_ENABLED` | `portal.neo4j.enabled` | `false` | Master toggle for Neo4j Aura integration. Set to `true` to enable. |
| `NEO4J_URI` | `portal.neo4j.uri` | `neo4j+s://demo.databases.neo4j.io` | Neo4j Aura Cloud connection URI (uses encrypted bolt protocol). |
| `NEO4J_USERNAME` | `portal.neo4j.username` | `neo4j` | Neo4j Aura database username. |
| `NEO4J_PASSWORD` | `portal.neo4j.password` | `""` | Neo4j Aura database password. |

### Running with Neo4j Aura Enabled:

```bash
export PORTAL_NEO4J_ENABLED=true
export NEO4J_URI=neo4j+s://<your-aura-instance-id>.databases.neo4j.io
export NEO4J_USERNAME=neo4j
export NEO4J_PASSWORD=<your-aura-password>

cd backend
mvn spring-boot:run
```

If these environment variables are omitted, the application starts with `portal.neo4j.enabled=false`. The portal remains 100% operational on Oracle, and the Cypher workspace and Graph Visualizer report "Neo4j Aura is disabled (portal.neo4j.enabled=false)".

---

## 3. End-to-End Synchronization Path (Oracle $\rightarrow$ Neo4j Aura)

The synchronization pipeline is exposed via:
- **REST Endpoint**: `POST /api/v1/graph/sync` (Requires `ROLE_ADMIN`)
- **Frontend UI**: One-click **"Sync Oracle to Aura"** button located in the SQL & Cypher Workspace header and on the Graph Visualizer page.

### What gets synchronized:
- `PROGRAM` $\rightarrow$ `(:Program {programId, programName, department})`
- `STUDENT` $\rightarrow$ `(:Student {studentId, name, cgpa, branch, placementStatus})` *(Passphrases and password hashes are strictly excluded)*
- `COMPANY` $\rightarrow$ `(:Company {companyId, companyName, industry, email})`
- `PLACEMENT_DRIVE` $\rightarrow$ `(:Drive {driveId, jobTitle, ctcLpa, minCgpa, location})`
- `APPLICATION` $\rightarrow$ `(:Application {applicationId, status, applyDate})` with `[:SUBMITTED_APPLICATION]`, `[:FOR_DRIVE]`
- `INTERVIEW` $\rightarrow$ `(:Interview {interviewId, interviewerName, result})` with `[:HAS_INTERVIEW]`
- `OFFER_LETTER` $\rightarrow$ `(:Offer {offerId, ctcLpa, status, offerDate})` with `[:RECEIVED_OFFER]`

---

## 4. Graph Schema & Verification Script

The complete constraints and verification queries are provided in:
`database/neo4j/aura_setup.cypher`

### Verification Steps in Neo4j Browser / Aura Console:

1. Open the **Neo4j Aura Console** or local Neo4j Browser connected to your Aura instance.
2. Run the uniqueness constraints from Part 1 of `database/neo4j/aura_setup.cypher`:
   ```cypher
   CREATE CONSTRAINT student_id_unique IF NOT EXISTS
   FOR (s:Student) REQUIRE s.studentId IS UNIQUE;
   ```
3. Trigger the sync via the portal or curl:
   ```bash
   curl -X POST http://localhost:8080/api/v1/graph/sync \
     -H "Authorization: Bearer <ADMIN_JWT_TOKEN>"
   ```
4. Run sample query 3.1 to inspect the end-to-end student journey:
   ```cypher
   MATCH (s:Student)-[:SUBMITTED_APPLICATION]->(a:Application)-[:FOR_DRIVE]->(d:Drive)<-[:HOSTS_DRIVE]-(c:Company)
   OPTIONAL MATCH (a)-[:HAS_INTERVIEW]->(iv:Interview)
   OPTIONAL MATCH (a)-[:RECEIVED_OFFER]->(o:Offer)
   RETURN s.studentId AS StudentID,
          s.name AS StudentName,
          s.cgpa AS CGPA,
          c.companyName AS CompanyName,
          d.jobTitle AS Role,
          d.ctcLpa AS Package_LPA,
          a.status AS ApplicationStatus,
          iv.interviewerName AS Interviewer,
          iv.result AS InterviewResult,
          o.ctcLpa AS OfferedCTC,
          o.status AS OfferStatus;
   ```

---

## 5. UI Workspaces

1. **SQL & Cypher Workspace (`/admin/sql` or `/admin/compiler`)**:
   - **Tab 1: Oracle SQL Editor (23ai)**: Authoritative OLTP query environment with live connection status (`localhost:1521/FREEPDB1`). Server-side validation restricts queries to safe, read-only SELECT and CTE statements.
   - **Tab 2: Neo4j Aura Cypher Editor**: Declarative graph query editor connected to Aura with preloaded sample queries for journeys, funnels, and eligibility matching.
2. **Interactive Graph Visualizer (`/graph`)**:
   - Interactive, hardware-accelerated force-directed graph canvas (powered by `vis-network`).
   - Filter by node types: Students, Companies, Placement Drives, Applications, Offers.
   - Color-coded node badges with direct click inspection of node properties.
   - Direct link to switch back to the Cypher workspace.
