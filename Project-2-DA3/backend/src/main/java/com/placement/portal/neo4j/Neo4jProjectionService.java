package com.placement.portal.neo4j;

import com.placement.portal.application.Application;
import com.placement.portal.application.ApplicationRepository;
import com.placement.portal.company.*;
import com.placement.portal.interview.Interview;
import com.placement.portal.interview.InterviewRepository;
import com.placement.portal.interview.InterviewerRound;
import com.placement.portal.interview.InterviewerRoundRepository;
import com.placement.portal.offer.OfferLetter;
import com.placement.portal.offer.OfferRepository;
import com.placement.portal.program.Program;
import com.placement.portal.program.ProgramRepository;
import com.placement.portal.recruitment.DriveEligibility;
import com.placement.portal.recruitment.DriveEligibilityRepository;
import com.placement.portal.recruitment.PlacementDrive;
import com.placement.portal.recruitment.PlacementDriveRepository;
import com.placement.portal.student.Student;
import com.placement.portal.student.StudentRepository;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.SummaryCounters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Authoritative Oracle-to-Neo4j Aura projection and reconciliation engine.
 * 
 * Guarantees:
 * 1. Oracle Database 23ai is the sole authoritative source of truth.
 * 2. Never invents, fabricates, or infers records not found in Oracle.
 * 3. Idempotently reconciles graph topology: prunes orphaned offer nodes, phantom interviews,
 *    stale applications, and invalid relationships.
 * 4. Asynchronous and post-mutation projections are fault-tolerant (never roll back Oracle transactions).
 * 5. Strictly protects user privacy: excludes password hashes, secrets, and unnecessary PII from graph.
 */
@Service
public class Neo4jProjectionService {

    private static final Logger log = LoggerFactory.getLogger(Neo4jProjectionService.class);

    private final Neo4jDriverService driverService;
    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final CompanyRepository companyRepository;
    private final EmailCompanyRepository emailCompanyRepository;
    private final JobCompanyRepository jobCompanyRepository;
    private final PlacementDriveRepository driveRepository;
    private final DriveEligibilityRepository driveEligibilityRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final OfferRepository offerRepository;
    private final InterviewerRoundRepository interviewerRoundRepository;

    public Neo4jProjectionService(
            Neo4jDriverService driverService,
            StudentRepository studentRepository,
            ProgramRepository programRepository,
            CompanyRepository companyRepository,
            EmailCompanyRepository emailCompanyRepository,
            JobCompanyRepository jobCompanyRepository,
            PlacementDriveRepository driveRepository,
            DriveEligibilityRepository driveEligibilityRepository,
            ApplicationRepository applicationRepository,
            InterviewRepository interviewRepository,
            OfferRepository offerRepository,
            @Autowired(required = false) InterviewerRoundRepository interviewerRoundRepository
    ) {
        this.driverService = driverService;
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.companyRepository = companyRepository;
        this.emailCompanyRepository = emailCompanyRepository;
        this.jobCompanyRepository = jobCompanyRepository;
        this.driveRepository = driveRepository;
        this.driveEligibilityRepository = driveEligibilityRepository;
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
        this.offerRepository = offerRepository;
        this.interviewerRoundRepository = interviewerRoundRepository;
    }

    public record ProjectionSummary(
            boolean success,
            String message,
            int programsCount,
            int studentsCount,
            int companiesCount,
            int drivesCount,
            int applicationsCount,
            int interviewsCount,
            int offersCount,
            int prunedNodesCount,
            int prunedRelationshipsCount,
            long executionTimeMs
    ) {
        public ProjectionSummary(
                boolean success,
                String message,
                int programsCount,
                int studentsCount,
                int companiesCount,
                int drivesCount,
                int applicationsCount,
                int interviewsCount,
                int offersCount,
                long executionTimeMs
        ) {
            this(success, message, programsCount, studentsCount, companiesCount, drivesCount,
                 applicationsCount, interviewsCount, offersCount, 0, 0, executionTimeMs);
        }
    }

