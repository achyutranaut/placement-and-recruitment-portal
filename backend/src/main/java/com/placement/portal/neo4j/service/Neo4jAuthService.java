package com.placement.portal.neo4j.service;

import com.placement.portal.auth.*;
import com.placement.portal.common.DuplicateResourceException;
import com.placement.portal.common.ResourceNotFoundException;
import com.placement.portal.config.JwtTokenProvider;
import com.placement.portal.neo4j.model.CompanyNode;
import com.placement.portal.neo4j.model.PortalUserNode;
import com.placement.portal.neo4j.model.ProgramNode;
import com.placement.portal.neo4j.model.StudentNode;
import com.placement.portal.neo4j.repository.Neo4jCompanyRepository;
import com.placement.portal.neo4j.repository.Neo4jProgramRepository;
import com.placement.portal.neo4j.repository.Neo4jStudentRepository;
import com.placement.portal.neo4j.repository.Neo4jUserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Profile("neo4j")
@Primary
public class Neo4jAuthService implements IAuthService {

    private final Neo4jUserRepository userRepository;
    private final Neo4jStudentRepository studentRepository;
    private final Neo4jProgramRepository programRepository;
    private final Neo4jCompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public Neo4jAuthService(
            Neo4jUserRepository userRepository,
            Neo4jStudentRepository studentRepository,
            Neo4jProgramRepository programRepository,
            Neo4jCompanyRepository companyRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider
    ) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsername() != null ? request.getUsername().trim() : "";
        Optional<PortalUserNode> userOpt = userRepository.findByUsername(identifier);

        if (userOpt.isEmpty()) {
            userOpt = userRepository.findAll().stream()
                    .filter(u -> identifier.equalsIgnoreCase(u.getUsername()))
                    .findFirst();
        }

        // Direct referenceId lookup (e.g. STU023, STU002, ADMIN01, COM004)
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findAll().stream()
                    .filter(u -> u.getReferenceId() != null && identifier.equalsIgnoreCase(u.getReferenceId()))
                    .findFirst();
        }

        // Check if student logged in using studentId, VIT student email, or registration number
        if (userOpt.isEmpty()) {
            var stuOpt = studentRepository.findByStudentId(identifier.toUpperCase());
            if (stuOpt.isEmpty()) {
                stuOpt = studentRepository.findByEmail(identifier.toLowerCase());
            }
            if (stuOpt.isEmpty()) {
                stuOpt = studentRepository.findByRegistrationNo(identifier.toUpperCase());
            }
            if (stuOpt.isPresent()) {
                String studentId = stuOpt.get().getStudentId();
                userOpt = userRepository.findAll().stream()
                        .filter(u -> studentId.equalsIgnoreCase(u.getReferenceId()))
                        .findFirst();
            }
        }

        // Check if user logged in using account email or full name
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findAll().stream()
                    .filter(u -> (u.getEmail() != null && identifier.equalsIgnoreCase(u.getEmail()))
                            || (u.getFullName() != null && identifier.equalsIgnoreCase(u.getFullName())))
                    .findFirst();
        }

        PortalUserNode user = userOpt.orElseThrow(() -> new BadCredentialsException("Invalid credentials. Please verify your username / email and password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials. Please verify your username / email and password.");
        }

        String displayName = user.getFullName();
        if (displayName == null || displayName.isBlank()) {
            if (user.getRole() == Role.ROLE_STUDENT && user.getReferenceId() != null) {
                displayName = studentRepository.findByStudentId(user.getReferenceId())
                        .map(StudentNode::getName).orElse(user.getUsername());
            } else {
                displayName = user.getUsername();
            }
        }

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name(), user.getReferenceId());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getReferenceId(), displayName, user.getEmail());
    }

    @Override
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

        ProgramNode program = null;
        if (request.getProgramId() != null && !request.getProgramId().isBlank()) {
            program = programRepository.findByProgramId(request.getProgramId()).orElse(null);
            if (program == null) {
                throw new IllegalArgumentException("Invalid academic program selected: " + request.getProgramId());
            }
        } else if (request.getBranch() != null && !request.getBranch().isBlank()) {
            String branch = request.getBranch().trim();
            program = programRepository.findByProgramId(branch).orElse(null);
            if (program == null) {
                var allProgs = programRepository.findAll();
                for (ProgramNode p : allProgs) {
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

        validateRegistrationNoForProgram(program, regNo);

        if (studentRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("A student with email '" + email + "' is already registered.");
        }
        if (studentRepository.findByRegistrationNo(regNo).isPresent()) {
            throw new IllegalArgumentException("Registration number '" + regNo + "' is already registered.");
        }
        if (userRepository.findByUsername(regNo.toLowerCase()).isPresent()) {
            throw new IllegalArgumentException("Username '" + regNo.toLowerCase() + "' is already in use.");
        }

        long count = studentRepository.count();
        String studentId = String.format("STU%03d", count + 1);
        while (studentRepository.findByStudentId(studentId).isPresent()) {
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

        StudentNode student = new StudentNode(
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

        PortalUserNode portalUser = new PortalUserNode(
                "USR_" + studentId,
                regNo.toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_STUDENT,
                studentId,
                request.getName().trim(),
                email
        );
        userRepository.save(portalUser);

        String token = tokenProvider.generateToken(portalUser.getUsername(), portalUser.getRole().name(), portalUser.getReferenceId());
        return new AuthResponse(token, portalUser.getUsername(), portalUser.getRole().name(), portalUser.getReferenceId());
    }

    private void validateRegistrationNoForProgram(ProgramNode program, String regNo) {
        String pattern = program.getRegNoRegex();
        if (pattern != null && !pattern.isBlank()) {
            if (!regNo.matches(pattern)) {
                String ex = program.getRegNoSample() != null ? program.getRegNoSample() : "21BCE1234";
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

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        PortalUserNode user = new PortalUserNode(
                "USR_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getRole(),
                request.getReferenceId()
        );

        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name(), user.getReferenceId());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getReferenceId());
    }

    @Override
    @Transactional
    public RecruiterRegistrationResult registerRecruiter(RecruiterRegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.findAll().stream().anyMatch(u -> username.equalsIgnoreCase(u.getUsername()))) {
            throw new DuplicateResourceException("Username '" + username + "' is already registered.");
        }

        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (!email.isBlank() && userRepository.findAll().stream().anyMatch(u -> email.equalsIgnoreCase(u.getEmail()))) {
            throw new DuplicateResourceException("Corporate email '" + email + "' is already registered.");
        }

        String companyId = request.getCompanyId().trim();
        CompanyNode company = companyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company " + companyId + " does not exist."));

        String companyName = company.getCompanyName() != null ? company.getCompanyName() : companyId;

        String userId = "USR_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String fullName = request.getFullName() != null && !request.getFullName().isBlank()
                ? request.getFullName().trim()
                : username;

        PortalUserNode user = new PortalUserNode(
                userId,
                username,
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_RECRUITER,
                companyId,
                fullName,
                email
        );
        userRepository.save(user);
        userRepository.assignCompany(userId, companyId);

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name(), user.getReferenceId());

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
