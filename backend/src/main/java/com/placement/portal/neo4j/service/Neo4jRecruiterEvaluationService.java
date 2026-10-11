package com.placement.portal.neo4j.service;

import com.placement.portal.auth.Role;
import com.placement.portal.company.CompanyDto;
import com.placement.portal.company.ICompanyService;
import com.placement.portal.config.JwtTokenProvider;
import com.placement.portal.neo4j.model.*;
import com.placement.portal.neo4j.repository.*;
import com.placement.portal.recruitment.IRecruiterEvaluationService;
import com.placement.portal.recruitment.RecruiterRoleStatDto;
import com.placement.portal.recruitment.ResumeEvaluationDto;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("neo4j")
@Primary
public class Neo4jRecruiterEvaluationService implements IRecruiterEvaluationService {

    private final Neo4jResumeEvaluationRepository evaluationRepository;
    private final Neo4jDriveRepository driveRepository;
    private final Neo4jApplicationRepository applicationRepository;
    private final Neo4jCompanyRepository companyRepository;
    private final Neo4jUserRepository userRepository;
    private final ICompanyService companyService;
    private final JwtTokenProvider tokenProvider;

    public Neo4jRecruiterEvaluationService(
            Neo4jResumeEvaluationRepository evaluationRepository,
            Neo4jDriveRepository driveRepository,
            Neo4jApplicationRepository applicationRepository,
            Neo4jCompanyRepository companyRepository,
            Neo4jUserRepository userRepository,
            ICompanyService companyService,
            JwtTokenProvider tokenProvider
    ) {
        this.evaluationRepository = evaluationRepository;
        this.driveRepository = driveRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.companyService = companyService;
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
        PortalUserNode user = userRepository.findByUsername(principal.getName()).orElse(null);
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
            PortalUserNode user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user != null && user.getRole() == Role.ROLE_RECRUITER) {
                ApplicationNode app = applicationRepository.findByApplicationId(dto.getApplicationId())
                        .orElseThrow(() -> new IllegalArgumentException("Application not found: " + dto.getApplicationId()));
                String driveId = app.getDriveId();
                if (driveId != null) {
                    DriveNode drive = driveRepository.findByDriveId(driveId).orElse(null);
                    if (drive != null && drive.getCompanyId() != null) {
                        if (!companyService.isRecruiterAuthorizedForCompany(principal.getName(), drive.getCompanyId())) {
                            throw new AccessDeniedException("Access denied: You may only evaluate candidates for drives belonging to your authorized company (" + drive.getCompanyId() + ").");
                        }
                    }
                }
            }
        }

        String evalId = dto.getEvaluationId() != null && !dto.getEvaluationId().isEmpty()
                ? dto.getEvaluationId()
                : "EVAL_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ResumeEvaluationNode eval = new ResumeEvaluationNode(
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

        ResumeEvaluationNode saved = evaluationRepository.save(eval);
        return toDto(saved);
    }

    @Override
    public List<ResumeEvaluationDto> getEvaluationsByApplication(String applicationId) {
        return evaluationRepository.findByApplicationIdOrderByEvaluatedAtDesc(applicationId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecruiterRoleStatDto> getRecruiterRoleStats(String companyId, Principal principal) {
        Set<String> allowedCompanyIds = null;
        if (principal != null) {
            PortalUserNode user = userRepository.findByUsername(principal.getName()).orElse(null);
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

        List<DriveNode> drives = driveRepository.findAllOrdered();
        List<RecruiterRoleStatDto> roleStats = new ArrayList<>();

        for (DriveNode d : drives) {
            String compId = d.getCompanyId() != null ? d.getCompanyId() : "";
            String compName = "Corporate Recruiter";

            if (!compId.isBlank()) {
                var cOpt = companyRepository.findByCompanyId(compId);
                if (cOpt.isPresent() && cOpt.get().getCompanyName() != null) {
                    compName = cOpt.get().getCompanyName();
                }
            }

            if (allowedCompanyIds != null && !allowedCompanyIds.contains(compId.toUpperCase())) {
                continue;
            }

            if (companyId != null && !companyId.isEmpty() && !"ALL".equalsIgnoreCase(companyId) && !compId.equalsIgnoreCase(companyId)) {
                continue;
            }

            List<ApplicationNode> driveApps = applicationRepository.findByDriveId(d.getDriveId());

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
                    d.getCtc(),
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

    private ResumeEvaluationDto toDto(ResumeEvaluationNode e) {
        return new ResumeEvaluationDto(
                e.getEvaluationId(),
                e.getResumeId(),
                e.getApplicationId(),
                e.getRecruiterId(),
                e.getTechnicalScore(),
                e.getEducationScore(),
                e.getProjectScore(),
                e.getExperienceScore(),
                e.getOverallScore(),
                e.getComments(),
                e.getEvaluatedAt()
        );
    }
}
