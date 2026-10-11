package com.placement.portal.recruitment;

import com.placement.portal.application.Application;
import com.placement.portal.application.ApplicationRepository;
import com.placement.portal.application.DailyRoutineRepository;
import com.placement.portal.application.StudentDailyRoutineDrive;
import com.placement.portal.auth.PortalUser;
import com.placement.portal.auth.Role;
import com.placement.portal.auth.UserRepository;
import com.placement.portal.company.*;
import com.placement.portal.config.JwtTokenProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile({"oracle", "h2", "default", "!neo4j"})
public class JpaRecruiterEvaluationService implements IRecruiterEvaluationService {

    private final ResumeEvaluationRepository evaluationRepository;
    private final PlacementDriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;
    private final DailyRoutineRepository dailyRoutineRepository;
    private final JobCompanyRepository jobCompanyRepository;
    private final CompanyRepository companyRepository;
    private final EmailCompanyRepository emailCompanyRepository;
    private final ICompanyService companyService;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    public JpaRecruiterEvaluationService(
            ResumeEvaluationRepository evaluationRepository,
            PlacementDriveRepository driveRepository,
            ApplicationRepository applicationRepository,
            DailyRoutineRepository dailyRoutineRepository,
            JobCompanyRepository jobCompanyRepository,
            CompanyRepository companyRepository,
            EmailCompanyRepository emailCompanyRepository,
            ICompanyService companyService,
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

    @Override
    public List<CompanyDto> getAuthorizedCompanies(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return companyService.getAuthorizedCompanies(principal.getName());
    }

    @Override
    public Map<String, Object> selectActiveCompany(String companyId, Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
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

        return Map.of(
                "token", refreshedToken,
                "company", company
        );
    }

    @Override
    @Transactional
    public ResumeEvaluationDto recordEvaluation(ResumeEvaluationDto dto, Principal principal) {
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
        return ResumeEvaluationDto.fromEntity(saved);
    }

    @Override
    public List<ResumeEvaluationDto> getEvaluationsByApplication(String applicationId) {
        return evaluationRepository.findByApplicationIdOrderByEvaluatedAtDesc(applicationId)
                .stream().map(ResumeEvaluationDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<RecruiterRoleStatDto> getRecruiterRoleStats(String companyId, Principal principal) {
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

        return roleStats;
    }
}
