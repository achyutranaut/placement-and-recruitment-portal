# REST API Specification

The **Student Placement and Recruitment Portal** exposes a RESTful API built on Spring Boot 3.4.3, secured with JWT Bearer tokens and documented via OpenAPI 3.0 / Swagger.

---

## 1. Global Standards & Envelope

All API endpoints return responses encapsulated in a standardized `ApiResponse<T>` envelope:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-09-11T20:30:00Z"
}
```

On error:
```json
{
  "success": false,
  "message": "[ORA-20002] Ineligible: Candidate CGPA is below minimum requirement",
  "data": null,
  "timestamp": "2026-09-11T20:30:00Z"
}
```

### Authentication Header
Protected endpoints require an HTTP Bearer authorization token:
```http
Authorization: Bearer <jwt-token>
```

---

## 2. Authentication API (`/api/v1/auth`)

### `POST /api/v1/auth/login`
Authenticates user credentials and returns a signed JWT token containing user identity and role.

- **Request Body**:
```json
{
  "username": "student1",
  "password": "password123"
}
```
- **Response `200 OK`**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "student1",
    "role": "ROLE_STUDENT",
    "referenceId": "STU001"
  }
}
```

---

## 3. Placement Drives API (`/api/v1/drives`)

### `GET /api/v1/drives`
Retrieves all active and upcoming campus placement drives.

- **Response `200 OK`**:
```json
{
  "success": true,
  "data": [
    {
      "driveId": "DRV001",
      "jobTitle": "Software Development Engineer - I",
      "companyId": "C001",
      "companyName": "Microsoft IDC",
      "minCgpa": 8.50,
      "packageLpa": 44.0
    }
  ]
}
```

---

## 4. Applications API (`/api/v1/applications`)

### `POST /api/v1/applications`
Applies for a placement drive. Invokes the native Oracle PL/SQL stored procedure `APPLY_FOR_DRIVE`.

- **Role Required**: `ROLE_STUDENT`, `ROLE_ADMIN`
- **Request Body**:
```json
{
  "studentId": "STU001",
  "driveId": "DRV001",
  "applyDate": "2026-09-11"
}
```
- **Response `200 OK`**:
```json
{
  "success": true,
  "message": "Applied successfully via PL/SQL procedure",
  "data": {
    "applicationId": "APP007",
    "studentId": "STU001",
    "driveId": "DRV001",
    "applyDate": "2026-09-11",
    "status": "APPLIED"
  }
}
```

### `GET /api/v1/applications/student/{studentId}`
Lists all applications submitted by a specific student.

### `PATCH /api/v1/applications/{id}/status`
Updates candidate application status (`SHORTLISTED`, `INTERVIEWING`, `SELECTED`, `REJECTED`). Logs transition to `APPLICATION_AUDIT`.

---

## 5. Interviews API (`/api/v1/interviews`)

### `POST /api/v1/interviews/schedule`
Schedules an interview round. Invokes the native Oracle PL/SQL stored procedure `SCHEDULE_INTERVIEW`.

- **Role Required**: `ROLE_RECRUITER`, `ROLE_ADMIN`
- **Request Body**:
```json
{
  "applicationId": "APP002",
  "interviewerName": "Dr. Rajesh Sharma",
  "oa": "Y",
  "gd": "N",
  "hr": "N",
  "result": "PENDING",
  "online": "Y",
  "offline": "N"
}
```

### `PATCH /api/v1/interviews/{applicationId}/{interviewerName}/result`
Records evaluation scores (OA, GD, HR) and round outcome (`PASSED`, `FAILED`).

---

## 6. Offers API (`/api/v1/offers`)

### `POST /api/v1/offers`
Issues a formal employment offer. Invokes the native Oracle PL/SQL stored procedure `ISSUE_OFFER`.
*Trigger `TRG_OFFER_APPLICATION_STATUS` automatically updates `APPLICATION.Status = 'SELECTED'`.*

- **Role Required**: `ROLE_RECRUITER`, `ROLE_ADMIN`
- **Request Body**:
```json
{
  "applicationId": "APP002",
  "ctcLpa": 36.5,
  "offerDate": "2026-09-11"
}
```
- **Response `200 OK`**:
```json
{
  "success": true,
  "message": "Offer issued successfully via PL/SQL procedure",
  "data": {
    "offerId": "OFF004",
    "applicationId": "APP002",
    "ctcLpa": 36.5,
    "offerDate": "2026-09-11"
  }
}
```

---

## 7. Reports & Analytics API (`/api/v1/reports`)

### `GET /api/v1/reports/summary`
Calculates campus-wide placement metrics using Oracle PL/SQL functions `GET_PLACEMENT_RATE` and `GET_AVERAGE_PACKAGE`.

- **Response `200 OK`**:
```json
{
  "success": true,
  "data": {
    "totalStudents": 22,
    "placedStudents": 14,
    "placementRate": 63.6,
    "averageCtcLpa": 18.25,
    "highestCtcLpa": 44.0,
    "totalOffers": 14,
    "totalCompanies": 10,
    "totalDrives": 8
  }
}
```

---

## 8. Interactive Documentation (Swagger UI)

When the backend application is running, visit:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
