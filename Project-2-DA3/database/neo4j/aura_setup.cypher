// ==============================================================================
// STUDENT PLACEMENT & RECRUITMENT PORTAL
// NEO4J AURA GRAPH SCHEMA CONSTRAINTS & DEMONSTRATION CYPHER SCRIPT
// Coexisting with Oracle Database 23ai (OLTP Authoritative Source of Truth)
// ==============================================================================

// ------------------------------------------------------------------------------
// PART 1: UNIQUENESS CONSTRAINTS & INDEXES
// Execute these statements in Neo4j Aura Browser / Cypher Editor to enforce
// entity integrity on projected nodes. All statements are non-destructive (IF NOT EXISTS).
// ------------------------------------------------------------------------------

CREATE CONSTRAINT program_id_unique IF NOT EXISTS
FOR (p:Program) REQUIRE p.programId IS UNIQUE;

CREATE CONSTRAINT student_id_unique IF NOT EXISTS
FOR (s:Student) REQUIRE s.studentId IS UNIQUE;

CREATE CONSTRAINT company_id_unique IF NOT EXISTS
FOR (c:Company) REQUIRE c.companyId IS UNIQUE;

CREATE CONSTRAINT drive_id_unique IF NOT EXISTS
FOR (d:Drive) REQUIRE d.driveId IS UNIQUE;

CREATE CONSTRAINT app_id_unique IF NOT EXISTS
FOR (a:Application) REQUIRE a.applicationId IS UNIQUE;

CREATE CONSTRAINT interview_id_unique IF NOT EXISTS
FOR (i:Interview) REQUIRE i.interviewId IS UNIQUE;

CREATE CONSTRAINT offer_id_unique IF NOT EXISTS
FOR (o:Offer) REQUIRE o.offerId IS UNIQUE;

// Performance indexes for frequent traversal filters
CREATE INDEX student_cgpa_idx IF NOT EXISTS
FOR (s:Student) ON (s.cgpa);

CREATE INDEX drive_ctc_idx IF NOT EXISTS
FOR (d:Drive) ON (d.ctcLpa);

// ------------------------------------------------------------------------------
// PART 2: GRAPH SCHEMA VERIFICATION QUERIES
// Verify connectivity and schema structure in Neo4j Aura
// ------------------------------------------------------------------------------

// 2.1 Inspect all active node labels
CALL db.labels() YIELD label
RETURN label
ORDER BY label;

// 2.2 Inspect all active relationship types
CALL db.relationshipTypes() YIELD relationshipType
RETURN relationshipType
ORDER BY relationshipType;

// 2.3 Inspect active constraints
SHOW CONSTRAINTS;

// 2.4 Overall graph population summary
MATCH (n)
RETURN labels(n) AS Label, count(n) AS NodeCount
ORDER BY NodeCount DESC;

// ------------------------------------------------------------------------------
// PART 3: PLACEMENT WORKFLOW & MULTI-HOP GRAPH QUERIES
// These demonstrate relationships projected from Oracle into Aura
// ------------------------------------------------------------------------------

// 3.1 End-to-End Placement Journey for a Student
// Traverses Student -> Application -> Drive -> Company -> Interview -> Offer
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
       o.status AS OfferStatus
ORDER BY s.studentId, d.ctcLpa DESC
LIMIT 25;

// 3.2 Automated Eligibility Match (Graph Traversal Rule)
// Finds active drives where student's CGPA satisfies the drive's cutoff and program requirement
MATCH (s:Student)-[:ENROLLED_IN]->(p:Program)<-[:REQUIRES_PROGRAM]-(d:Drive)<-[:HOSTS_DRIVE]-(c:Company)
WHERE s.cgpa >= d.minCgpa
RETURN s.studentId AS StudentID,
       s.name AS StudentName,
       s.cgpa AS StudentCGPA,
       p.programName AS DegreeProgram,
       c.companyName AS EligibleCompany,
       d.jobTitle AS Position,
       d.minCgpa AS CutoffCGPA,
       d.ctcLpa AS Package_LPA
ORDER BY d.ctcLpa DESC, s.cgpa DESC
LIMIT 25;

// 3.3 Placement Conversion Funnel per Company Drive
// Analyzes student progression through Applications, Interviews, and Offers
MATCH (c:Company)-[:HOSTS_DRIVE]->(d:Drive)
OPTIONAL MATCH (sApp:Student)-[:SUBMITTED_APPLICATION]->(a:Application)-[:FOR_DRIVE]->(d)
OPTIONAL MATCH (a)-[:HAS_INTERVIEW]->(iv:Interview)
OPTIONAL MATCH (a)-[:RECEIVED_OFFER]->(o:Offer)
RETURN c.companyName AS Company,
       d.jobTitle AS JobTitle,
       d.ctcLpa AS CTC_LPA,
       count(DISTINCT sApp) AS TotalApplicants,
       count(DISTINCT iv) AS TotalInterviews,
       count(DISTINCT o) AS TotalOffersAwarded
ORDER BY TotalOffersAwarded DESC, TotalApplicants DESC;

// 3.4 Academic Program Placement Statistics
// Aggregates offers across academic degree programs
MATCH (s:Student)-[:ENROLLED_IN]->(p:Program)
MATCH (s)-[:SUBMITTED_APPLICATION]->(a:Application)-[:RECEIVED_OFFER]->(o:Offer)
RETURN p.programName AS Program,
       p.department AS Department,
       count(DISTINCT s) AS PlacedStudents,
       round(avg(o.ctcLpa), 2) AS AveragePackage_LPA,
       max(o.ctcLpa) AS HighestPackage_LPA
ORDER BY AveragePackage_LPA DESC;

// 3.5 Full Visual Subgraph for Interactive Graph View
// Returns top 50 related elements for browser rendering
MATCH (s:Student)-[r1:SUBMITTED_APPLICATION]->(a:Application)-[r2:FOR_DRIVE]->(d:Drive)<-[r3:HOSTS_DRIVE]-(c:Company)
RETURN s, r1, a, r2, d, r3, c
LIMIT 50;
