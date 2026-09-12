# Student Placement and Recruitment Portal

> **Academic Course Project for DA2 (Database Assessment 2)**  
> **Authoritative Database Engine**: Oracle SQL & PL/SQL  
> **Backend Architecture**: Spring Boot 3.4.3 Modular Monolith (Java 21/26)  
> **Frontend Architecture**: React 19, Vite, Tailwind CSS v4, Lucide Icons  
> **DevOps & Infrastructure**: Docker Compose, Redis, Prometheus, GitHub Actions CI  

---

## Table of Contents
1. [Project Overview & Academic Context](#1-project-overview--academic-context)
2. [Key Highlights & Architectural Decisions](#2-key-highlights--architectural-decisions)
3. [System Architecture](#3-system-architecture)
4. [Database & PL/SQL Engine (DA2 Requirements)](#4-database--plsql-engine-da2-requirements)
5. [User Portals & Features](#5-user-portals--features)
6. [Quickstart & Running Locally](#6-quickstart--running-locally)
7. [Automated Testing & CI/CD](#7-automated-testing--cicd)
8. [Documentation Index](#8-documentation-index)

---

## 1. Project Overview & Academic Context

The **Student Placement and Recruitment Portal** is a production-grade enterprise system designed to automate campus placement drives, candidate registrations, technical evaluations, and offer letter generation for university placement cells.

This project implements the **DA1 database design** (scanned handwritten relational analysis, entity mappings, and 1NF→BCNF normalization proofs) as an authoritative database using **Oracle SQL & PL/SQL**, integrated with a modern Spring Boot backend and responsive web interface.

---

## 2. Key Highlights & Architectural Decisions

1. **Strict DA1 Relational Fidelity (Locked Decisions)**:
   - **Composite Batch Primary Key**: `BATCH` primary key is `(Program_Id, Batch_No)`, correctly scoping batches to their parent programs without cross-program ID collisions.
   - **Pinned Interviewer Round Model**: Strictly implements `INTERVIEWER_ROUND(Interviewer_Name PK, Interview_Round_No)` enforcing the functional dependency `Interviewer_Name → Interview_Round_No`.
   - **21 Normalized Relational Tables**: Built upon the 13 base relations + 2 junction relations designed and proven to BCNF in DA1.
2. **Oracle SQL & PL/SQL as the Authoritative Engine**:
   - Stored procedures with transactional `SAVEPOINT` and `ROLLBACK TO SAVEPOINT` (`REGISTER_STUDENT_PROGRAM`, `APPLY_FOR_DRIVE`, `SCHEDULE_INTERVIEW`, `ISSUE_OFFER`).
   - Autonomic database triggers (`TRG_OFFER_APPLICATION_STATUS`, `TRG_APPLICATION_AUDIT`).
   - Analytic computational functions (`GET_PLACEMENT_RATE`, `GET_AVERAGE_PACKAGE`) and Ref Cursors.
   - Cursor-driven report generation package (`PKG_PLACEMENT_REPORTS`).
3. **Dual Database Profile Architecture**:
   - **Oracle Profile (`application-oracle.yml`)**: Authoritative engine for deployment with Oracle Database 23ai Free / 21c XE.
   - **H2 Profile (`application-h2.yml`)**: Zero-dependency offline test profile executing identical procedures and schemas for 100% automated CI test pass rates.
4. **Resilient Web Client**:
   - Fast React 19 + Vite frontend with Tailwind CSS v4 styling.
   - Real-time backend status detection with local offline state simulation matching PL/SQL semantics for seamless viva demonstrations.

---

## 3. System Architecture

```
                                  [ WEB BROWSER / CLIENT ]
                                             |
                         +-------------------+-------------------+
                         |                   |                   |
                   /student view      /recruiter view       /admin view
                         |                   |                   |
                         +-------------------+-------------------+
                                             |
                                    [ REACT 19 + VITE ]
                                 (Port 3000 / Nginx Alpine)
                                             |  REST / JWT
                                             v
                           [ SPRING BOOT 3.4 MODULAR MONOLITH ]
                                 (Port 8080 / Java 21)
                                             |
                  +--------------------------+--------------------------+
                  |                          |                          |
           [ Spring Security ]      [ JPA Repositories ]      [ Spring JDBC DAO ]
              (JWT Bearer)             (CRUD & Domain)         (PL/SQL Procedures)
                  |                          |                          |
                  +--------------------------+--------------------------+
                                             |
                                             v
                             [ ORACLE DATABASE 23ai FREE ]
                               (Port 1521 / FREEPDB1)
                     - 21 Normalized BCNF Relational Tables
                     - Stored Procedures, Functions & Ref Cursors
                     - Triggers & Automatic Audit Trail
```

---

## 4. Database & PL/SQL Engine (DA2 Requirements)

| Category | Requirement | Implemented Object / Script |
|----------|-------------|------------------------------|
| **DDL** | Create Tables, Constraints | `database/migrations/V1__create_schema.sql`, `V2__constraints.sql` |
| **Indexes** | Performance B-Tree Indexes | `database/migrations/V3__indexes.sql` |
| **Views** | Analytical & Summary Views | `database/migrations/V4__views.sql` (`V_STUDENT_APPLICATIONS`, `V_OFFER_SUMMARY`, etc.) |
| **DML** | Inserts, Realistic Seed Data | `database/migrations/V5__sample_data.sql` (22 students, 10 companies, drives, offers) |
| **PL/SQL Procedures** | Transactional Procedures | `database/migrations/V6__procedures.sql` (`REGISTER_STUDENT_PROGRAM`, `APPLY_FOR_DRIVE`, `SCHEDULE_INTERVIEW`, `ISSUE_OFFER`) |
| **PL/SQL Functions** | Metrics & Ref Cursors | `database/migrations/V7__functions.sql` (`GET_PLACEMENT_RATE`, `GET_AVERAGE_PACKAGE`, `GET_ELIGIBLE_STUDENTS`) |
| **PL/SQL Cursors** | Batch Reports Package | `database/migrations/V8__cursors.sql` (`PKG_PLACEMENT_REPORTS`) |
| **PL/SQL Triggers** | Autonomic Business Rules | `database/migrations/V9__triggers.sql` (`TRG_OFFER_APPLICATION_STATUS`, `TRG_APPLICATION_AUDIT`) |
| **DQL** | Complex Queries & Joins | `database/migrations/V10__demo_queries.sql` (5-table JOIN, GROUP BY + HAVING, DENSE_RANK) |
| **DCL** | Roles, Grants, Revokes | `database/dcl/roles_and_grants.sql` (`ROLE_PORTAL_STUDENT`, `ROLE_PORTAL_RECRUITER`, `ROLE_PORTAL_ADMIN`) |
| **TCL** | Commit, Savepoint, Rollback | `database/tcl/transaction_scenarios.sql` |

---

## 5. User Portals & Features

### Student Portal (`/student`)
- **Profile Card**: CGPA, Branch, Registration ID, and Super Dream eligibility badge.
- **Enrolled Programs**: Batch schedule, classroom location, and duration.
- **Active Placement Drives**: Real-time eligibility checking (`student.cgpa >= drive.minCgpa`) with **1-Click Apply** invoking PL/SQL `APPLY_FOR_DRIVE`.
- **Applications Tracker**: Visual pipeline stepper (`APPLIED` ➔ `SHORTLISTED` ➔ `INTERVIEWING` ➔ `SELECTED`).
- **Scheduled Interviews**: View assigned round, pinned interviewer, and scores.
- **Offers Vault**: View issued offers and generate formal offer letters.

### Recruiter Portal (`/recruiter`)
- **Company Overview**: Contact info, partner ID, and drive metrics.
- **Candidate Pipeline**: Filter applicants by drive or name; shortlist candidates.
- **Interview Scheduling**: Assign rounds to pinned interviewers (`INTERVIEWER_ROUND`) and delivery modes (Online/Offline) invoking PL/SQL `SCHEDULE_INTERVIEW`.
- **Score Recording**: Grade candidates across OA, GD, and HR rounds.
- **Offer Issuance**: Award CTC package (LPA) invoking PL/SQL `ISSUE_OFFER` which fires `TRG_OFFER_APPLICATION_STATUS`.

### Admin & DBA Portal (`/admin`)
- **Executive Placement KPIs**: Placement rate (%), Average CTC (LPA), Total offers.
- **Interactive Charts**: Salary Tier distribution (Super Dream, Dream, Regular) and application funnel.
- **Students Directory**: Searchable directory of registered candidates.
- **Database Trigger Audit Trail**: Live view of `APPLICATION_AUDIT` table records populated by autonomic triggers.
- **DA2 DQL Runner**: Interactive query runner demonstrating multi-table joins, aggregations with `HAVING`, and window functions.

---

## 6. Quickstart & Running Locally

### Option 1: Docker Compose (Recommended)

```bash
cd infrastructure
docker compose up --build -d
```
Access the application at `http://localhost:3000`.

### Option 2: Local Development

#### Start Backend:
```bash
cd backend
export JAVA_HOME=/opt/homebrew/opt/openjdk
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

#### Start Frontend:
```bash
cd frontend
npm run dev
```

### Demo Accounts

| Persona | Username | Password | Role | Reference |
|---------|----------|----------|------|-----------|
| **Student** | `student1` | `password123` | `ROLE_STUDENT` | `STU001` (Aarav Sharma, CGPA: 9.42) |
| **Recruiter** | `recruiter1` | `password123` | `ROLE_RECRUITER` | `C001` (Microsoft IDC) |
| **Admin** | `admin` | `admin123` | `ROLE_ADMIN` | `ADMIN` (Placement Cell) |

---

## 7. Automated Testing & CI/CD

### Backend Tests
```bash
cd backend
mvn clean test -o
```
**Results**: 11 out of 11 tests pass (0 failures, 0 errors).

### Frontend Production Build
```bash
cd frontend
npm run build
```
**Results**: Production bundle built cleanly in ~400ms.

---

## 8. Documentation Index

- [DA1 Analysis & Normalization Proofs](docs/da1-analysis.md)
- [Locked DA1/DA2 Decisions Record](docs/da1-da2-decisions.md)
- [System Architecture Specification](docs/architecture.md)
- [Database & PL/SQL Engine Reference](docs/database.md)
- [REST API Specification](docs/api.md)
- [Setup & Installation Guide](docs/setup.md)
- [Testing Strategy & Test Suite](docs/testing.md)
- [DA2 Traceability Matrix](docs/da2-traceability.md)
- [Academic Demonstration & Viva Guide](docs/demo-guide.md)
