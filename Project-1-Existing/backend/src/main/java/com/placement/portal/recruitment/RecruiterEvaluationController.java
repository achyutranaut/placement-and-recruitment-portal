package com.placement.portal.recruitment;

import com.placement.portal.application.Application;
import com.placement.portal.application.ApplicationRepository;
import com.placement.portal.application.DailyRoutineRepository;
import com.placement.portal.application.StudentDailyRoutineDrive;
import com.placement.portal.auth.PortalUser;
import com.placement.portal.auth.Role;
import com.placement.portal.auth.UserRepository;
import com.placement.portal.common.ApiResponse;
import com.placement.portal.company.CompanyDto;
import com.placement.portal.company.CompanyRepository;
import com.placement.portal.company.CompanyService;
import com.placement.portal.company.EmailCompany;
import com.placement.portal.company.EmailCompanyRepository;
import com.placement.portal.company.JobCompanyRepository;
import com.placement.portal.config.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/recruiter")
@Tag(name = "Recruiter Operations", description = "Candidate resume evaluation and job role management")
public class RecruiterEvaluationController {

    private final ResumeEvaluationRepository evaluationRepository;
    private final PlacementDriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;
    private final DailyRoutineRepository dailyRoutineRepository;
    private final JobCompanyRepository jobCompanyRepository;
    private final CompanyRepository companyRepository;
    private final EmailCompanyRepository emailCompanyRepository;
    private final CompanyService companyService;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    public RecruiterEvaluationController(
            ResumeEvaluationRepository evaluationRepository,
            PlacementDriveRepository driveRepository,
            ApplicationRepository applicationRepository,
            DailyRoutineRepository dailyRoutineRepository,
            JobCompanyRepository jobCompanyRepository,
            CompanyRepository companyRepository,
            EmailCompanyRepository emailCompanyRepository,
            CompanyService companyService,
            UserRepository userRepository,
            JwtTokenProvider tokenProvider
    ) {
        this.evaluationRepository = evaluationRepository;
        this.driveRepository = driveRepository;
        this.applicationRepository = applicationRepository;
        this.dailyRoutineRepository = dailyRoutineRepository;
        this.jobCompanyRepository = jobCompanyRepository;
        this.companyRepository = companyRepository;
        this.emailCompanyRepository = emailCompanyRepository;
        this.companyService = companyService;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    @GetMapping("/companies")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Get companies authorized for authenticated recruiter")
    public ResponseEntity<ApiResponse<List<CompanyDto>>> getAuthorizedCompanies(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return ResponseEntity.ok(ApiResponse.ok(companyService.getAuthorizedCompanies(principal.getName())));
    }

    @PostMapping("/company/select")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Establish active company context for recruiter session")
    public ResponseEntity<ApiResponse<Map<String, Object>>> selectActiveCompany(
            @RequestBody Map<String, String> payload,
            Principal principal
    ) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        String companyId = payload.get("companyId");
        if (companyId == null || companyId.isBlank()) {
            throw new IllegalArgumentException("Company ID is required");
        }

        if (!companyService.isRecruiterAuthorizedForCompany(principal.getName(), companyId)) {
            throw new AccessDeniedException("Access denied: You are not authorized to represent company " + companyId);
        }

        CompanyDto company = companyService.getCompanyById(companyId);
        PortalUser user = userRepository.findByUsernameIgnoreCase(principal.getName()).orElse(null);
        String roleName = user != null ? user.getRole().name() : Role.ROLE_RECRUITER.name();
        String refreshedToken = tokenProvider.generateToken(principal.getName(), roleName, companyId);

        return ResponseEntity.ok(ApiResponse.ok("Company context established for " + company.getCompanyName(), Map.of(
                "token", refreshedToken,
                "company", company
        )));
    }

    @PostMapping("/evaluations")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Record structured resume evaluation for candidate")
    @Transactional
    public ResponseEntity<ApiResponse<ResumeEvaluationDto>> recordEvaluation(
            @RequestBody ResumeEvaluationDto dto,
            Principal principal
    ) {
        if (principal != null) {
            PortalUser user = userRepository.findByUsernameIgnoreCase(principal.getName()).orElse(null);
            if (user != null && user.getRole() == Role.ROLE_RECRUITER) {
                var appOpt = applicationRepository.findById(dto.getApplicationId());
                if (appOpt.isEmpty()) {
                    throw new IllegalArgumentException("Application not found: " + dto.getApplicationId());
                }
                var app = appOpt.get();
                String driveId = app.getDriveId();
                if (driveId == null || driveId.isBlank()) {
                    var routineOpt = dailyRoutineRepository.findByStudentIdAndApplyDate(app.getStudentId(), app.getApplyDate());
                    if (routineOpt.isPresent()) {
                        driveId = routineOpt.get().getDriveId();
                    }
                }
                if (driveId != null) {
                    var drvOpt = driveRepository.findById(driveId);
                    if (drvOpt.isPresent()) {
                        var jcOpt = jobCompanyRepository.findById(drvOpt.get().getJobTitle());
                        if (jcOpt.isPresent()) {
                            String driveCompId = jcOpt.get().getCompanyId();
                            if (!companyService.isRecruiterAuthorizedForCompany(principal.getName(), driveCompId)) {
                                throw new AccessDeniedException("Access denied: You may only evaluate candidates for drives belonging to your authorized company (" + driveCompId + ").");
                            }
                        }
                    }
                }
            }
        }

        String evalId = dto.getEvaluationId() != null && !dto.getEvaluationId().isEmpty()
                ? dto.getEvaluationId()
                : "EVAL_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ResumeEvaluation eval = new ResumeEvaluation(
                evalId,
                dto.getResumeId(),
                dto.getApplicationId(),
                dto.getRecruiterId() != null ? dto.getRecruiterId() : (principal != null ? principal.getName() : "RECRUITER"),
                dto.getTechnicalScore(),
                dto.getEducationScore(),
                dto.getProjectScore(),
                dto.getExperienceScore(),
                dto.getOverallScore(),
                dto.getComments()
        );

