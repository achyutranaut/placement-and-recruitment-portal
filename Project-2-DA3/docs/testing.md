# Testing Strategy & Automated Test Suite

This document details the testing framework, automated test execution, and verification procedures for the **Student Placement and Recruitment Portal**.

---

## 1. Automated Test Suite Overview

The backend is tested using **JUnit 5**, **Mockito**, and **Spring Boot Test / MockMvc**. The tests execute against an in-memory Oracle-mode H2 database, ensuring fast, deterministic, zero-network-dependent verification.

### Test Matrix

| Test Class | Scope | Tested Functionality |
|------------|-------|----------------------|
| `StudentPlacementPortalApplicationTests` | Integration | Spring context initialization & bean wiring |
| `StudentRepositoryTest` | Persistence | Data JPA CRUD, city search queries, constraints |
| `ApplicationRepositoryTest` | Persistence | Status filters, composite date keys, foreign keys |
| `AuthServiceTest` | Unit | JWT token generation, password encoding, bad credential guards |
| `ApplicationServiceTest` | Unit / Logic | Business rules, eligibility checks, duplicate application prevention |
| `AuthControllerTest` | Controller / Web | `POST /api/v1/auth/login`, `POST /api/v1/auth/register`, DTO validation |
| `StudentControllerTest` | Controller / Web | `GET /api/v1/students`, `GET /api/v1/students/{id}`, Role security checks |

---

## 2. Running Automated Tests

### Run Backend Tests (Offline Mode)

```bash
cd backend
export JAVA_HOME=/opt/homebrew/opt/openjdk
mvn clean test -o
```

**Verified Test Output**:
```
[INFO] Running com.placement.portal.StudentPlacementPortalApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.492 s
[INFO] Running com.placement.portal.student.StudentRepositoryTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.176 s
[INFO] Running com.placement.portal.student.StudentControllerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.286 s
[INFO] Running com.placement.portal.application.ApplicationServiceTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.009 s
[INFO] Running com.placement.portal.application.ApplicationRepositoryTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.083 s
[INFO] Running com.placement.portal.auth.AuthServiceTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.021 s
[INFO] Running com.placement.portal.auth.AuthControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.108 s
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

---

## 3. JDK 21/26 Bytecode Instrumentation Note

On modern JDK versions (JDK 21+ and JDK 26 Early Access), the Java Virtual Machine issues warnings or restricts dynamic agent attachment. This is resolved by:
1. Configuring `<argLine>-XX:+EnableDynamicAgentLoading</argLine>` in `pom.xml` Surefire plugin.
2. Configuring `mock-maker-subclass` in `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` to utilize subclass-based mock generation rather than unsafe agent instrumentation.

---

## 4. Frontend Build Verification

The React frontend build is verified with:

```bash
cd frontend
npm run build
```

Expected output:
```
vite v8.1.5 building client environment for production...
transforming...✓ 1689 modules transformed.
rendering chunks...
dist/index.html                   0.47 kB
dist/assets/index-BRn2x9HJ.css   39.41 kB
dist/assets/index-vmX75g3e.js   334.70 kB
✓ built in 382ms
```
