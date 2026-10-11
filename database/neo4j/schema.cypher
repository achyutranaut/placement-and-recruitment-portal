// ============================================================
// Neo4j 5.x Schema Definition for VIT Placement & Recruitment Portal
// Versioned & Idempotent Schema Script
// ============================================================

// 1. UNIQUE NODE CONSTRAINTS (Natural Keys)
CREATE CONSTRAINT constraint_student_id IF NOT EXISTS
FOR (s:Student) REQUIRE s.studentId IS UNIQUE;

CREATE CONSTRAINT constraint_student_regno IF NOT EXISTS
FOR (s:Student) REQUIRE s.registrationNo IS UNIQUE;

CREATE CONSTRAINT constraint_program_id IF NOT EXISTS
FOR (p:Program) REQUIRE p.programId IS UNIQUE;

CREATE CONSTRAINT constraint_batch_id IF NOT EXISTS
FOR (b:Batch) REQUIRE b.batchId IS UNIQUE;

CREATE CONSTRAINT constraint_company_id IF NOT EXISTS
FOR (c:Company) REQUIRE c.companyId IS UNIQUE;

CREATE CONSTRAINT constraint_job_title IF NOT EXISTS
FOR (j:JobPosting) REQUIRE j.jobTitle IS UNIQUE;

CREATE CONSTRAINT constraint_drive_id IF NOT EXISTS
FOR (d:Drive) REQUIRE d.driveId IS UNIQUE;

CREATE CONSTRAINT constraint_application_id IF NOT EXISTS
FOR (a:Application) REQUIRE a.applicationId IS UNIQUE;

CREATE CONSTRAINT constraint_interviewer_name IF NOT EXISTS
FOR (i:Interviewer) REQUIRE i.interviewerName IS UNIQUE;

CREATE CONSTRAINT constraint_interview_id IF NOT EXISTS
FOR (iv:Interview) REQUIRE iv.interviewId IS UNIQUE;

CREATE CONSTRAINT constraint_offer_id IF NOT EXISTS
FOR (o:Offer) REQUIRE o.offerId IS UNIQUE;

CREATE CONSTRAINT constraint_resume_id IF NOT EXISTS
FOR (r:Resume) REQUIRE r.resumeId IS UNIQUE;

CREATE CONSTRAINT constraint_evaluation_id IF NOT EXISTS
FOR (e:ResumeEvaluation) REQUIRE e.evaluationId IS UNIQUE;

CREATE CONSTRAINT constraint_audit_id IF NOT EXISTS
FOR (ev:AuditEvent) REQUIRE ev.auditId IS UNIQUE;

CREATE CONSTRAINT constraint_user_id IF NOT EXISTS
FOR (u:PortalUser) REQUIRE u.userId IS UNIQUE;

CREATE CONSTRAINT constraint_user_username IF NOT EXISTS
FOR (u:PortalUser) REQUIRE u.username IS UNIQUE;

CREATE CONSTRAINT constraint_assessment_id IF NOT EXISTS
FOR (asm:Assessment) REQUIRE asm.assessmentId IS UNIQUE;

// 2. PROPERTY EXISTENCE CONSTRAINTS (Enterprise Edition / APOC compatible)
// Note: Handled by application validation layer for Community Edition compatibility

// 3. FREQUENTLY FILTERED PROPERTY INDEXES
CREATE INDEX idx_student_cgpa IF NOT EXISTS
FOR (s:Student) ON (s.cgpa);

CREATE INDEX idx_student_placement_status IF NOT EXISTS
FOR (s:Student) ON (s.placementStatus);

CREATE INDEX idx_drive_min_cgpa IF NOT EXISTS
FOR (d:Drive) ON (d.minCgpa);

CREATE INDEX idx_drive_deadline IF NOT EXISTS
FOR (d:Drive) ON (d.applicationDeadline);

CREATE INDEX idx_drive_date IF NOT EXISTS
FOR (d:Drive) ON (d.driveDate);

CREATE INDEX idx_application_status IF NOT EXISTS
FOR (a:Application) ON (a.status);

CREATE INDEX idx_application_apply_date IF NOT EXISTS
FOR (a:Application) ON (a.applyDate);

CREATE INDEX idx_offer_status IF NOT EXISTS
FOR (o:Offer) ON (o.status);

CREATE INDEX idx_resume_is_current IF NOT EXISTS
FOR (r:Resume) ON (r.isCurrent);
