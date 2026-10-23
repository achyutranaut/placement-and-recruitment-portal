package com.placement.portal.neo4j;

import com.placement.portal.application.Application;
import com.placement.portal.application.ApplicationRepository;
import com.placement.portal.company.*;
import com.placement.portal.interview.Interview;
import com.placement.portal.interview.InterviewRepository;
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
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Projects authoritative Oracle relational entities into Neo4j Aura graph nodes and relationships.
 * Preserves security: strictly excludes passwords, password hashes, and unnecessary student PII.
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
            OfferRepository offerRepository
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
            long executionTimeMs
    ) {}

    @Transactional(readOnly = true)
    public ProjectionSummary syncOracleToNeo4j() {
        long start = System.currentTimeMillis();

        if (!driverService.isEnabled()) {
            return new ProjectionSummary(
                    false,
                    "Synchronization aborted: Neo4j Aura integration is disabled by configuration (portal.neo4j.enabled=false).",
                    0, 0, 0, 0, 0, 0, 0,
                    System.currentTimeMillis() - start
            );
        }

        if (!driverService.isConnected()) {
            return new ProjectionSummary(
                    false,
                    "Synchronization failed: Neo4j Aura is enabled but not connected. Check network reachability and credentials.",
                    0, 0, 0, 0, 0, 0, 0,
                    System.currentTimeMillis() - start
            );
        }

        try (Session session = driverService.openSession()) {
            // 1. Create Unique Constraints in Aura
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (p:Program) REQUIRE p.programId IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (s:Student) REQUIRE s.studentId IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (c:Company) REQUIRE c.companyId IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (d:Drive) REQUIRE d.driveId IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (a:Application) REQUIRE a.applicationId IS UNIQUE");
            session.run("CREATE CONSTRAINT IF NOT EXISTS FOR (o:Offer) REQUIRE o.offerId IS UNIQUE");

            // 2. Fetch Oracle entities
            List<Program> programs = programRepository.findAll();
            List<Student> students = studentRepository.findAll();
            List<Company> companies = companyRepository.findAll();
            List<PlacementDrive> drives = driveRepository.findAll();
            List<Application> applications = applicationRepository.findAll();
            List<Interview> interviews = interviewRepository.findAll();
            List<OfferLetter> offers = offerRepository.findAll();
            List<DriveEligibility> eligibilities = driveEligibilityRepository.findAll();

            Map<String, String> emailToName = emailCompanyRepository.findAll().stream()
                    .collect(Collectors.toMap(EmailCompany::getEmail, EmailCompany::getCompanyName, (a, b) -> a));
            Map<String, String> jobToCompanyId = jobCompanyRepository.findAll().stream()
                    .collect(Collectors.toMap(JobCompany::getJobTitle, JobCompany::getCompanyId, (a, b) -> a));

            // 3. Project Programs
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

            // 4. Project Companies
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

            // 5. Project Students & ENROLLED_IN relationship (WITHOUT password hashes or PII)
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

            // 6. Project Drives & HOSTS_DRIVE / REQUIRES_PROGRAM
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

            // Drive eligibility program links
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

            // 7. Project Applications & SUBMITTED_APPLICATION / FOR_DRIVE
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
                        "drvId", a.getDriveId()
                ));
            }

            // 8. Project Interviews
            for (Interview iv : interviews) {
                session.run("""
                    MATCH (app:Application {applicationId: $appId})
                    MERGE (iv:Interview {interviewId: $ivId})
                    SET iv.interviewerName = $interviewer,
                        iv.result = $result
                    MERGE (app)-[:HAS_INTERVIEW]->(iv)
                """, Map.of(
                        "appId", iv.getApplicationId(),
                        "ivId", iv.getApplicationId() + "_" + iv.getInterviewerName().replaceAll("\\s+", "_"),
                        "interviewer", iv.getInterviewerName(),
                        "result", iv.getResult() != null ? iv.getResult() : "PENDING"
                ));
            }

            // 9. Project Offers
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

            if (totalProjected == 0) {
                return new ProjectionSummary(
                        false,
                        "Synchronization aborted: No Oracle transactional records found to project.",
                        0, 0, 0, 0, 0, 0, 0,
                        elapsed
                );
            }

            log.info("Successfully projected {} Oracle transactional records into Neo4j Aura in {} ms.", totalProjected, elapsed);

            return new ProjectionSummary(
                    true,
                    "Successfully projected " + totalProjected + " Oracle records into Neo4j Aura graph.",
                    programs.size(),
                    students.size(),
                    companies.size(),
                    drives.size(),
                    applications.size(),
                    interviews.size(),
                    offers.size(),
                    elapsed
            );
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("Projection error: {}", e.getMessage());
            return new ProjectionSummary(
                    false,
                    "Synchronization failed: " + e.getMessage(),
                    0, 0, 0, 0, 0, 0, 0,
                    elapsed
            );
        }
    }
}
