# System Architecture & Technical Design Specification

**Project**: Student Placement and Recruitment Portal (DA2)  
**Architecture Pattern**: Modular Monolith  
**Tech Stack**: Next.js 16 | Spring Boot 3.4+ (Java 21/25) | Oracle Database 19c/21c/23ai | Redis  

---

## 1. High-Level Architectural Topology

The system uses a **Modular Monolith** architecture designed for enterprise robustness, maintainability, strict transaction boundaries, and direct execution of Oracle SQL & PL/SQL primitives.

```
+-------------------------------------------------------------------------+
|                         Next.js 16 App Router                           |
|        (React 19, TypeScript, Tailwind CSS, shadcn/ui, Recharts)        |
+-------------------------------------------------------------------------+
                                    |
                            HTTPS / REST (JSON)
                           JWT Bearer Authentication
                                    v
+-------------------------------------------------------------------------+
|                  Spring Boot 3.4+ Modular Monolith                      |
|                                                                         |
|  [Controllers]  /api/v1/{auth, students, programs, batches,             |
|                          companies, jobs, drives, applications,         |
|                          interviews, offers, reports, admin}            |
|                                    |                                    |
|  [Security]     Spring Security 6.x + Stateless JWT Filter              |
|                                    |                                    |
|  [Service Layer] Business logic, cross-module service interfaces,       |
|                  declarative @Transactional boundaries                  |
|                   /                                \                    |
|       [Spring Data JPA]                    [Spring JDBC]                |
|      Standard CRUD Queries           PL/SQL Stored Procedures,          |
|      Entity Lifecycle Mapping         Stored Functions, REF CURSORS     |
+-------------------------------------------------------------------------+
             |                                    |
             v                                    v
+--------------------------+       +--------------------------------------+
|       Redis Cache        |       |         Oracle Database Engine       |
| Selective cache-aside:   |       | (Authoritative Source of Truth)      |
| - Programs & Batches     |       | - 19 Normalized Relational Tables    |
| - Companies & Jobs       |       | - Declarative PK/FK/CHECK/Indexes    |
| - Aggregated Reports     |       | - Analytical Views (V1..V4)          |
|                          |       | - Sample Placement Data (V5)         |
|                          |       | - Stored Procedures (V6)             |
|                          |       | - Stored Functions (V7)              |
|                          |       | - Explicit Cursors & Packages (V8)   |
|                          |       | - Lifecycle Triggers & Audit (V9)    |
|                          |       | - DQL / DCL / TCL Scenarios (V10)    |
+--------------------------+       +--------------------------------------+
```

---

## 2. Backend Module Decomposition (Package-by-Feature)

The Spring Boot backend enforces strict modular encapsulation. No module repository is directly accessed by another module; inter-module operations occur via clean service interfaces.

```
com.placement.portal
├── auth/            # Authentication, JWT issue/validate, PortalUser, Security details
├── student/         # Student profile, phones, academic records
├── program/         # Programs, Batches, Attendance, Assessments
├── company/         # Companies, corporate emails, contact phones, job postings
├── recruitment/     # Placement drives, eligibility criteria thresholds
├── application/     # Student applications, daily routine limits, status lifecycle
├── interview/       # Multi-round evaluations (OA, GD, HR), interviewer assignments
├── offer/           # Job offer issuance, CTC records, acceptance tracking
├── reports/         # Executive analytics, placement rate, salary distribution
├── admin/           # Administrative overview, aggregate operations
├── common/          # ApiResponse, PageResponse, GlobalExceptionHandler, BaseEntity
└── config/          # SecurityConfig, JwtFilter, OpenApiConfig, RedisConfig, JdbcConfig
```

---

## 3. Dual-Access Persistence Strategy: JPA + Spring JDBC

### 3.1 Spring Data JPA (Standard CRUD)
- Used for entity management, relational browsing, lookups, and basic administrative updates.
- Entities strictly decoupled from external representations using MapStruct/manual DTO mappers.
- JPA entities are never exposed directly to REST clients.

### 3.2 Spring JDBC (PL/SQL Integration)
Complex business workflows and academic requirements execute inside Oracle PL/SQL stored procedures and functions:

1. **`REGISTER_STUDENT_PROGRAM` (Procedure)**
   - Validates student existence and program capacity.
   - Inserts `REGISTERS` row in a transactional block with SAVEPOINT.
   - Raises application-level custom exceptions on conflict.

2. **`APPLY_FOR_DRIVE` (Procedure)**
   - Validates student CGPA against drive minimum threshold.
   - Enforces `STUDENT_DAILY_ROUTINE_DRIVE` constraint (maximum 1 application per student per day).
   - Atomically inserts into `STUDENT_DAILY_ROUTINE_DRIVE` and `APPLICATION` (`APPLIED` status).

3. **`SCHEDULE_INTERVIEW` (Procedure)**
   - Verifies application is in `SHORTLISTED` or `APPLIED` status.
   - Assigns interviewer and schedules interview round.
   - Updates `APPLICATION.STATUS` to `INTERVIEWING`.

4. **`ISSUE_OFFER` (Procedure)**
   - Validates successful interview result.
   - Inserts row into `OFFER_LETTER`.
   - Triggers `TRG_OFFER_APPLICATION_STATUS` which transitions `APPLICATION.STATUS` to `SELECTED`.

5. **Analytical Functions & REF CURSORs**
   - `GET_PLACEMENT_RATE(p_program_id)`: Calculates placement percentage.
   - `GET_AVERAGE_PACKAGE(p_industry)`: Computes mean CTC.
   - `GET_ELIGIBLE_STUDENTS(p_drive_id)`: Returns dynamic `SYS_REFCURSOR` of eligible students for a drive.
   - `PKG_PLACEMENT_REPORTS`: Cursor-based batch reporting package.

---

## 4. Security Architecture

- **Authentication**: Stateless JWT token authentication via Spring Security 6.
- **Password Encryption**: Strong BCrypt hashing (12 salt rounds).
- **Role-Based Access Control (RBAC)**:
  - `STUDENT`: Access to `/api/v1/students/**`, drive listings, own applications, interviews, and offers.
  - `RECRUITER`: Access to `/api/v1/companies/**`, jobs, drive management, applicant shortlisting, interviews, and offers.
  - `ADMIN`: Full administrative control across all system entities, batch assignments, and executive reports (`/api/v1/admin/**`, `/api/v1/reports/**`).

---

## 5. Cache-Aside Architecture with Redis

- **Cached Datasets**:
  - `programs:all` (TTL: 10 minutes, evicted on program modification)
  - `companies:list` (TTL: 10 minutes, evicted on company creation)
  - `reports:placement_summary` (TTL: 5 minutes)
- **Non-Cached Datasets**:
  - `APPLICATION`, `INTERVIEW`, `OFFER_LETTER` are strictly read from Oracle to ensure real-time consistency with triggers and transaction rollback states.

---

## 6. Observability & Quality Assurance

- **Spring Boot Actuator**: Health, info, metrics, and Prometheus scrape endpoints enabled at `/actuator`.
- **Micrometer & Prometheus**: Exposes JVM memory, connection pool metrics (`HikariCP`), and REST latency.
- **Flyway Database Migration**: Automated schema evolution guaranteeing migration reproducibility from scratch.