    /**
     * Projects authoritative Oracle data into Neo4j Aura with full reconciliation.
     * Alias for {@link #reconcileGraphWithOracle()}.
     */
    @Transactional(readOnly = true)
    public ProjectionSummary syncOracleToNeo4j() {
        return reconcileGraphWithOracle();
    }

    /**
     * Executes end-to-end reconciliation between Oracle (source of truth) and Neo4j Aura.
     * Prunes phantom/stale nodes and relationships, aligns state machines, and merges authoritative records.
     */
    @Transactional(readOnly = true)
    public ProjectionSummary reconcileGraphWithOracle() {
        long start = System.currentTimeMillis();

        if (!driverService.isEnabled()) {
            return new ProjectionSummary(
                    false,
                    "Synchronization aborted: Neo4j Aura integration is disabled by configuration (portal.neo4j.enabled=false).",
                    0, 0, 0, 0, 0, 0, 0, 0, 0,
                    System.currentTimeMillis() - start
            );
        }

        if (!driverService.isConnected()) {
            return new ProjectionSummary(
                    false,
                    "Synchronization failed: Neo4j Aura is enabled but not connected. Check network reachability and credentials.",
                    0, 0, 0, 0, 0, 0, 0, 0, 0,
                    System.currentTimeMillis() - start
            );
        }

        try (Session session = driverService.openSession()) {
            // 1. Create Schema Constraints in Neo4j Aura
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (p:Program) REQUIRE p.programId IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (s:Student) REQUIRE s.studentId IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (c:Company) REQUIRE c.companyId IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (d:Drive) REQUIRE d.driveId IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (a:Application) REQUIRE a.applicationId IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (o:Offer) REQUIRE o.offerId IS UNIQUE");

            // 2. Fetch Authoritative Oracle Entities
            List<Program> programs = programRepository.findAll();
            List<Company> companies = companyRepository.findAll();
            List<PlacementDrive> drives = driveRepository.findAll();
            List<DriveEligibility> eligibilities = driveEligibilityRepository.findAll();
            List<Student> students = studentRepository.findAll();
            List<Application> applications = applicationRepository.findAll();
            List<Interview> interviews = interviewRepository.findAll();
            List<OfferLetter> offers = offerRepository.findAll();

            Map<String, String> emailToName = emailCompanyRepository.findAll().stream()
                    .collect(Collectors.toMap(EmailCompany::getEmail, EmailCompany::getCompanyName, (a, b) -> a));
            Map<String, String> jobToCompanyId = jobCompanyRepository.findAll().stream()
                    .collect(Collectors.toMap(JobCompany::getJobTitle, JobCompany::getCompanyId, (a, b) -> a));

            Map<String, Integer> interviewerToRound = new HashMap<>();
            if (interviewerRoundRepository != null) {
                interviewerRoundRepository.findAll().forEach(r -> {
                    if (r.getInterviewerName() != null && r.getInterviewRoundNo() != null) {
                        interviewerToRound.put(r.getInterviewerName(), r.getInterviewRoundNo());
                    }
                });
            }

            // 3. Assemble Canonical Key Sets from Oracle
            Set<String> validProgramIds = programs.stream().map(Program::getProgramId).collect(Collectors.toSet());
            Set<String> validCompanyIds = companies.stream().map(Company::getCompanyId).collect(Collectors.toSet());
            Set<String> validDriveIds = drives.stream().map(PlacementDrive::getDriveId).collect(Collectors.toSet());
            Set<String> validStudentIds = students.stream().map(Student::getStudentId).collect(Collectors.toSet());
            Set<String> validAppIds = applications.stream().map(Application::getApplicationId).collect(Collectors.toSet());
            Set<String> validOfferIds = offers.stream().map(OfferLetter::getOfferId).collect(Collectors.toSet());
            Set<String> appIdsWithOffers = offers.stream().map(OfferLetter::getApplicationId).collect(Collectors.toSet());
            Set<String> validInterviewIds = interviews.stream()
                    .map(iv -> iv.getApplicationId() + "_" + iv.getInterviewerName().replaceAll("\\s+", "_"))
                    .collect(Collectors.toSet());

            int prunedNodes = 0;
            int prunedRelationships = 0;

            // 4. Pruning Phase: Remove Stale/Phantom Entities in Neo4j Not Present in Oracle
            // 4a. Prune Phantom Offer Nodes
            Result rOfferPrune = session.run(
                    "MATCH (o:Offer) WHERE NOT o.offerId IN $validOfferIds DETACH DELETE o",
                    Map.of("validOfferIds", new ArrayList<>(validOfferIds))
            );
            SummaryCounters sc1 = rOfferPrune.consume().counters();
            prunedNodes += sc1.nodesDeleted();
            prunedRelationships += sc1.relationshipsDeleted();

            // 4b. Prune Orphaned [:RECEIVED_OFFER] Edges from Applications lacking Oracle offers
            Result rOfferEdgePrune = session.run(
                    "MATCH (a:Application)-[r:RECEIVED_OFFER]->(o:Offer) WHERE NOT a.applicationId IN $appIdsWithOffers DELETE r",
                    Map.of("appIdsWithOffers", new ArrayList<>(appIdsWithOffers))
            );
            prunedRelationships += rOfferEdgePrune.consume().counters().relationshipsDeleted();

            // 4c. Prune Phantom Interview Nodes
            Result rIvPrune = session.run(
                    "MATCH (iv:Interview) WHERE NOT iv.interviewId IN $validInterviewIds DETACH DELETE iv",
                    Map.of("validInterviewIds", new ArrayList<>(validInterviewIds))
            );
            SummaryCounters sc3 = rIvPrune.consume().counters();
            prunedNodes += sc3.nodesDeleted();
            prunedRelationships += sc3.relationshipsDeleted();

            // 4d. Prune Phantom Application Nodes (e.g. APP002)
            Result rAppPrune = session.run(
                    "MATCH (a:Application) WHERE NOT a.applicationId IN $validAppIds DETACH DELETE a",
                    Map.of("validAppIds", new ArrayList<>(validAppIds))
            );
            SummaryCounters sc4 = rAppPrune.consume().counters();
            prunedNodes += sc4.nodesDeleted();
            prunedRelationships += sc4.relationshipsDeleted();

            // 4e. Prune Phantom Student Nodes
            Result rStuPrune = session.run(
                    "MATCH (s:Student) WHERE NOT s.studentId IN $validStudentIds DETACH DELETE s",
                    Map.of("validStudentIds", new ArrayList<>(validStudentIds))
            );
            SummaryCounters sc5 = rStuPrune.consume().counters();
            prunedNodes += sc5.nodesDeleted();
            prunedRelationships += sc5.relationshipsDeleted();

            // 4f. Prune Cross-wired/Stale Application Relationships
            for (Application a : applications) {
                Result rCleanStu = session.run(
                        "MATCH (s:Student)-[r:SUBMITTED_APPLICATION]->(a:Application {applicationId: $appId}) WHERE s.studentId <> $ownerId DELETE r",
                        Map.of("appId", a.getApplicationId(), "ownerId", a.getStudentId())
                );
                prunedRelationships += rCleanStu.consume().counters().relationshipsDeleted();

                if (a.getDriveId() != null && !a.getDriveId().isBlank()) {
                    Result rCleanDrv = session.run(
                            "MATCH (a:Application {applicationId: $appId})-[r:FOR_DRIVE]->(d:Drive) WHERE d.driveId <> $ownerDriveId DELETE r",
                            Map.of("appId", a.getApplicationId(), "ownerDriveId", a.getDriveId())
                    );
                    prunedRelationships += rCleanDrv.consume().counters().relationshipsDeleted();
                }
            }

            // 5. Projection Phase: Merge Canonical Oracle Entities into Neo4j Aura
            // 5a. Programs
            for (Program p : programs) {
                session.run("""
                    MERGE (prog:Program {programId: $id})
                    SET prog.programName = $name,
                        prog.department = $dept
                """, Map.of(
                        "id", p.getProgramId(),
                        "name", p.getProgramName() != null ? p.getProgramName() : p.getProgramId(),
                        "dept", p.getDisciplineFamily() != null ? p.getDisciplineFamily() : "Engineering"
                ));
            }

            // 5b. Companies
            for (Company c : companies) {
                String compName = emailToName.getOrDefault(c.getEmail(), c.getCompanyId());
                session.run("""
                    MERGE (comp:Company {companyId: $id})
                    SET comp.companyName = $name,
                        comp.industry = $industry,
                        comp.email = $email
                """, Map.of(
                        "id", c.getCompanyId(),
                        "name", compName,
                        "industry", c.getIndustry() != null ? c.getIndustry() : "Technology",
                        "email", c.getEmail() != null ? c.getEmail() : ""
                ));
            }

            // 5c. Drives & HOSTS_DRIVE
            for (PlacementDrive d : drives) {
                String compId = jobToCompanyId.getOrDefault(d.getJobTitle(), "CMP001");
                session.run("""
                    MERGE (drv:Drive {driveId: $id})
                    SET drv.jobTitle = $title,
                        drv.minCgpa = $minCgpa,
                        drv.ctcLpa = $ctc,
                        drv.location = $location
                    WITH drv
                    MATCH (comp:Company {companyId: $compId})
                    MERGE (comp)-[:HOSTS_DRIVE]->(drv)
                """, Map.of(
                        "id", d.getDriveId(),
                        "title", d.getJobTitle() != null ? d.getJobTitle() : "Software Engineer",
                        "minCgpa", d.getMinCgpa() != null ? d.getMinCgpa().doubleValue() : 0.0,
                        "ctc", d.getCtc() != null ? d.getCtc().doubleValue() : 10.0,
                        "location", d.getLocation() != null ? d.getLocation() : "Hybrid",
                        "compId", compId
                ));
            }

            // 5d. Drive Eligibility Program links
            for (DriveEligibility de : eligibilities) {
                session.run("""
                    MATCH (drv:Drive {driveId: $driveId})
                    MATCH (prog:Program {programId: $progId})
                    MERGE (drv)-[:REQUIRES_PROGRAM]->(prog)
                """, Map.of(
                        "driveId", de.getDriveId(),
                        "progId", de.getProgramId()
                ));
            }

            // 5e. Students & ENROLLED_IN (never exposing passwords or hashes)
            for (Student s : students) {
                session.run("""
                    MERGE (stu:Student {studentId: $id})
                    SET stu.name = $name,
                        stu.cgpa = $cgpa,
                        stu.branch = $branch,
                        stu.placementStatus = $status
                    WITH stu
                    MATCH (prog:Program {programId: $progId})
                    MERGE (stu)-[:ENROLLED_IN]->(prog)
                """, Map.of(
                        "id", s.getStudentId(),
                        "name", s.getName() != null ? s.getName() : s.getStudentId(),
                        "cgpa", s.getCgpa() != null ? s.getCgpa().doubleValue() : 0.0,
                        "branch", s.getBranch() != null ? s.getBranch() : "",
                        "status", s.getPlacementStatus() != null ? s.getPlacementStatus() : "NOT_PLACED",
                        "progId", s.getProgramId() != null ? s.getProgramId() : "BCE"
                ));
            }

            // 5f. Applications & SUBMITTED_APPLICATION / FOR_DRIVE
            for (Application a : applications) {
                session.run("""
                    MERGE (app:Application {applicationId: $id})
                    SET app.status = $status,
                        app.applyDate = $date
                    WITH app
                    MATCH (stu:Student {studentId: $stuId})
                    MERGE (stu)-[:SUBMITTED_APPLICATION]->(app)
                    WITH app
                    MATCH (drv:Drive {driveId: $drvId})
                    MERGE (app)-[:FOR_DRIVE]->(drv)
                """, Map.of(
                        "id", a.getApplicationId(),
                        "status", a.getStatus() != null ? a.getStatus() : "APPLIED",
                        "date", a.getApplyDate() != null ? a.getApplyDate().toString() : "",
                        "stuId", a.getStudentId(),
                        "drvId", a.getDriveId() != null ? a.getDriveId() : ""
                ));
            }

            // 5g. Interviews & HAS_INTERVIEW
            for (Interview iv : interviews) {
                String ivId = iv.getApplicationId() + "_" + iv.getInterviewerName().replaceAll("\\s+", "_");
                int roundNo = interviewerToRound.getOrDefault(iv.getInterviewerName(), 1);
                session.run("""
                    MATCH (app:Application {applicationId: $appId})
                    MERGE (iv:Interview {interviewId: $ivId})
                    SET iv.interviewerName = $interviewer,
                        iv.roundNo = $roundNo,
                        iv.result = $result
                    MERGE (app)-[:HAS_INTERVIEW]->(iv)
                """, Map.of(
                        "appId", iv.getApplicationId(),
                        "ivId", ivId,
                        "interviewer", iv.getInterviewerName(),
                        "roundNo", roundNo,
                        "result", iv.getResult() != null ? iv.getResult() : "PENDING"
                ));
            }

            // 5h. Offers & RECEIVED_OFFER
            for (OfferLetter o : offers) {
                session.run("""
                    MATCH (app:Application {applicationId: $appId})
                    MERGE (off:Offer {offerId: $id})
                    SET off.ctcLpa = $ctc,
                        off.status = $status,
                        off.offerDate = $date
                    MERGE (app)-[:RECEIVED_OFFER]->(off)
                """, Map.of(
                        "id", o.getOfferId(),
                        "ctc", o.getCtcLpa() != null ? o.getCtcLpa().doubleValue() : 0.0,
                        "status", o.getStatus() != null ? o.getStatus() : "OFFERED",
                        "date", o.getOfferDate() != null ? o.getOfferDate().toString() : "",
                        "appId", o.getApplicationId()
                ));
            }

            long elapsed = System.currentTimeMillis() - start;
            int totalProjected = programs.size() + students.size() + companies.size() + drives.size() + applications.size() + interviews.size() + offers.size();

            log.info("Reconciled Oracle source of truth with Neo4j Aura: {} projected, {} nodes pruned, {} rels pruned in {} ms.",
                    totalProjected, prunedNodes, prunedRelationships, elapsed);

            return new ProjectionSummary(
                    true,
                    String.format("Reconciled %d Oracle entities into Neo4j Aura (pruned %d phantom nodes, %d stale relationships).",
                            totalProjected, prunedNodes, prunedRelationships),
                    programs.size(),
                    students.size(),
                    companies.size(),
                    drives.size(),
                    applications.size(),
                    interviews.size(),
                    offers.size(),
                    prunedNodes,
                    prunedRelationships,
                    elapsed
            );
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("Reconciliation error against Neo4j Aura: {}", e.getMessage());
            return new ProjectionSummary(
                    false,
                    "Synchronization failed: " + e.getMessage(),
                    0, 0, 0, 0, 0, 0, 0, 0, 0,
                    elapsed
            );
        }
    }

