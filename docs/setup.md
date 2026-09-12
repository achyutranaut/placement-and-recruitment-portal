# Setup & Installation Guide

This document provides step-by-step instructions to set up, build, and run the **Student Placement and Recruitment Portal** locally or in containers.

---

## 1. Prerequisites

Ensure your machine has the following tools installed:

| Tool | Minimum Version | Verified Version | Purpose |
|------|-----------------|------------------|---------|
| **Java Development Kit (JDK)** | Java 21 LTS | OpenJDK 21/26 | Spring Boot backend runtime |
| **Apache Maven** | 3.9+ | 3.9.9 | Backend build & dependency management |
| **Node.js & npm** | Node 20+ / npm 10+ | Node 24.14 / npm 11.9 | Frontend runtime and package manager |
| **Docker & Docker Compose** | 24+ | Recent | Containerized Oracle DB, Redis, and services |
| **Oracle Database** (Optional) | 21c XE / 23ai Free | 23ai Free | Authoritative relational database engine |

---

## 2. Quickstart with Docker Compose

To start the entire multi-service stack (Oracle Database 23ai, Redis, Spring Boot backend, and React frontend) with a single command:

```bash
# 1. Navigate to infrastructure directory
cd infrastructure

# 2. Copy environment file
cp .env.example .env

# 3. Launch the container stack
docker compose up --build -d

# 4. Check container health status
docker compose ps
```

### Service Access URLs:
- **Frontend Portal**: `http://localhost:3000`
- **Backend REST API**: `http://localhost:8080/api/v1`
- **Swagger / OpenAPI UI**: `http://localhost:8080/swagger-ui.html`
- **Spring Actuator Health**: `http://localhost:8080/actuator/health`
- **Prometheus Metrics**: `http://localhost:9090`
- **Oracle Database**: `localhost:1521` (Service: `FREEPDB1`, User: `c##placement_app`)

---

## 3. Local Development Setup (Dual-Profile Support)

The application supports **dual profiles**:
1. `h2`: Zero-configuration local development and offline unit testing without requiring Oracle to be installed.
2. `oracle`: Production profile connecting to a live Oracle Database 21c/23c instance.

### 3.1 Backend Setup

```bash
cd backend

# Option A: Run in H2 mode (zero setup required)
export JAVA_HOME=/opt/homebrew/opt/openjdk  # (or path to your JDK)
mvn spring-boot:run -Dspring-boot.run.profiles=h2

# Option B: Run in Oracle mode (requires Oracle running on port 1521)
mvn spring-boot:run -Dspring-boot.run.profiles=oracle
```

### 3.2 Running Backend Automated Tests

All 11 unit and integration tests can be executed offline using the cached Maven repository:

```bash
cd backend
mvn clean test -o
```

---

## 4. Frontend Setup

```bash
cd frontend

# 1. Install dependencies
npm install --prefer-offline

# 2. Start development server (with proxy to port 8080)
npm run dev

# 3. Build production bundle
npm run build
```

Open `http://localhost:3000` in your web browser.

---

## 5. Demo Credentials

The portal is seeded with realistic accounts for academic demonstration:

| Persona | Username | Password | Role | Reference ID / Notes |
|---------|----------|----------|------|-----------------------|
| **Student** | `student1` | `password123` | `ROLE_STUDENT` | `STU001` (Aarav Sharma, CGPA: 9.42) |
| **Recruiter** | `recruiter1` | `password123` | `ROLE_RECRUITER` | `C001` (Microsoft IDC Campus Team) |
| **Admin** | `admin` | `admin123` | `ROLE_ADMIN` | `ADMIN` (University Placement Cell) |

The UI includes **1-click login buttons** on `/login` to switch between personas instantly during demonstrations.
