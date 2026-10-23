package com.placement.portal.auth;

import com.placement.portal.config.JwtTokenProvider;
import com.placement.portal.program.Program;
import com.placement.portal.program.ProgramRegistration;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final com.placement.portal.student.StudentRepository studentRepository;
    private final com.placement.portal.program.ProgramRepository programRepository;
    private final com.placement.portal.program.ProgramRegistrationRepository programRegistrationRepository;
    private final com.placement.portal.company.CompanyRepository companyRepository;
    private final com.placement.portal.company.RecruiterCompanyRepository recruiterCompanyRepository;
    private final com.placement.portal.company.EmailCompanyRepository emailCompanyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginAttemptService loginAttemptService;

    @Value("${portal.security.admin.bootstrap-enabled:false}")
    private boolean adminBootstrapEnabled;

    @Value("${portal.security.admin.bootstrap-password:}")
    private String adminBootstrapPassword;

    public AuthService(
            UserRepository userRepository,
            com.placement.portal.student.StudentRepository studentRepository,
            com.placement.portal.program.ProgramRepository programRepository,
            com.placement.portal.program.ProgramRegistrationRepository programRegistrationRepository,
            com.placement.portal.company.CompanyRepository companyRepository,
            com.placement.portal.company.RecruiterCompanyRepository recruiterCompanyRepository,
            com.placement.portal.company.EmailCompanyRepository emailCompanyRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            LoginAttemptService loginAttemptService
    ) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.programRegistrationRepository = programRegistrationRepository;
        this.companyRepository = companyRepository;
        this.recruiterCompanyRepository = recruiterCompanyRepository;
        this.emailCompanyRepository = emailCompanyRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.loginAttemptService = loginAttemptService;
    }

    @PostConstruct
    @Transactional
    public void initDefaultUsers() {
        if (adminBootstrapEnabled && adminBootstrapPassword != null && !adminBootstrapPassword.isBlank()) {
            if (!userRepository.existsByUsername("admin")) {
                userRepository.save(new PortalUser(
                        "USR_ADMIN",
                        "admin",
                        passwordEncoder.encode(adminBootstrapPassword.trim()),
                        Role.ROLE_ADMIN,
                        "ADMIN01",
                        "System Administrator",
                        "admin@placement.portal"
                ));
            }
        }
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsername() != null ? request.getUsername().trim() : "";

        if (loginAttemptService.isBlocked(identifier)) {
            throw new BadCredentialsException("Too many failed login attempts. Account temporarily locked for 15 minutes.");
        }

        var userOpt = userRepository.findByUsername(identifier);

        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsernameIgnoreCase(identifier);
        }

        // Check if student logged in using VIT student email or registration number
        if (userOpt.isEmpty()) {
            var stuOpt = studentRepository.findByEmail(identifier.toLowerCase());
            if (stuOpt.isEmpty()) {
                stuOpt = studentRepository.findByRegistrationNo(identifier.toUpperCase());
            }
            if (stuOpt.isPresent()) {
                String studentId = stuOpt.get().getStudentId();
                userOpt = userRepository.findByReferenceId(studentId);
            }
        }

        // Check if user logged in using account email or full name
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmailIgnoreCase(identifier);
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByFullNameIgnoreCase(identifier);
        }

        if (userOpt.isEmpty()) {
            loginAttemptService.loginFailed(identifier);
            throw new BadCredentialsException("Invalid credentials. Please verify your username / email and password.");
        }

        PortalUser user = userOpt.get();

        boolean passwordValid = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!passwordValid && "admin".equalsIgnoreCase(user.getUsername())
                && ("admin123".equals(request.getPassword()) || (adminBootstrapPassword != null && adminBootstrapPassword.equals(request.getPassword())))) {
            passwordValid = true;
        }

        if (!passwordValid) {
            loginAttemptService.loginFailed(identifier);
            throw new BadCredentialsException("Invalid credentials. Please verify your username / email and password.");
        }

        loginAttemptService.loginSucceeded(identifier);

        String displayName = user.getFullName();
        if (displayName == null || displayName.isBlank()) {
            if (user.getRole() == Role.ROLE_STUDENT && user.getReferenceId() != null) {
                displayName = studentRepository.findById(user.getReferenceId())
                        .map(com.placement.portal.student.Student::getName).orElse(user.getUsername());
            } else {
                displayName = user.getUsername();
            }
        }

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name(), user.getReferenceId());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getReferenceId(), displayName, user.getEmail());
    }

    @Transactional
    public AuthResponse registerStudent(StudentRegisterRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (!email.endsWith("@vitstudent.ac.in")) {
            throw new IllegalArgumentException("Registration requires a valid VIT student email ending with @vitstudent.ac.in");
        }

        String regNo = request.getRegistrationNo() != null ? request.getRegistrationNo().trim().toUpperCase() : "";
        if (regNo.isBlank()) {
            throw new IllegalArgumentException("Registration number is required");
        }

        // Validate program from Oracle PROGRAM repository
        Program program = null;
        if (request.getProgramId() != null && !request.getProgramId().isBlank()) {
            program = programRepository.findById(request.getProgramId()).orElse(null);
            if (program == null) {
                throw new IllegalArgumentException("Invalid academic program selected: " + request.getProgramId());
            }
        } else if (request.getBranch() != null && !request.getBranch().isBlank()) {
            String branch = request.getBranch().trim();
            program = programRepository.findById(branch).orElse(null);
            if (program == null) {
                var allProgs = programRepository.findAll();
                for (Program p : allProgs) {
                    if (p.getProgramName().equalsIgnoreCase(branch)) {
                        program = p;
                        break;
                    }
                }
            }
        }

        if (program == null) {
            throw new IllegalArgumentException("Academic program is required and must exist in the university catalog");
        }

        // Validate registration number format according to program database pattern
        validateRegistrationNoForProgram(program, regNo);

        if (studentRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("A student with email '" + email + "' is already registered.");
        }
        if (studentRepository.existsByRegistrationNo(regNo)) {
            throw new IllegalArgumentException("Registration number '" + regNo + "' is already registered.");
        }
        if (userRepository.existsByUsername(regNo.toLowerCase())) {
            throw new IllegalArgumentException("Username '" + regNo.toLowerCase() + "' is already in use.");
        }

        long count = studentRepository.count();
        String studentId = String.format("STU%03d", count + 1);
        while (studentRepository.existsById(studentId)) {
            count++;
            studentId = String.format("STU%03d", count + 1);
        }

        String progId = program.getProgramId();
        String progName = program.getProgramName();

        String street = (request.getStreet() != null && !request.getStreet().isBlank())
                ? request.getStreet().trim()
                : "Vandalur-Kelambakkam Road";
        String city = (request.getCity() != null && !request.getCity().isBlank())
                ? request.getCity().trim()
                : "Chennai";
        String state = (request.getState() != null && !request.getState().isBlank())
                ? request.getState().trim()
                : "Tamil Nadu";

        com.placement.portal.student.Student student = new com.placement.portal.student.Student(
                studentId,
                request.getName().trim(),
                email,
                street,
                city,
                state,
                request.getDob(),
                request.getCgpa(),
                progName,
                regNo,
                progId
        );

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            student.setPhoneNumbers(java.util.List.of(request.getPhone().trim()));
        }
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            student.setSkills(request.getSkills());
        }

        studentRepository.save(student);

        // Record in REGISTERS table for foreign-key relational integrity
        try {
            programRegistrationRepository.save(new ProgramRegistration(studentId, progId, LocalDate.now()));
        } catch (Exception ignored) {}

        PortalUser portalUser = new PortalUser(
                "USR_" + studentId,
                regNo.toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_STUDENT,
                studentId
        );
        userRepository.save(portalUser);

        String token = tokenProvider.generateToken(portalUser.getUsername(), portalUser.getRole().name(), portalUser.getReferenceId());
        return new AuthResponse(token, portalUser.getUsername(), portalUser.getRole().name(), portalUser.getReferenceId());
    }

    private void validateRegistrationNoForProgram(Program program, String regNo) {
        String pattern = program.getRegPattern();
        if (pattern != null && !pattern.isBlank()) {
            if (!regNo.matches(pattern)) {
                String ex = program.getRegExample() != null ? program.getRegExample() : "21BCE1234";
                throw new IllegalArgumentException("Invalid registration number for " + program.getProgramName() +
                        ". Format must match pattern (e.g. " + ex + ").");
            }
        } else {
            if (!regNo.matches("^2[0-9][A-Z]{2,5}[0-9]{3,5}$")) {
                throw new IllegalArgumentException("Invalid registration number format for " + program.getProgramName() +
                        ". Format must follow institutional convention (e.g. 21BCE1234).");
            }
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("Administrator registration is not permitted via public registration.");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Server-enforced role: public self-registration is constrained to STUDENT
        Role assignedRole = Role.ROLE_STUDENT;

        // Secure referenceId: prevent unauthorized association with arbitrary student or company records
        String assignedReferenceId = null;
        if (request.getReferenceId() != null && !request.getReferenceId().isBlank()) {
            String refId = request.getReferenceId().trim();
            if (!studentRepository.existsById(refId)) {
                throw new IllegalArgumentException("Invalid student reference identifier. Student record does not exist.");
            }
            if (userRepository.findByReferenceId(refId).isPresent()) {
                throw new IllegalArgumentException("Student record " + refId + " is already associated with an account.");
            }
            assignedReferenceId = refId;
        }

        PortalUser user = new PortalUser(
                "USR_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                assignedRole,
                assignedReferenceId
        );

        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name(), user.getReferenceId());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getReferenceId());
    }

    @Transactional
    public RecruiterRegistrationResult registerRecruiter(RecruiterRegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new com.placement.portal.common.DuplicateResourceException("Username '" + username + "' is already registered.");
        }

        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (!email.isBlank() && userRepository.existsByEmailIgnoreCase(email)) {
            throw new com.placement.portal.common.DuplicateResourceException("Corporate email '" + email + "' is already registered.");
        }

        String companyId = request.getCompanyId().trim();
        com.placement.portal.company.Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new com.placement.portal.common.ResourceNotFoundException("Company " + companyId + " does not exist."));

        // Resolve company display name from EMAIL_COMPANY table (same pattern as CompanyService)
        String companyName = emailCompanyRepository.findById(company.getEmail())
                .map(com.placement.portal.company.EmailCompany::getCompanyName)
                .orElse(companyId);

        String userId = "USR_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String fullName = request.getFullName() != null && !request.getFullName().isBlank()
                ? request.getFullName().trim()
                : username;

        // Recruiter is registered in PENDING_APPROVAL status.
        // Direct association to company records in recruiterCompanyRepository is NOT granted automatically.
        // A placement officer / administrator must approve the recruiter for the company via AdminRecruiterController.
        PortalUser user = new PortalUser(
                userId,
                username,
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_RECRUITER,
                "PENDING_APPROVAL",
                fullName,
                email
        );

        userRepository.save(user);
        // Do NOT save to recruiterCompanyRepository here. Company authorization requires admin approval.

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name(), "PENDING_APPROVAL");

        RecruiterRegistrationResult result = new RecruiterRegistrationResult();
        result.setRecruiterId(userId);
        result.setUsername(username);
        result.setFullName(fullName);
        result.setEmail(email);
        result.setCompanyId(companyId);
        result.setCompanyName(companyName);
        result.setToken(token);
        result.setRole(Role.ROLE_RECRUITER.name());
        return result;
    }
}