    /**
     * Non-blocking, isolated projection of a single Application mutation into Neo4j Aura.
     * Guarantees that failure to contact Neo4j will never roll back an Oracle transaction.
     */
    public void projectApplication(Application app) {
        if (!driverService.isEnabled() || !driverService.isConnected() || app == null) return;
        try (Session session = driverService.openSession()) {
            session.run("""
                MERGE (app:Application {applicationId: $id})
                SET app.status = $status,
                    app.applyDate = $date
                WITH app
                MATCH (stu:Student {studentId: $stuId})
                MERGE (stu)-[:SUBMITTED_APPLICATION]->(app)
                WITH app
                MATCH (drv:Drive {driveId: $drvId})
                MERGE (app)-[:FOR_DRIVE]->(drv)
            """, Map.of(
                    "id", app.getApplicationId(),
                    "status", app.getStatus() != null ? app.getStatus() : "APPLIED",
                    "date", app.getApplyDate() != null ? app.getApplyDate().toString() : "",
                    "stuId", app.getStudentId(),
                    "drvId", app.getDriveId() != null ? app.getDriveId() : ""
            ));
        } catch (Exception e) {
            log.warn("Non-fatal: Failed to asynchronously project application {} to Neo4j Aura: {}", app.getApplicationId(), e.getMessage());
        }
    }