        ResumeEvaluation saved = evaluationRepository.save(eval);
        return ResponseEntity.ok(ApiResponse.ok("Candidate evaluation saved successfully", ResumeEvaluationDto.fromEntity(saved)));
    }

    @GetMapping("/evaluations/application/{applicationId}")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Get evaluations for application")
    public ResponseEntity<ApiResponse<List<ResumeEvaluationDto>>> getEvaluationsByApplication(@PathVariable String applicationId) {
        List<ResumeEvaluationDto> dtos = evaluationRepository.findByApplicationIdOrderByEvaluatedAtDesc(applicationId)
                .stream().map(ResumeEvaluationDto::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('RECRUITER', 'ADMIN')")
    @Operation(summary = "Get recruiter placement roles with stage applicant counts")
    public ResponseEntity<ApiResponse<List<RecruiterRoleStatDto>>> getRecruiterRoleStats(
            @RequestParam(value = "companyId", required = false) String companyId,
            Principal principal
    ) {
        Set<String> allowedCompanyIds = null;
        if (principal != null) {
            PortalUser user = userRepository.findByUsernameIgnoreCase(principal.getName()).orElse(null);
            if (user != null && user.getRole() == Role.ROLE_RECRUITER) {
                if (companyId != null && !companyId.isBlank() && !"ALL".equalsIgnoreCase(companyId)) {
                    if (!companyService.isRecruiterAuthorizedForCompany(principal.getName(), companyId)) {
                        throw new AccessDeniedException("Access denied: You are not authorized to view recruitment data for company " + companyId);
                    }
                    allowedCompanyIds = Set.of(companyId.toUpperCase());
                } else {
                    allowedCompanyIds = companyService.getAuthorizedCompanies(principal.getName()).stream()
                            .map(CompanyDto::getCompanyId)
                            .map(String::toUpperCase)
                            .collect(Collectors.toSet());
                }
            }
        }

        List<PlacementDrive> drives = driveRepository.findAll();
        List<RecruiterRoleStatDto> roleStats = new ArrayList<>();

        for (PlacementDrive d : drives) {
            String compId = "";
            String compName = "Corporate Recruiter";

            var jobOpt = jobCompanyRepository.findById(d.getJobTitle());
            if (jobOpt.isPresent()) {
                compId = jobOpt.get().getCompanyId();
                var cOpt = companyRepository.findById(compId);
                if (cOpt.isPresent()) {
                    compName = emailCompanyRepository.findById(cOpt.get().getEmail())
                            .map(EmailCompany::getCompanyName).orElse(compName);
                }
            }

            if (allowedCompanyIds != null && !allowedCompanyIds.contains(compId.toUpperCase())) {
                continue;
            }

            if (companyId != null && !companyId.isEmpty() && !"ALL".equalsIgnoreCase(companyId) && !compId.equalsIgnoreCase(companyId)) {
                continue;
            }

            // Find applications for this drive
            Set<String> routineStudentIds = dailyRoutineRepository.findByDriveId(d.getDriveId()).stream()
                    .map(StudentDailyRoutineDrive::getStudentId)
                    .collect(Collectors.toSet());

            List<Application> driveApps = applicationRepository.findAll().stream()
                    .filter(a -> routineStudentIds.contains(a.getStudentId()) || d.getDriveId().equalsIgnoreCase(a.getDriveId()))
                    .collect(Collectors.toList());

            long total = driveApps.size();
            long shortlisted = driveApps.stream().filter(a -> "SHORTLISTED".equalsIgnoreCase(a.getStatus())).count();
            long interviewing = driveApps.stream().filter(a -> "INTERVIEWING".equalsIgnoreCase(a.getStatus())).count();
            long selected = driveApps.stream().filter(a -> "SELECTED".equalsIgnoreCase(a.getStatus())).count();
            long offered = driveApps.stream().filter(a -> "OFFERED".equalsIgnoreCase(a.getStatus())).count();
            long rejected = driveApps.stream().filter(a -> "REJECTED".equalsIgnoreCase(a.getStatus())).count();

            roleStats.add(new RecruiterRoleStatDto(
                    d.getDriveId(),
                    d.getJobTitle(),
                    compId,
                    compName,
                    d.getPackageLpa(),
                    d.getMinCgpa(),
                    d.getOpenings(),
                    d.getDriveDate(),
                    d.getApplicationDeadline(),
                    total,
                    shortlisted,
                    interviewing,
                    selected,
                    offered,
                    rejected
            ));
        }

        return ResponseEntity.ok(ApiResponse.ok(roleStats));
    }
}
