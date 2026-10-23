# 🎓 VIT Student Placement & Recruitment Portal — Academic Submissions Repository

> **Academic Course Project**: Database Management Systems (DBMS)  
> **Course Submissions**: DA2 (Relational Oracle 23ai Engine) & DA3 (Polyglot Oracle 23ai + Neo4j Aura Graph Engine)  
> **Repository**: [https://github.com/achyutranaut/placement-and-recruitment-portal](https://github.com/achyutranaut/placement-and-recruitment-portal)

---

## 📌 Repository Organization & Faculty Guidance

Following academic faculty instructions for the **Digital Assignment 3 (DA3)** submission, this repository consolidates both academic deliverables into two independently operable, self-contained project directories under a single unified GitHub repository:

```
placement-and-recruitment-portal/
├── Project-1-Existing/      # Baseline DA2 submission (Oracle 23ai + Spring Boot + React)
├── Project-2-DA3/           # Official DA3 submission (Oracle 23ai + Neo4j Aura Cloud Graph)
└── README.md                # Master repository documentation & cross-project navigation
```

### Purpose of the Two-Project Structure
* **Project 1 — Existing (`Project-1-Existing/`)**: Preserves the complete, original baseline source code, relational schema, PL/SQL stored procedures, and configuration exactly as submitted for **DA2**. No DA3 or graph dependencies are present in this folder.
* **Project 2 — DA3 (`Project-2-DA3/`) [OFFICIAL SUBMISSION FOR DA3]**: Houses the comprehensive **DA3** deliverable featuring the polyglot persistence architecture. It extends the authoritative Oracle 23ai database with a **Neo4j Aura Cloud** property graph, interactive hardware-accelerated network visualizations (`/graph`), dual in-browser SQL & Cypher compiler, and security hardening.

---

## 🗂️ Project Comparison Matrix

| Attribute | Project 1 (`Project-1-Existing`) | Project 2 (`Project-2-DA3`) — **DA3 SUBMISSION** |
|:---|:---|:---|
| **Academic Target** | DBMS Digital Assignment 2 (DA2) | DBMS Digital Assignment 3 (DA3) |
| **Primary Relational Store** | Oracle Database 23ai Free (`FREEPDB1`) | Oracle Database 23ai Free (`FREEPDB1`) (Authoritative OLTP) |
| **Graph Database** | *None* | **Neo4j Aura Cloud** (5.x Cypher Property Graph) |
| **Persistence Pattern** | Spring Data JPA + Spring JDBC (`SimpleJdbcCall`) | Dual Persistence: Oracle OLTP + Neo4j On-Demand Graph Projection |
| **Frontend UI** | React 19, Tailwind CSS v4, Lucide Icons | React 19, Tailwind CSS v4, Vis.js Network Hardware Acceleration |
| **In-Browser Terminal** | Dedicated Oracle SQL Compiler | Dual Workspace: Oracle SQL & Neo4j Cypher Compilers |
| **Graph Visualizer** | *None* | Interactive Topology Explorer (`/graph`) with deduplication |
| **Security Controls** | Stateless JJWT, BCrypt, Spring Security 6 | AST SQL Validation Guard, Login Rate-Limiter, PDF Streaming Guard |
| **Frontend Dev Port** | `3000` | `3001` (Configurable via `FRONTEND_PORT`) |
| **Backend Dev Port** | `8080` | `8080` (Configurable via `SERVER_PORT`) |
| **Automated Test Suite** | 133 Java Classes Compiled, Unit & Integration Tests | **171 Backend Tests (100% Pass)**, **11 Frontend Tests (100% Pass)** |

---

## 📁 Repository Directory Structure

```text
placement-and-recruitment-portal/
├── Project-1-Existing/                      # --- PROJECT 1: ORIGINAL DA2 RELATIONAL PORTAL ---
│   ├── backend/                             # Spring Boot 3.4.3 backend (Oracle JPA + JDBC only)
│   │   ├── pom.xml                          # Maven POM (No Neo4j dependencies)
│   │   └── src/                             # 133 Java domain classes & 17 unit test suites
│   ├── database/                            # Oracle Database DDL, PL/SQL, & Flyway migrations
│   │   ├── dcl/                             # Database Control Language (Roles, Grants)
│   │   ├── migrations/                      # Flyway relational migrations (V1 through V27)
│   │   └── tcl/                             # Transaction Control Language scenarios
│   ├── docs/                                # Technical specifications & DA2 traceability
│   │   ├── DA1_25BCE1799_DBMS.pdf           # Original DA1 design paper & normalization proofs
│   │   ├── da1-analysis.md                  # Relational schema functional dependency breakdown
│   │   ├── da2-traceability.md              # Traceability matrix for DA2 faculty evaluation
│   │   └── demo-guide.md                    # 10-minute viva evaluation demonstration script
│   ├── frontend/                            # React 19 + Tailwind CSS single-page application
│   │   ├── package.json                     # Frontend dependencies (Port 3000 default)
│   │   └── src/                             # Student, Recruiter, and Admin portal interfaces
│   ├── infrastructure/                      # Docker orchestration & environment templates
│   │   ├── docker-compose.yml               # Local Oracle 23ai & Redis container definitions
│   │   └── .env.example                     # Environment configuration template
│   ├── .env.example                         # Root configuration template
│   ├── README.md                            # Comprehensive DA2 project guide
│   └── run_portal.sh                        # Automated service launcher for Project 1
│
├── Project-2-DA3/                           # --- PROJECT 2: DA3 POLYGLOT GRAPH EDITION ---
│   ├── backend/                             # Spring Boot backend with Oracle 23ai + Neo4j Aura
│   │   ├── pom.xml                          # Maven POM with neo4j-java-driver
│   │   └── src/                             # 144 Java classes, Neo4j services, & AST security guards
│   ├── database/                            # Dual-database migrations & Cypher schemas
│   │   ├── migrations/                      # Flyway relational migrations (V1 through V27)
│   │   ├── neo4j/                           # Graph schema & Cypher indexes
│   │   │   └── aura_setup.cypher            # Neo4j Aura cloud initialization & traversal scripts
│   │   └── dcl/, tcl/, scripts/             # Relational security & transaction scripts
│   ├── docs/                                # Complete documentation suite
│   │   ├── neo4j_aura_guide.md              # Dual-engine architectural & coexistence guide
│   │   ├── da2-traceability.md              # Historical requirement traceability
│   │   └── FINAL_VERIFICATION_REPORT.md     # Security audit & regression test execution report
│   ├── frontend/                            # React 19 interface with hardware-accelerated graph visualizer
│   │   ├── package.json                     # Dependencies including vis-network & vis-data
│   │   └── src/                             # Includes GraphViewPage.jsx & Vitest test suites
│   ├── infrastructure/                      # Hardened container definitions
│   │   ├── docker-compose.yml               # Loopback-isolated Redis (`127.0.0.1:6379`) & Oracle
│   │   └── .env.example                     # Infrastructure environment template
│   ├── .env.example                         # DA3 environment template with Neo4j Aura placeholders
│   ├── README.md                            # Comprehensive DA3 technical guide
│   └── run_portal.sh                        # Hardened multi-service launcher (Port 3001)
│
└── README.md                                # This master navigation document
```

---

## 💻 Prerequisites & System Requirements

Ensure the host workstation meets the following tooling requirements before running either project:

1. **Java Development Kit (JDK)**: Java 21 LTS or newer (Tested on OpenJDK 26.0.2).
2. **Apache Maven**: Version 3.9+ (Installed via `brew install maven` on macOS or package manager).
3. **Node.js & npm**: Node.js v20.x or v24.x and npm 10+ (Tested on Node v24.14.0).
4. **Docker Desktop**: Docker Engine 24+ and Docker Compose v2+ (Required for local Oracle Database 23ai and Redis containers).
5. **Neo4j Aura Cloud Instance (For Project 2 only)**: A free Neo4j AuraDB instance (`neo4j+s://...`). *Note: Project 2 includes graceful degradation—if Neo4j credentials are not supplied, the portal continues to run seamlessly in Oracle-only mode.*

---

## 🚀 Execution & Verification Guides

### Project 1: Baseline Existing Project (`Project-1-Existing`)

Project 1 represents the pure relational DA2 baseline.

#### 1. Setup Environment
```bash
cd Project-1-Existing
cp .env.example .env
```

#### 2. Start Supporting Containers
```bash
cd infrastructure
docker compose up -d oracle-db redis
cd ..
```

#### 3. Run Automated Tests
```bash
# Backend test compilation & non-destructive unit/integration tests
cd backend
mvn test-compile
mvn test -Dtest='!LiveOracleResumeIntegrationTests'

# Frontend build verification
cd ../frontend
npm install
npm run build
```

#### 4. Launch Application
```bash
# Execute the self-contained launcher
./run_portal.sh
```
* **Frontend Portal**: `http://localhost:3000`
* **Backend API**: `http://localhost:8080/api/v1`

---

### Project 2: DA3 Polyglot Graph Portal (`Project-2-DA3`) — **OFFICIAL SUBMISSION**

Project 2 contains the full DA3 graph integration, security hardening, and test suites.

#### 1. Setup Environment
```bash
cd Project-2-DA3
cp .env.example .env
```
*(Optional: Populate `NEO4J_URI`, `NEO4J_USERNAME`, and `NEO4J_PASSWORD` in `.env` with your Neo4j Aura instance credentials. Set `PORTAL_NEO4J_ENABLED=true` to enable graph visualizer sync.)*

#### 2. Run Automated Verification Suites
Both test suites have been executed and verified empirically:

```bash
# Frontend Unit & Security Regression Suite (Vitest)
cd frontend
npm install
npm run build
npx vitest run
```
**Empirical Result**: **11 passed (11 total)** across Graph relationship deduplication, AuthContext security, and PDF BLOB lifecycle.

```bash
# Backend Automated Test Suite (171 tests across 19 suites)
cd ../backend
mvn test -Dtest='!LiveOracleResumeIntegrationTests'
```
**Empirical Result**: **171 passed, 0 failures, 0 errors, 0 skipped** across SQL AST validation, Cypher security, and placement workflows.

#### 3. Launch Application
```bash
# Execute the hardened multi-tier launcher
./run_portal.sh
```
* **Frontend Portal**: `http://localhost:3001`
* **Backend API**: `http://localhost:8080/api/v1`
* **Interactive Graph Visualizer**: `http://localhost:3001/graph`
* **In-Browser Dual Compiler (SQL & Cypher)**: `http://localhost:3001/admin/sql`
* **Actuator System Health**: `http://localhost:8080/actuator/health`

---

## 🔒 Security & Credential Hygiene

In strict adherence to academic and industry security best practices:
1. **Zero Hardcoded Credentials**: No production passwords, JWT private keys, or cloud database credentials exist in committed repository files.
2. **Ignored Secret Files**: `.env`, `backend/.env`, `target/`, and `node_modules/` are strictly ignored across root and subproject `.gitignore` manifests.
3. **Template Placeholders Only**: All `.env.example` files contain sanitized, descriptive placeholder variables.
4. **Network Isolation**: The Redis caching service is explicitly bound to `127.0.0.1` loopback interface to prevent unauthorized network bridging.

---

## 👨‍🎓 Academic Declaration & Course Context

* **Course**: Database Management Systems (DBMS)
* **Student Name**: Achyut Ranaut
* **Student Registration Number**: 25BCE1799
* **Institution**: Vellore Institute of Technology (VIT), Chennai
* **Primary Evaluation Deliverable**: **`Project-2-DA3/`**