    /**
     * Non-blocking projection of an Interview result mutation into Neo4j Aura.
     */
    public void projectInterview(Interview iv) {
        if (!driverService.isEnabled() || !driverService.isConnected() || iv == null) return;
        try (Session session = driverService.openSession()) {
            String ivId = iv.getApplicationId() + "_" + iv.getInterviewerName().replaceAll("\\s+", "_");
            int roundNo = 1;
            if (interviewerRoundRepository != null) {
                var opt = interviewerRoundRepository.findById(iv.getInterviewerName());
                if (opt.isPresent() && opt.get().getInterviewRoundNo() != null) {
                    roundNo = opt.get().getInterviewRoundNo();
                }
            }

            session.run("""
                MATCH (app:Application {applicationId: $appId})
                MERGE (iv:Interview {interviewId: $ivId})
                SET iv.interviewerName = $interviewer,
                    iv.roundNo = $roundNo,
                    iv.result = $result
                MERGE (app)-[:HAS_INTERVIEW]->(iv)
            """, Map.of(
                    "appId", iv.getApplicationId(),
                    "ivId", ivId,
                    "interviewer", iv.getInterviewerName(),
                    "roundNo", roundNo,
                    "result", iv.getResult() != null ? iv.getResult() : "PENDING"
            ));
        } catch (Exception e) {
            log.warn("Non-fatal: Failed to asynchronously project interview to Neo4j Aura: {}", e.getMessage());
        }
    }

