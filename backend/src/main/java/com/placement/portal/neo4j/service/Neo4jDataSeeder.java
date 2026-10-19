package com.placement.portal.neo4j.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
@Profile({"neo4j", "neo4j-seed"})
public class Neo4jDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Neo4jDataSeeder.class);

    private final Driver driver;
    private final ObjectMapper objectMapper;

    public Neo4jDataSeeder(Driver driver, ObjectMapper objectMapper) {
        this.driver = driver;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking Neo4j dataset initialization status...");

        try (Session session = driver.session()) {
            var res = session.run("MATCH (n:Program) RETURN count(n) AS cnt");
            long programCount = res.hasNext() ? res.next().get("cnt").asLong() : 0;

            if (programCount > 0) {
                log.info("Neo4j database already initialized ({} program nodes found). Skipping initial seed migration.", programCount);
                printReconciliationReport(session);
                return;
            }

            log.info("Seeding Neo4j database from canonical dataset (neo4j-seed-data.json)...");
            ClassPathResource resource = new ClassPathResource("neo4j-seed-data.json");
            if (!resource.exists()) {
                log.warn("neo4j-seed-data.json not found in classpath. Skipping seed.");
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);

                // 1. Programs
                JsonNode programs = root.path("programs");
                for (JsonNode p : programs) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("id", p.path("id").asText());
                    params.put("name", p.path("name").asText());
                    params.put("fee", p.path("fee").asDouble());
                    params.put("duration", p.path("duration").asText());
                    params.put("level", p.path("level").asText());
                    params.put("dept", p.path("dept").asText());
                    params.put("spec", p.path("spec").asText());
                    params.put("regex", p.path("pattern").asText());
                    params.put("sample", p.path("example").asText());
                    params.put("active", p.path("active").asText("Y"));

                    session.run("""
                        MERGE (pr:Program {programId: $id})
                        SET pr.programName = $name,
                            pr.fee = $fee,
                            pr.duration = $duration,
                            pr.degreeLevel = $level,
                            pr.department = $dept,
                            pr.specialization = $spec,
                            pr.regNoRegex = $regex,
                            pr.regNoSample = $sample,
                            pr.isOffered = $active
                    """, params);
                }

                // 2. Companies
                JsonNode companies = root.path("companies");
                for (JsonNode c : companies) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("id", c.path("id").asText());
                    params.put("name", c.path("name").asText());
                    params.put("email", c.path("email").asText());
                    params.put("industry", c.path("industry").asText());

                    session.run("""
                        MERGE (co:Company {companyId: $id})
                        SET co.companyName = $name,
                            co.email = $email,
                            co.industry = $industry
                    """, params);
                }

                // 3. Batches
                JsonNode batches = root.path("batches");
                for (JsonNode b : batches) {
                    String progId = b.path("progId").asText();
                    String batchNo = b.path("batchNo").asText();
                    String batchId = progId + "_" + batchNo;
                    Map<String, Object> params = new HashMap<>();
                    params.put("batchId", batchId);
                    params.put("progId", progId);
                    params.put("batchNo", batchNo);
                    params.put("sched", b.path("sched").asText());
                    params.put("room", b.path("room").asText());
                    params.put("start", b.path("start").asText());
                    params.put("end", b.path("end").asText());

                    session.run("""
                        MERGE (bt:Batch {batchId: $batchId})
                        SET bt.programId = $progId,
                            bt.batchNo = $batchNo,
                            bt.schedule = $sched,
                            bt.room = $room,
                            bt.startDate = date($start),
                            bt.endDate = date($end)
                        WITH bt
                        MATCH (pr:Program {programId: $progId})
                        MERGE (bt)-[:OF_PROGRAM]->(pr)
                    """, params);
                }

                // 4. Interviewers
                JsonNode interviewers = root.path("interviewers");
                for (JsonNode iv : interviewers) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("name", iv.path("name").asText());
                    params.put("roundNo", iv.path("roundNo").asInt());

                    session.run("""
                        MERGE (i:Interviewer {interviewerName: $name})
                        SET i.interviewRoundNo = $roundNo
                    """, params);
                }

                // 5. Drives
                JsonNode drives = root.path("drives");
                for (JsonNode d : drives) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("id", d.path("id").asText());
                    params.put("title", d.path("title").asText());
                    params.put("companyId", d.path("companyId").asText());
                    params.put("minCgpa", d.path("minCgpa").asDouble());
                    params.put("driveDate", d.path("driveDate").asText());
                    params.put("deadline", d.path("deadline").asText());
                    params.put("openings", d.path("openings").asInt());
                    params.put("ctc", d.path("ctc").asDouble());
                    params.put("loc", d.path("location").asText());
                    params.put("branches", d.path("branches").asText());
                    params.put("process", d.path("process").asText());

                    session.run("""
                        MERGE (dr:Drive {driveId: $id})
                        SET dr.jobTitle = $title,
                            dr.companyId = $companyId,
                            dr.minCgpa = $minCgpa,
                            dr.driveDate = date($driveDate),
                            dr.applicationDeadline = date($deadline),
                            dr.openings = $openings,
                            dr.ctc = $ctc,
                            dr.location = $loc,
                            dr.eligibleBranches = $branches,
                            dr.selectionProcess = $process
                        WITH dr
                        MATCH (co:Company {companyId: $companyId})
                        MERGE (co)-[:HOSTS_DRIVE]->(dr)
                    """, params);
                }

                // 6. Drive Eligibility (Drive -> Program)
                JsonNode eligibilities = root.path("eligibilities");
                for (JsonNode el : eligibilities) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("driveId", el.path("driveId").asText());
                    params.put("progId", el.path("progId").asText());

                    session.run("""
                        MATCH (dr:Drive {driveId: $driveId})
                        MATCH (pr:Program {programId: $progId})
                        MERGE (dr)-[:ELIGIBLE_FOR]->(pr)
                    """, params);
                }

                // 7. Students
                JsonNode students = root.path("students");
                for (JsonNode s : students) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("id", s.path("id").asText());
                    params.put("name", s.path("name").asText());
                    params.put("email", s.path("email").asText());
                    params.put("street", s.path("street").asText());
                    params.put("city", s.path("city").asText());
                    params.put("state", s.path("state").asText());
                    params.put("dob", s.path("dob").asText());
                    params.put("cgpa", s.path("cgpa").asDouble());
                    params.put("branch", s.path("branch").asText());
                    params.put("regNo", s.path("regNo").asText());
                    params.put("progId", s.path("progId").asText());
                    params.put("status", s.path("status").asText("NOT_PLACED"));

                    session.run("""
                        MERGE (st:Student {studentId: $id})
                        SET st.name = $name,
                            st.email = $email,
                            st.street = $street,
                            st.city = $city,
                            st.state = $state,
                            st.dob = date($dob),
                            st.cgpa = $cgpa,
                            st.branch = $branch,
                            st.registrationNo = $regNo,
                            st.programId = $progId,
                            st.placementStatus = $status
                        WITH st
                        MATCH (pr:Program {programId: $progId})
                        MERGE (st)-[:ENROLLED_IN]->(pr)
                    """, params);
                }

                // Connect students to batches
                session.run("""
                    MATCH (st:Student)
                    MATCH (bt:Batch)
                    WHERE bt.batchNo = 'BAT001' AND st.studentId IN ['STU023', 'STU002']
                    MERGE (st)-[:ATTENDS]->(bt)
                """);

                // 8. Portal Users
                JsonNode users = root.path("users");
                for (JsonNode u : users) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("id", u.path("id").asText());
                    params.put("username", u.path("username").asText());
                    params.put("hash", u.path("hash").asText());
                    params.put("role", u.path("role").asText());
                    params.put("refId", u.path("refId").asText());
                    params.put("fullName", u.path("fullName").asText());
                    params.put("email", u.path("email").asText());

                    session.run("""
                        MERGE (usr:PortalUser {userId: $id})
                        SET usr.username = $username,
                            usr.passwordHash = $hash,
                            usr.role = $role,
                            usr.referenceId = $refId,
                            usr.fullName = $fullName,
                            usr.email = $email
                    """, params);
                }

                // 9. Recruiter-Company Authorizations
                JsonNode recruiterCompanies = root.path("recruiterCompanies");
                for (JsonNode rc : recruiterCompanies) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("userId", rc.path("userId").asText());
                    params.put("companyId", rc.path("companyId").asText());

                    session.run("""
                        MATCH (usr:PortalUser {userId: $userId})
                        MATCH (co:Company {companyId: $companyId})
                        MERGE (usr)-[:AUTHORIZED_FOR]->(co)
                    """, params);
                }

                // 10. Resumes
                JsonNode resumes = root.path("resumes");
                for (JsonNode r : resumes) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("id", r.path("id").asText());
                    params.put("studentId", r.path("studentId").asText());
                    params.put("fileName", r.path("fileName").asText());
                    params.put("contentType", r.path("contentType").asText());
                    params.put("fileSize", r.path("fileSize").asLong());
                    params.put("versionNo", r.path("versionNo").asInt());
                    params.put("isCurrent", r.path("isCurrent").asText());
                    params.put("filePath", "./data/resumes/" + r.path("id").asText() + ".pdf");
                    params.put("hash", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

                    session.run("""
                        MERGE (rm:Resume {resumeId: $id})
                        SET rm.studentId = $studentId,
                            rm.fileName = $fileName,
                            rm.contentType = $contentType,
                            rm.fileSize = $fileSize,
                            rm.versionNo = $versionNo,
                            rm.isCurrent = $isCurrent,
                            rm.filePath = $filePath,
                            rm.sha256Hash = $hash,
                            rm.uploadedAt = datetime()
                        WITH rm
                        MATCH (st:Student {studentId: $studentId})
                        MERGE (st)-[:HAS_RESUME]->(rm)
                    """, params);
                }

                // 11. Applications
                JsonNode applications = root.path("applications");
                for (JsonNode a : applications) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("id", a.path("id").asText());
                    params.put("studentId", a.path("studentId").asText());
                    params.put("driveId", a.path("driveId").asText());
                    params.put("applyDate", a.path("applyDate").asText());
                    params.put("status", a.path("status").asText());

                    session.run("""
                        MERGE (app:Application {applicationId: $id})
                        SET app.studentId = $studentId,
                            app.driveId = $driveId,
                            app.applyDate = date($applyDate),
                            app.status = $status
                        WITH app
                        MATCH (st:Student {studentId: $studentId})
                        MATCH (dr:Drive {driveId: $driveId})
                        MERGE (st)-[:SUBMITTED_APPLICATION]->(app)
                        MERGE (app)-[:FOR_DRIVE]->(dr)
                    """, params);
                }

                // 12. Interviews
                JsonNode interviews = root.path("interviews");
                for (JsonNode iv : interviews) {
                    String appId = iv.path("appId").asText();
                    String interviewer = iv.path("name").asText();
                    String interviewId = appId + "_" + interviewer;

                    Map<String, Object> params = new HashMap<>();
                    params.put("id", interviewId);
                    params.put("appId", appId);
                    params.put("interviewer", interviewer);
                    params.put("oa", iv.path("oa").asText().isEmpty() ? null : Double.parseDouble(iv.path("oa").asText()));
                    params.put("gd", iv.path("gd").asText().isEmpty() ? null : Double.parseDouble(iv.path("gd").asText()));
                    params.put("hr", iv.path("hr").asText().isEmpty() ? null : Double.parseDouble(iv.path("hr").asText()));
                    params.put("result", iv.path("result").asText());
                    params.put("online", iv.path("online").asText());
                    params.put("offline", iv.path("offline").asText());

                    session.run("""
                        MERGE (it:Interview {interviewId: $id})
                        SET it.applicationId = $appId,
                            it.interviewerName = $interviewer,
                            it.oa = $oa,
                            it.gd = $gd,
                            it.hr = $hr,
                            it.result = $result,
                            it.online = $online,
                            it.offline = $offline
                        WITH it
                        MATCH (app:Application {applicationId: $appId})
                        MATCH (ivr:Interviewer {interviewerName: $interviewer})
                        MERGE (app)-[:HAS_INTERVIEW]->(it)
                        MERGE (it)-[:CONDUCTED_BY]->(ivr)
                    """, params);
                }

                // 13. Offers
                JsonNode offers = root.path("offers");
                for (JsonNode o : offers) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("id", o.path("id").asText());
                    params.put("appId", o.path("appId").asText());
                    params.put("date", o.path("date").asText());
                    params.put("ctc", o.path("ctc").asDouble());
                    params.put("status", o.path("status").asText());

                    session.run("""
                        MERGE (of:Offer {offerId: $id})
                        SET of.applicationId = $appId,
                            of.offerDate = date($date),
                            of.ctcLpa = $ctc,
                            of.status = $status
                        WITH of
                        MATCH (app:Application {applicationId: $appId})
                        MERGE (app)-[:RECEIVED_OFFER]->(of)
                    """, params);
                }

                // 14. Audit Events
                session.run("""
                    MERGE (ae1:AuditEvent {auditId: 'AUD_APP036_1'})
                    SET ae1.applicationId = 'APP036',
                        ae1.oldStatus = 'SELECTED',
                        ae1.newStatus = 'REJECTED',
                        ae1.changedAt = datetime(),
                        ae1.changedBy = 'RECRUITER'
                    WITH ae1
                    MATCH (app:Application {applicationId: 'APP036'})
                    MERGE (app)-[:HAS_AUDIT]->(ae1)
                """);
                session.run("""
                    MERGE (ae2:AuditEvent {auditId: 'AUD_APP038_1'})
                    SET ae2.applicationId = 'APP038',
                        ae2.oldStatus = 'OFFERED',
                        ae2.newStatus = 'ACCEPTED',
                        ae2.changedAt = datetime(),
                        ae2.changedBy = 'STUDENT'
                    WITH ae2
                    MATCH (app:Application {applicationId: 'APP038'})
                    MERGE (app)-[:HAS_AUDIT]->(ae2)
                """);
            }

            log.info("Neo4j database successfully seeded.");
            printReconciliationReport(session);
        }
    }

    private void printReconciliationReport(Session session) {
        log.info("===============================================================================");
        log.info("        NEO4J VS ORACLE DATABASE RECONCILIATION AUDIT REPORT                  ");
        log.info("===============================================================================");
        logEntityCount(session, "Program", "Node:Program");
        logEntityCount(session, "Company", "Node:Company");
        logEntityCount(session, "Drive", "Node:Drive");
        logEntityCount(session, "Batch", "Node:Batch");
        logEntityCount(session, "Student", "Node:Student");
        logEntityCount(session, "PortalUser", "Node:PortalUser");
        logEntityCount(session, "Application", "Node:Application");
        logEntityCount(session, "Interview", "Node:Interview");
        logEntityCount(session, "Offer", "Node:Offer");
        logEntityCount(session, "Resume", "Node:Resume");
        log.info("-------------------------------------------------------------------------------");
        logRelCount(session, "HOSTS_DRIVE", "[:HOSTS_DRIVE]");
        logRelCount(session, "ELIGIBLE_FOR", "[:ELIGIBLE_FOR]");
        logRelCount(session, "SUBMITTED_APPLICATION", "[:SUBMITTED_APPLICATION]");
        logRelCount(session, "FOR_DRIVE", "[:FOR_DRIVE]");
        logRelCount(session, "HAS_INTERVIEW", "[:HAS_INTERVIEW]");
        logRelCount(session, "CONDUCTED_BY", "[:CONDUCTED_BY]");
        logRelCount(session, "RECEIVED_OFFER", "[:RECEIVED_OFFER]");
        logRelCount(session, "AUTHORIZED_FOR", "[:AUTHORIZED_FOR]");
        log.info("===============================================================================");
    }

    private void logEntityCount(Session session, String label, String description) {
        try {
            var res = session.run("MATCH (n:" + label + ") RETURN count(n) AS cnt");
            long cnt = res.hasNext() ? res.next().get("cnt").asLong() : 0;
            log.info("  {}: {} nodes", String.format("%-25s", description), cnt);
        } catch (Exception e) {
            log.warn("  Could not count {}: {}", label, e.getMessage());
        }
    }

    private void logRelCount(Session session, String relType, String description) {
        try {
            var res = session.run("MATCH ()-[r:" + relType + "]->() RETURN count(r) AS cnt");
            long cnt = res.hasNext() ? res.next().get("cnt").asLong() : 0;
            log.info("  {}: {} edges", String.format("%-25s", description), cnt);
        } catch (Exception e) {
            log.warn("  Could not count relationship {}: {}", relType, e.getMessage());
        }
    }
}
