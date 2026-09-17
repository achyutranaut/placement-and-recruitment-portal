package com.placement.portal;

import com.placement.portal.application.*;
import com.placement.portal.auth.PortalUser;
import com.placement.portal.auth.Role;
import com.placement.portal.auth.UserRepository;
import com.placement.portal.company.*;
import com.placement.portal.interview.Interview;
import com.placement.portal.interview.InterviewRepository;
import com.placement.portal.interview.InterviewerRound;
import com.placement.portal.interview.InterviewerRoundRepository;
import com.placement.portal.offer.OfferLetter;
import com.placement.portal.offer.OfferRepository;
import com.placement.portal.program.*;
import com.placement.portal.recruitment.*;
import com.placement.portal.student.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test Fixture Initializer - ISOLATED TO TEST SLICE ONLY.
 * This class ensures that unit and integration tests under src/test/java have predictable
 * test fixtures available in H2 without polluting the production runtime.
 *
 * ONLY activated for the "h2" profile. Oracle-profile integration tests
 * (e.g. LiveOracleResumeIntegrationTests) connect to a live Oracle database that
 * already contains real data (STU023, APP036, etc.) and must NOT have mock
 * H2 fixtures grafted on top of them.
 */
@Component
@Order(2)
@org.springframework.context.annotation.Profile("h2")
public class TestFixturesInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestFixturesInitializer.class);

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final BatchRepository batchRepository;
    private final ProgramRegistrationRepository programRegistrationRepository;
    private final CompanyRepository companyRepository;
    private final EmailCompanyRepository emailCompanyRepository;
    private final JobCompanyRepository jobCompanyRepository;
    private final PlacementDriveRepository driveRepository;
    private final InterviewerRoundRepository interviewerRoundRepository;
    private final ApplicationRepository applicationRepository;
    private final DailyRoutineRepository dailyRoutineRepository;
    private final InterviewRepository interviewRepository;
    private final OfferRepository offerRepository;
    private final ApplicationAuditRepository auditRepository;
    private final StudentResumeRepository resumeRepository;
    private final ResumeEvaluationRepository evaluationRepository;
    private final DriveEligibilityRepository driveEligibilityRepository;
    private final RecruiterCompanyRepository recruiterCompanyRepository;
    private final PasswordEncoder passwordEncoder;

    public TestFixturesInitializer(
            UserRepository userRepository,
            StudentRepository studentRepository,
            ProgramRepository programRepository,
            BatchRepository batchRepository,
            ProgramRegistrationRepository programRegistrationRepository,
            CompanyRepository companyRepository,
            EmailCompanyRepository emailCompanyRepository,
            JobCompanyRepository jobCompanyRepository,
            PlacementDriveRepository driveRepository,
            InterviewerRoundRepository interviewerRoundRepository,
            ApplicationRepository applicationRepository,
            DailyRoutineRepository dailyRoutineRepository,
            InterviewRepository interviewRepository,
            OfferRepository offerRepository,
            ApplicationAuditRepository auditRepository,
            StudentResumeRepository resumeRepository,
            ResumeEvaluationRepository evaluationRepository,
            DriveEligibilityRepository driveEligibilityRepository,
            RecruiterCompanyRepository recruiterCompanyRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.batchRepository = batchRepository;
        this.programRegistrationRepository = programRegistrationRepository;
        this.companyRepository = companyRepository;
        this.emailCompanyRepository = emailCompanyRepository;
        this.jobCompanyRepository = jobCompanyRepository;
        this.driveRepository = driveRepository;
        this.interviewerRoundRepository = interviewerRoundRepository;
        this.applicationRepository = applicationRepository;
        this.dailyRoutineRepository = dailyRoutineRepository;
        this.interviewRepository = interviewRepository;
        this.offerRepository = offerRepository;
        this.auditRepository = auditRepository;
        this.resumeRepository = resumeRepository;
        this.evaluationRepository = evaluationRepository;
        this.driveEligibilityRepository = driveEligibilityRepository;
        this.recruiterCompanyRepository = recruiterCompanyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (driveRepository.count() > 0 && studentRepository.count() >= 20) {
            return;
        }

        log.info("[TEST-ONLY] Initializing isolated test fixtures for automated test suites...");

        seedUsers();
        seedRecruiterCompanies();
        seedStudents();
        seedProgramsAndBatches();
        seedCompaniesAndJobs();
        seedPlacementDrives();
        seedInterviewerRounds();
        seedApplications();
        seedInterviews();
        seedOffersAndAudits();
        seedResumes();
        seedResumeEvaluations();
        seedDriveEligibility();
        alignDriveAndOfferPackages();
    }

    private void seedUsers() {
        if (userRepository.count() <= 1) {
            userRepository.saveAll(List.of(
                    new PortalUser("USR001", "student1", passwordEncoder.encode("password123"), Role.ROLE_STUDENT, "STU001"),
                    new PortalUser("USR002", "student2", passwordEncoder.encode("password123"), Role.ROLE_STUDENT, "STU002"),
                    new PortalUser("USR006", "student4", passwordEncoder.encode("password123"), Role.ROLE_STUDENT, "STU004"),
                    new PortalUser("USR003", "recruiter1", passwordEncoder.encode("password123"), Role.ROLE_RECRUITER, "COM004", "Vikram Singh", "vikram.singh@microsoft.com"),
                    new PortalUser("USR004", "recruiter2", passwordEncoder.encode("password123"), Role.ROLE_RECRUITER, "COM001", "Neha Sharma", "neha.sharma@tcs.com"),
                    new PortalUser("USR_COM002", "infosys_recruiter", passwordEncoder.encode("password123"), Role.ROLE_RECRUITER, "COM002", "Infosys Recruiter", "talent@infosys.com"),
                    new PortalUser("USR_COM003", "wipro_recruiter", passwordEncoder.encode("password123"), Role.ROLE_RECRUITER, "COM003", "Wipro Recruiter", "careers@wipro.com"),
                    new PortalUser("USR_COM005", "amazon_recruiter", passwordEncoder.encode("password123"), Role.ROLE_RECRUITER, "COM005", "Amazon Recruiter", "campus-in@amazon.com"),
                    new PortalUser("USR_COM006", "oracle_recruiter", passwordEncoder.encode("password123"), Role.ROLE_RECRUITER, "COM006", "Oracle Recruiter", "campus_recruitment@oracle.com")
            ));
        }
    }

    private void seedRecruiterCompanies() {
        if (recruiterCompanyRepository.count() == 0) {
            recruiterCompanyRepository.saveAll(List.of(
                    new RecruiterCompany("USR003", "COM004"),
                    new RecruiterCompany("USR003", "COM001"),
                    new RecruiterCompany("USR004", "COM001"),
                    new RecruiterCompany("USR_COM002", "COM002"),
                    new RecruiterCompany("USR_COM003", "COM003"),
                    new RecruiterCompany("USR_COM005", "COM005"),
                    new RecruiterCompany("USR_COM006", "COM006")
            ));
        }
    }

    private void seedStudents() {
        if (studentRepository.count() < 22) {
            List<Student> students = List.of(
                    new Student("STU001", "Aarav Sharma", "aarav.sharma@vitstudent.ac.in", "12 Brigade Road", "Bengaluru", "Karnataka", LocalDate.of(2003, 4, 15), BigDecimal.valueOf(9.25), "Computer Science & Engineering", "21BCE1001", "BCE"),
                    new Student("STU002", "Diya Patel", "diya.patel@vitstudent.ac.in", "45 CG Road", "Ahmedabad", "Gujarat", LocalDate.of(2003, 8, 20), BigDecimal.valueOf(8.80), "Information Technology", "21BCT1002", "BCT"),
                    new Student("STU003", "Rohan Iyer", "rohan.iyer@vitstudent.ac.in", "78 Anna Salai", "Chennai", "Tamil Nadu", LocalDate.of(2002, 11, 12), BigDecimal.valueOf(8.45), "Computer Science & Engineering", "21BCE1003", "BCE"),
                    new Student("STU004", "Ananya Gupta", "ananya.gupta@vitstudent.ac.in", "23 Park Street", "Kolkata", "West Bengal", LocalDate.of(2003, 2, 18), BigDecimal.valueOf(9.50), "Data Science", "21BDS1004", "BDS"),
                    new Student("STU005", "Ishaan Verma", "ishaan.verma@vitstudent.ac.in", "89 Hazratganj", "Lucknow", "Uttar Pradesh", LocalDate.of(2003, 6, 30), BigDecimal.valueOf(7.90), "Electronics & Communication", "21BEC1005", "BEC"),
                    new Student("STU006", "Priya Nair", "priya.nair@vitstudent.ac.in", "56 MG Road", "Kochi", "Kerala", LocalDate.of(2002, 9, 5), BigDecimal.valueOf(8.65), "Computer Science & Engineering", "21BCE1006", "BCE"),
                    new Student("STU007", "Siddharth Rao", "siddharth.rao@vitstudent.ac.in", "34 Banjara Hills", "Hyderabad", "Telangana", LocalDate.of(2003, 1, 25), BigDecimal.valueOf(8.10), "Information Technology", "21BCT1007", "BCT"),
                    new Student("STU008", "Tanvi Joshi", "tanvi.joshi@vitstudent.ac.in", "67 FC Road", "Pune", "Maharashtra", LocalDate.of(2003, 7, 14), BigDecimal.valueOf(9.15), "Computer Science & Engineering", "21BCE1008", "BCE"),
                    new Student("STU009", "Aditya Deshmukh", "aditya.deshmukh@vitstudent.ac.in", "101 Shivaji Nagar", "Nagpur", "Maharashtra", LocalDate.of(2002, 12, 1), BigDecimal.valueOf(7.60), "Mechanical Engineering", "21BME1009", "BME"),
                    new Student("STU010", "Sneha Kulkarni", "sneha.kulkarni@vitstudent.ac.in", "15 Deccan Gymkhana", "Pune", "Maharashtra", LocalDate.of(2003, 3, 22), BigDecimal.valueOf(8.95), "Computer Science & Engineering", "21BCE1010", "BCE"),
                    new Student("STU011", "Vikram Reddy", "vikram.reddy@vitstudent.ac.in", "88 Jubilee Hills", "Hyderabad", "Telangana", LocalDate.of(2003, 5, 19), BigDecimal.valueOf(8.35), "Data Science", "21BDS1011", "BDS"),
                    new Student("STU012", "Neha Sengupta", "neha.sengupta@vitstudent.ac.in", "42 Salt Lake Sector 5", "Kolkata", "West Bengal", LocalDate.of(2003, 10, 10), BigDecimal.valueOf(9.40), "Computer Science & Engineering", "21BCE1012", "BCE"),
                    new Student("STU013", "Rahul Mehra", "rahul.mehra@vitstudent.ac.in", "19 Connaught Place", "New Delhi", "Delhi", LocalDate.of(2002, 8, 16), BigDecimal.valueOf(7.80), "Information Technology", "21BCT1013", "BCT"),
                    new Student("STU014", "Riya Kapoor", "riya.kapoor@vitstudent.ac.in", "73 Bandra West", "Mumbai", "Maharashtra", LocalDate.of(2003, 11, 28), BigDecimal.valueOf(8.70), "Electronics & Communication", "21BEC1014", "BEC"),
                    new Student("STU015", "Arjun Das", "arjun.das@vitstudent.ac.in", "91 GS Road", "Guwahati", "Assam", LocalDate.of(2003, 4, 3), BigDecimal.valueOf(7.45), "Electrical & Electronics", "21BEE1015", "BEE"),
                    new Student("STU016", "Pooja Hegde", "pooja.hegde@vitstudent.ac.in", "27 Hampankatta", "Mangaluru", "Karnataka", LocalDate.of(2003, 6, 11), BigDecimal.valueOf(8.55), "Computer Science & Engineering", "21BCE1016", "BCE"),
                    new Student("STU017", "Varun Choudhury", "varun.choudhury@vitstudent.ac.in", "31 MI Road", "Jaipur", "Rajasthan", LocalDate.of(2002, 10, 24), BigDecimal.valueOf(8.05), "Mechanical Engineering", "21BME1017", "BME"),
                    new Student("STU018", "Meera Nambiar", "meera.nambiar@vitstudent.ac.in", "50 Palayam", "Thiruvananthapuram", "Kerala", LocalDate.of(2003, 1, 9), BigDecimal.valueOf(9.10), "Information Technology", "21BCT1018", "BCT"),
                    new Student("STU019", "Kunal Malhotra", "kunal.malhotra@vitstudent.ac.in", "62 Sector 17", "Chandigarh", "Chandigarh", LocalDate.of(2002, 7, 7), BigDecimal.valueOf(7.30), "Civil Engineering", "21BCL1019", "BCV"),
                    new Student("STU020", "Shreya Mukherjee", "shreya.mukherjee@vitstudent.ac.in", "84 South City", "Kolkata", "West Bengal", LocalDate.of(2003, 9, 17), BigDecimal.valueOf(8.90), "Data Science", "21BDS1020", "BDS"),
                    new Student("STU021", "Karthik Subramanian", "karthik.subramanian@vitstudent.ac.in", "11 T Nagar", "Chennai", "Tamil Nadu", LocalDate.of(2002, 12, 19), BigDecimal.valueOf(9.60), "Computer Science & Engineering", "21BCE1021", "BCE"),
                    new Student("STU022", "Divya Menon", "divya.menon@vitstudent.ac.in", "95 Marine Drive", "Kochi", "Kerala", LocalDate.of(2003, 5, 2), BigDecimal.valueOf(8.20), "Electronics & Communication", "21BEC1022", "BEC")
            );

            students.forEach(s -> {
                s.setPhoneNumbers(List.of("+91-98765" + s.getStudentId().replace("STU", "43")));
                if (s.getStudentId().equals("STU001")) {
                    s.setSkills(List.of("Java", "Spring Boot", "Oracle SQL", "Docker", "Microservices", "Kubernetes", "React"));
                } else if (s.getStudentId().equals("STU002")) {
                    s.setSkills(List.of("Python", "Django", "PostgreSQL", "AWS", "REST APIs", "Machine Learning"));
                } else if (s.getStudentId().equals("STU003")) {
                    s.setSkills(List.of("C++", "Data Structures & Algorithms", "Distributed Systems", "Linux Kernel", "Redis"));
                } else {
                    s.setSkills(List.of("Java", "SQL", "Data Structures", "Cloud Computing", "Web Technologies"));
                }
            });
            studentRepository.saveAll(students);
        }
    }

    private void seedProgramsAndBatches() {
        if (batchRepository.count() == 0) {
            Batch b1 = new Batch("BCE", "BAT001", "Mon-Wed-Fri 09:00-11:00", "Lab 401", LocalDate.of(2025, 8, 1), LocalDate.of(2025, 10, 31));
            Batch b2 = new Batch("BCT", "BAT002", "Tue-Thu-Sat 10:00-12:00", "AI Lab 201", LocalDate.of(2025, 8, 1), LocalDate.of(2025, 11, 30));
            Batch b3 = new Batch("BEC", "BAT003", "Mon-Wed-Fri 14:00-16:00", "Cloud Lab 105", LocalDate.of(2025, 8, 1), LocalDate.of(2025, 10, 31));
            Batch b4 = new Batch("BDS", "BAT004", "Mon-to-Fri 16:30-18:00", "Seminar Hall B", LocalDate.of(2025, 8, 1), LocalDate.of(2025, 9, 30));
            Batch b5 = new Batch("BME", "BAT005", "Tue-Thu 14:00-17:00", "Design Lab 102", LocalDate.of(2025, 8, 1), LocalDate.of(2025, 11, 30));

            batchRepository.saveAll(List.of(b1, b2, b3, b4, b5));

            programRegistrationRepository.saveAll(List.of(
                    new ProgramRegistration("STU001", "BCE", LocalDate.of(2026, 1, 5)),
                    new ProgramRegistration("STU002", "BCT", LocalDate.of(2026, 1, 5)),
                    new ProgramRegistration("STU003", "BCE", LocalDate.of(2026, 1, 5)),
                    new ProgramRegistration("STU004", "BDS", LocalDate.of(2026, 1, 5)),
                    new ProgramRegistration("STU005", "BEC", LocalDate.of(2026, 1, 5))
            ));
        }
    }

    private void seedCompaniesAndJobs() {
        if (companyRepository.count() == 0) {
            EmailCompany e1 = new EmailCompany("campus@tcs.com", "Tata Consultancy Services");
            EmailCompany e2 = new EmailCompany("talent@infosys.com", "Infosys Limited");
            EmailCompany e3 = new EmailCompany("careers@wipro.com", "Wipro Enterprises");
            EmailCompany e4 = new EmailCompany("indiarecruiting@microsoft.com", "Microsoft India");
            EmailCompany e5 = new EmailCompany("campus-in@amazon.com", "Amazon Development Centre");
            EmailCompany e6 = new EmailCompany("campus_recruitment@oracle.com", "Oracle India Pvt Ltd");
            EmailCompany e7 = new EmailCompany("campuscareers@gs.com", "Goldman Sachs Services");
            EmailCompany e8 = new EmailCompany("talent_apac@cisco.com", "Cisco Systems India");
            EmailCompany e9 = new EmailCompany("hiring@flipkart.com", "Flipkart Internet Pvt Ltd");
            EmailCompany e10 = new EmailCompany("earlycareers@zomato.com", "Zomato Limited");

            emailCompanyRepository.saveAll(List.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));

            Company c1 = new Company("COM001", "campus@tcs.com", "IT Services & Consulting");
            c1.setPhoneNumbers(List.of("+91-22-67789999"));
            Company c2 = new Company("COM002", "talent@infosys.com", "IT Services & Consulting");
            c2.setPhoneNumbers(List.of("+91-80-28520261"));
            Company c3 = new Company("COM003", "careers@wipro.com", "IT Services & Consulting");
            c3.setPhoneNumbers(List.of("+91-80-28440011"));
            Company c4 = new Company("COM004", "indiarecruiting@microsoft.com", "Software Products & Cloud");
            c4.setPhoneNumbers(List.of("+91-40-66930000"));
            Company c5 = new Company("COM005", "campus-in@amazon.com", "E-Commerce & Cloud Computing");
            c5.setPhoneNumbers(List.of("+91-80-41030000"));
            Company c6 = new Company("COM006", "campus_recruitment@oracle.com", "Enterprise Software & Database");
            c6.setPhoneNumbers(List.of("+91-80-41084000"));
            Company c7 = new Company("COM007", "campuscareers@gs.com", "Financial Services & FinTech");
            c7.setPhoneNumbers(List.of("+91-80-41278000"));
            Company c8 = new Company("COM008", "talent_apac@cisco.com", "Networking & Cyber Security");
            c8.setPhoneNumbers(List.of("+91-80-44260000"));
            Company c9 = new Company("COM009", "hiring@flipkart.com", "E-Commerce & Retail Tech");
            c9.setPhoneNumbers(List.of("+91-80-67981111"));
            Company c10 = new Company("COM010", "earlycareers@zomato.com", "FoodTech & Consumer Internet");
            c10.setPhoneNumbers(List.of("+91-124-4268000"));

            companyRepository.saveAll(List.of(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10));

            JobCompany j1 = new JobCompany("Associate Software Engineer", "COM001");
            JobCompany j2 = new JobCompany("Systems Engineer - Specialist", "COM002");
            JobCompany j3 = new JobCompany("Project Engineer", "COM003");
            JobCompany j4 = new JobCompany("Software Development Engineer I", "COM004");
            JobCompany j5 = new JobCompany("Cloud Support Associate", "COM005");
            JobCompany j6 = new JobCompany("Associate Applications Engineer", "COM006");
            JobCompany j7 = new JobCompany("Technology Analyst", "COM007");
            JobCompany j8 = new JobCompany("Network Consulting Engineer", "COM008");
            JobCompany j9 = new JobCompany("Backend Software Engineer", "COM009");
            JobCompany j10 = new JobCompany("Software Engineer - Platform", "COM010");

            jobCompanyRepository.saveAll(List.of(j1, j2, j3, j4, j5, j6, j7, j8, j9, j10));
        }
    }

    private void seedPlacementDrives() {
        if (driveRepository.count() == 0) {
            List<PlacementDrive> drives = List.of(
                    new PlacementDrive("DRV001", "Software Development Engineer I", BigDecimal.valueOf(8.50), LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 20), 15, BigDecimal.valueOf(44.00), "Design, build, and deploy high-concurrency microservices, cloud pipelines, and distributed event architectures.", "Bengaluru / Hyderabad", "CSE, IT, Data Science", "1. Online Assessment | 2. Technical Interview | 3. HR Interview"),
                    new PlacementDrive("DRV002", "Associate Software Engineer", BigDecimal.valueOf(6.50), LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 22), 50, BigDecimal.valueOf(7.50), "Develop reliable enterprise applications, modernization services, and digital cloud workflows.", "Pan India / Multiple Locations", "All Engineering Disciplines", "1. Foundation OA | 2. Advanced Coding | 3. Managerial HR"),
                    new PlacementDrive("DRV003", "Systems Engineer - Specialist", BigDecimal.valueOf(7.00), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 9, 26), 25, BigDecimal.valueOf(9.50), "Build cloud infrastructure, microservices, and AI-enabled software solutions.", "Bengaluru / Pune", "CSE, IT, ECE", "1. HackerEarth Challenge | 2. Technical Round | 3. HR Fitment"),
                    new PlacementDrive("DRV004", "Cloud Support Associate", BigDecimal.valueOf(7.50), LocalDate.of(2026, 10, 5), LocalDate.of(2026, 9, 29), 20, BigDecimal.valueOf(18.50), "Support mission-critical AWS enterprise infrastructure and design automated troubleshooting tooling.", "Hyderabad / Chennai", "CSE, IT, ECE, Data Science", "1. Online Coding | 2. Cloud Architecture Review | 3. Bar Raiser"),
                    new PlacementDrive("DRV005", "Associate Applications Engineer", BigDecimal.valueOf(8.00), LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 1), 12, BigDecimal.valueOf(21.00), "Architect mission-critical Oracle Cloud ERP applications and distributed database engines.", "Bengaluru", "CSE, IT", "1. Coding Assessment | 2. System Design & Database | 3. Director Round"),
                    new PlacementDrive("DRV006", "Technology Analyst", BigDecimal.valueOf(8.50), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 4), 10, BigDecimal.valueOf(32.00), "Build algorithmic trading infrastructure, risk analytics, and quantitative valuation platforms.", "Bengaluru", "CSE, IT, Data Science", "1. Math & Coding OA | 2. Core CS Interviews | 3. Managing Director Review"),
                    new PlacementDrive("DRV007", "Backend Software Engineer", BigDecimal.valueOf(8.00), LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 7), 18, BigDecimal.valueOf(26.00), "Scale ultra-high volume e-commerce order systems, supply-chain logistics, and payment gateways.", "Bengaluru", "CSE, IT, Data Science", "1. Machine Coding | 2. Data Structures & System Design | 3. Engineering Director"),
                    new PlacementDrive("DRV008", "Network Consulting Engineer", BigDecimal.valueOf(7.20), LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 10), 15, BigDecimal.valueOf(18.50), "Implement next-generation software-defined networking, cloud interconnects, and zero-trust security.", "Bengaluru", "CSE, IT, ECE", "1. Networking & Logic OA | 2. Technical Deep Dive | 3. HR Leadership")
            );
            driveRepository.saveAll(drives);
        }
    }

    private void seedInterviewerRounds() {
        if (interviewerRoundRepository.count() == 0) {
            interviewerRoundRepository.saveAll(List.of(
                    new InterviewerRound("Vikram Malhotra", 1),
                    new InterviewerRound("Sangeeta Rao", 2),
                    new InterviewerRound("Arvind Swamy", 3),
                    new InterviewerRound("Dr. Rajesh Sundaram", 1),
                    new InterviewerRound("Priya Ramanathan", 2),
                    new InterviewerRound("Vikramaditya Bose", 3),
                    new InterviewerRound("Ananya Chatterjee", 1),
                    new InterviewerRound("Suresh Venkataraman", 2),
                    new InterviewerRound("Kavita Menon", 3)
            ));
        }
    }

    private void seedApplications() {
        if (applicationRepository.count() == 0) {
            List<StudentDailyRoutineDrive> routines = List.of(
                    new StudentDailyRoutineDrive("STU001", LocalDate.of(2026, 9, 1), "DRV001"),
                    new StudentDailyRoutineDrive("STU001", LocalDate.of(2026, 9, 2), "DRV002"),
                    new StudentDailyRoutineDrive("STU002", LocalDate.of(2026, 9, 1), "DRV001"),
                    new StudentDailyRoutineDrive("STU002", LocalDate.of(2026, 9, 2), "DRV003"),
                    new StudentDailyRoutineDrive("STU003", LocalDate.of(2026, 9, 1), "DRV001"),
                    new StudentDailyRoutineDrive("STU003", LocalDate.of(2026, 9, 3), "DRV004"),
                    new StudentDailyRoutineDrive("STU004", LocalDate.of(2026, 9, 1), "DRV001"),
                    new StudentDailyRoutineDrive("STU004", LocalDate.of(2026, 9, 4), "DRV006"),
                    new StudentDailyRoutineDrive("STU004", LocalDate.of(2026, 9, 5), "DRV008"),
                    new StudentDailyRoutineDrive("STU006", LocalDate.of(2026, 9, 1), "DRV001"),
                    new StudentDailyRoutineDrive("STU006", LocalDate.of(2026, 9, 2), "DRV002"),
                    new StudentDailyRoutineDrive("STU008", LocalDate.of(2026, 9, 1), "DRV001"),
                    new StudentDailyRoutineDrive("STU008", LocalDate.of(2026, 9, 4), "DRV007"),
                    new StudentDailyRoutineDrive("STU010", LocalDate.of(2026, 9, 1), "DRV001"),
                    new StudentDailyRoutineDrive("STU011", LocalDate.of(2026, 9, 2), "DRV002"),
                    new StudentDailyRoutineDrive("STU012", LocalDate.of(2026, 9, 1), "DRV001"),
                    new StudentDailyRoutineDrive("STU014", LocalDate.of(2026, 9, 3), "DRV003"),
                    new StudentDailyRoutineDrive("STU016", LocalDate.of(2026, 9, 1), "DRV001"),
                    new StudentDailyRoutineDrive("STU018", LocalDate.of(2026, 9, 2), "DRV002"),
                    new StudentDailyRoutineDrive("STU020", LocalDate.of(2026, 9, 4), "DRV004"),
                    new StudentDailyRoutineDrive("STU021", LocalDate.of(2026, 9, 1), "DRV001"),
                    new StudentDailyRoutineDrive("STU022", LocalDate.of(2026, 9, 2), "DRV002")
            );
            dailyRoutineRepository.saveAll(routines);

            List<Application> apps = List.of(
                    new Application("APP001", "STU001", "DRV001", LocalDate.of(2026, 9, 1), "OFFERED"),
                    new Application("APP002", "STU001", "DRV002", LocalDate.of(2026, 9, 2), "INTERVIEWING"),
                    new Application("APP003", "STU002", "DRV001", LocalDate.of(2026, 9, 1), "OFFERED"),
                    new Application("APP004", "STU002", "DRV003", LocalDate.of(2026, 9, 2), "OFFERED"),
                    new Application("APP005", "STU003", "DRV001", LocalDate.of(2026, 9, 1), "SELECTED"),
                    new Application("APP006", "STU003", "DRV004", LocalDate.of(2026, 9, 3), "INTERVIEWING"),
                    new Application("APP007", "STU004", "DRV001", LocalDate.of(2026, 9, 1), "OFFERED"),
                    new Application("APP008", "STU004", "DRV006", LocalDate.of(2026, 9, 4), "OFFERED"),
                    new Application("APP009", "STU004", "DRV008", LocalDate.of(2026, 9, 5), "INTERVIEWING"),
                    new Application("APP010", "STU006", "DRV001", LocalDate.of(2026, 9, 1), "OFFERED"),
                    new Application("APP011", "STU006", "DRV002", LocalDate.of(2026, 9, 2), "OFFERED"),
                    new Application("APP012", "STU008", "DRV001", LocalDate.of(2026, 9, 1), "OFFERED"),
                    new Application("APP013", "STU008", "DRV007", LocalDate.of(2026, 9, 4), "SHORTLISTED"),
                    new Application("APP014", "STU010", "DRV001", LocalDate.of(2026, 9, 1), "OFFERED"),
                    new Application("APP015", "STU011", "DRV002", LocalDate.of(2026, 9, 2), "OFFERED"),
                    new Application("APP016", "STU012", "DRV001", LocalDate.of(2026, 9, 1), "OFFERED"),
                    new Application("APP017", "STU014", "DRV003", LocalDate.of(2026, 9, 3), "OFFERED"),
                    new Application("APP018", "STU016", "DRV001", LocalDate.of(2026, 9, 1), "OFFERED"),
                    new Application("APP019", "STU018", "DRV002", LocalDate.of(2026, 9, 2), "APPLIED"),
                    new Application("APP020", "STU020", "DRV004", LocalDate.of(2026, 9, 4), "SHORTLISTED"),
                    new Application("APP021", "STU021", "DRV001", LocalDate.of(2026, 9, 1), "OFFERED"),
                    new Application("APP022", "STU022", "DRV002", LocalDate.of(2026, 9, 2), "APPLIED")
            );
            applicationRepository.saveAll(apps);
        }
    }

    private void seedInterviews() {
        if (interviewRepository.count() == 0) {
            List<Interview> ivs = List.of(
                    new Interview("APP001", "Vikram Malhotra", BigDecimal.valueOf(95), null, null, "CLEARED", "Y", "N"),
                    new Interview("APP001", "Sangeeta Rao", null, BigDecimal.valueOf(92), null, "CLEARED", "N", "Y"),
                    new Interview("APP001", "Arvind Swamy", null, null, BigDecimal.valueOf(96), "CLEARED", "N", "Y"),
                    new Interview("APP002", "Vikram Malhotra", BigDecimal.valueOf(88.0), null, null, "CLEARED", "Y", "N"),
                    new Interview("APP002", "Sangeeta Rao", null, BigDecimal.valueOf(85.0), null, "PENDING", "N", "Y"),
                    new Interview("APP003", "Vikram Malhotra", BigDecimal.valueOf(90.0), null, null, "CLEARED", "Y", "N"),
                    new Interview("APP004", "Ananya Chatterjee", BigDecimal.valueOf(84.0), null, null, "CLEARED", "Y", "N"),
                    new Interview("APP004", "Suresh Venkataraman", null, BigDecimal.valueOf(87.0), null, "CLEARED", "N", "Y"),
                    new Interview("APP007", "Dr. Rajesh Sundaram", BigDecimal.valueOf(98.0), null, null, "CLEARED", "Y", "N"),
                    new Interview("APP007", "Priya Ramanathan", null, BigDecimal.valueOf(95.0), null, "CLEARED", "N", "Y"),
                    new Interview("APP007", "Vikramaditya Bose", null, null, BigDecimal.valueOf(97.0), "CLEARED", "N", "Y"),
                    new Interview("APP008", "Ananya Chatterjee", BigDecimal.valueOf(92.0), null, null, "CLEARED", "Y", "N"),
                    new Interview("APP008", "Suresh Venkataraman", null, BigDecimal.valueOf(94.0), null, "CLEARED", "N", "Y"),
                    new Interview("APP008", "Kavita Menon", null, null, BigDecimal.valueOf(95.0), "CLEARED", "N", "Y")
            );
            interviewRepository.saveAll(ivs);
        }
    }

    private void seedOffersAndAudits() {
        if (offerRepository.count() == 0) {
            List<OfferLetter> offers = List.of(
                    new OfferLetter("OFF001", "APP001", LocalDate.of(2026, 9, 12), BigDecimal.valueOf(44.00), "OFFERED"),
                    new OfferLetter("OFF002", "APP003", LocalDate.of(2026, 9, 12), BigDecimal.valueOf(9.50), "OFFERED"),
                    new OfferLetter("OFF003", "APP004", LocalDate.of(2026, 9, 12), BigDecimal.valueOf(9.50), "OFFERED"),
                    new OfferLetter("OFF004", "APP007", LocalDate.of(2026, 9, 12), BigDecimal.valueOf(44.00), "OFFERED"),
                    new OfferLetter("OFF005", "APP008", LocalDate.of(2026, 9, 13), BigDecimal.valueOf(32.00), "OFFERED"),
                    new OfferLetter("OFF006", "APP010", LocalDate.of(2026, 9, 14), BigDecimal.valueOf(44.00), "OFFERED"),
                    new OfferLetter("OFF007", "APP011", LocalDate.of(2026, 9, 12), BigDecimal.valueOf(9.50), "OFFERED"),
                    new OfferLetter("OFF008", "APP012", LocalDate.of(2026, 9, 12), BigDecimal.valueOf(44.00), "OFFERED"),
                    new OfferLetter("OFF009", "APP014", LocalDate.of(2026, 9, 12), BigDecimal.valueOf(44.00), "OFFERED"),
                    new OfferLetter("OFF010", "APP015", LocalDate.of(2026, 9, 12), BigDecimal.valueOf(9.50), "OFFERED"),
                    new OfferLetter("OFF011", "APP016", LocalDate.of(2026, 9, 12), BigDecimal.valueOf(44.00), "OFFERED"),
                    new OfferLetter("OFF012", "APP017", LocalDate.of(2026, 9, 12), BigDecimal.valueOf(9.50), "OFFERED"),
                    new OfferLetter("OFF013", "APP018", LocalDate.of(2026, 9, 12), BigDecimal.valueOf(44.00), "OFFERED"),
                    new OfferLetter("OFF014", "APP021", LocalDate.of(2026, 9, 12), BigDecimal.valueOf(44.00), "OFFERED")
            );
            offerRepository.saveAll(offers);
        }
    }

    private void seedResumes() {
        if (resumeRepository.count() == 0) {
            List<StudentResume> resumes = List.of(
                    new StudentResume("RES_STU001_V1", "STU001", "Aarav_Sharma_Resume_v1.pdf", "application/pdf", 245000L, createSamplePdf("Aarav Sharma", "Computer Science & Engineering"), 1, "N"),
                    new StudentResume("RES_STU001_V2", "STU001", "Aarav_Sharma_Resume_Latest.pdf", "application/pdf", 262144L, createSamplePdf("Aarav Sharma", "Computer Science & Engineering"), 2, "Y"),
                    new StudentResume("RES_STU002_V1", "STU002", "Diya_Patel_Resume.pdf", "application/pdf", 198000L, createSamplePdf("Diya Patel", "Information Technology"), 1, "Y"),
                    new StudentResume("RES_STU003_V1", "STU003", "Rohan_Iyer_Resume.pdf", "application/pdf", 215000L, createSamplePdf("Rohan Iyer", "Computer Science & Engineering"), 1, "Y"),
                    new StudentResume("RES_STU004_V1", "STU004", "Ananya_Gupta_Resume.pdf", "application/pdf", 280000L, createSamplePdf("Ananya Gupta", "Data Science"), 1, "Y")
            );
            resumeRepository.saveAll(resumes);
        }
    }

    private void seedResumeEvaluations() {
        if (evaluationRepository.count() == 0) {
            List<ResumeEvaluation> evals = List.of(
                    new ResumeEvaluation("EVAL001", "RES_STU001_V2", "APP001", "recruiter1", 95, 92, 94, 90, 94, "Outstanding candidate with extensive cloud computing and microservices architecture background. Highly recommended."),
                    new ResumeEvaluation("EVAL002", "RES_STU001_V2", "APP002", "recruiter2", 88, 90, 85, 82, 87, "Solid computer science fundamentals, clear communication and strong data structures grasp."),
                    new ResumeEvaluation("EVAL003", "RES_STU002_V1", "APP003", "recruiter1", 90, 89, 92, 86, 90, "Impressive project portfolio in distributed data processing. High technical competence."),
                    new ResumeEvaluation("EVAL004", "RES_STU003_V1", "APP006", "recruiter1", 92, 88, 90, 85, 89, "Strong problem solving and systems background. Passed preliminary evaluations with high marks."),
                    new ResumeEvaluation("EVAL005", "RES_STU004_V1", "APP007", "recruiter1", 96, 95, 96, 92, 95, "Exceptional academic pedigree (9.65 CGPA) and demonstrated leadership in university engineering hackathons.")
            );
            evaluationRepository.saveAll(evals);
        }
    }

    private void seedDriveEligibility() {
        if (driveEligibilityRepository.count() == 0) {
            driveEligibilityRepository.saveAll(List.of(
                    new DriveEligibility("DRV001", "BCE"),
                    new DriveEligibility("DRV001", "BCT"),
                    new DriveEligibility("DRV001", "BDS"),
                    new DriveEligibility("DRV002", "BCE"),
                    new DriveEligibility("DRV002", "BCT"),
                    new DriveEligibility("DRV002", "BEC"),
                    new DriveEligibility("DRV002", "BME"),
                    new DriveEligibility("DRV002", "BDS"),
                    new DriveEligibility("DRV002", "BEE"),
                    new DriveEligibility("DRV002", "BCV"),
                    new DriveEligibility("DRV003", "BCE"),
                    new DriveEligibility("DRV003", "BCT"),
                    new DriveEligibility("DRV003", "BEC"),
                    new DriveEligibility("DRV004", "BCE"),
                    new DriveEligibility("DRV004", "BCT"),
                    new DriveEligibility("DRV004", "BEC"),
                    new DriveEligibility("DRV004", "BDS"),
                    new DriveEligibility("DRV005", "BCE"),
                    new DriveEligibility("DRV005", "BCT"),
                    new DriveEligibility("DRV006", "BCE"),
                    new DriveEligibility("DRV006", "BCT"),
                    new DriveEligibility("DRV006", "BDS"),
                    new DriveEligibility("DRV007", "BCE"),
                    new DriveEligibility("DRV007", "BCT"),
                    new DriveEligibility("DRV007", "BDS"),
                    new DriveEligibility("DRV008", "BCE"),
                    new DriveEligibility("DRV008", "BCT"),
                    new DriveEligibility("DRV008", "BEC")
            ));
        }
    }

    private void alignDriveAndOfferPackages() {
        driveRepository.findById("DRV001").ifPresent(d -> {
            if (d.getCtc() == null || d.getCtc().compareTo(BigDecimal.valueOf(44.00)) != 0) {
                d.setCtc(BigDecimal.valueOf(44.00));
                driveRepository.save(d);
            }
        });
    }

    private byte[] createSamplePdf(String candidateName, String department) {
        String content = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
                "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n" +
                "3 0 obj<</Type/Page/MediaBox[0 0 595 842]/Parent 2 0 R/Resources<<>>>>endobj\n" +
                "xref\n0 4\n0000000000 65535 f\n0000000009 00000 n\n0000000052 00000 n\n0000000101 00000 n\n" +
                "trailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF\n";
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