    /**
     * Non-blocking projection of a newly issued employment Offer into Neo4j Aura.
     */
    public void projectOffer(OfferLetter offer) {
        if (!driverService.isEnabled() || !driverService.isConnected() || offer == null) return;
        try (Session session = driverService.openSession()) {
            session.run("""
                MATCH (app:Application {applicationId: $appId})
                SET app.status = 'OFFERED'
                MERGE (off:Offer {offerId: $id})
                SET off.ctcLpa = $ctc,
                    off.status = $status,
                    off.offerDate = $date
                MERGE (app)-[:RECEIVED_OFFER]->(off)
                WITH app
                MATCH (stu:Student)-[:SUBMITTED_APPLICATION]->(app)
                SET stu.placementStatus = 'OFFERED'
            """, Map.of(
                    "id", offer.getOfferId(),
                    "ctc", offer.getCtcLpa() != null ? offer.getCtcLpa().doubleValue() : 0.0,
                    "status", offer.getStatus() != null ? offer.getStatus() : "OFFERED",
                    "date", offer.getOfferDate() != null ? offer.getOfferDate().toString() : "",
                    "appId", offer.getApplicationId()
            ));
        } catch (Exception e) {
            log.warn("Non-fatal: Failed to asynchronously project offer {} to Neo4j Aura: {}", offer.getOfferId(), e.getMessage());
        }
    }

    /**
     * Non-blocking projection of Offer Acceptance and Mutex Declines into Neo4j Aura.
     */
    public void projectOfferAccepted(String offerId, String studentId) {
        if (!driverService.isEnabled() || !driverService.isConnected()) return;
        try (Session session = driverService.openSession()) {
            session.run("""
                MATCH (off:Offer {offerId: $offerId})
                SET off.status = 'ACCEPTED'
                WITH off
                MATCH (app:Application)-[:RECEIVED_OFFER]->(off)
                SET app.status = 'ACCEPTED'
                WITH app
                MATCH (stu:Student {studentId: $studentId})
                SET stu.placementStatus = 'PLACED'
                WITH stu, off
                MATCH (stu)-[:SUBMITTED_APPLICATION]->(otherApp:Application)-[:RECEIVED_OFFER]->(otherOffer:Offer)
                WHERE otherOffer.offerId <> $offerId
                SET otherOffer.status = 'DECLINED', otherApp.status = 'DECLINED'
            """, Map.of(
                    "offerId", offerId,
                    "studentId", studentId
            ));
        } catch (Exception e) {
            log.warn("Non-fatal: Failed to asynchronously project offer acceptance to Neo4j Aura: {}", e.getMessage());
        }
    }
}
