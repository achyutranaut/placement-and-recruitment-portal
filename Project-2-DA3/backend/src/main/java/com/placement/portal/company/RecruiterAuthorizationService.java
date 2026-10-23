package com.placement.portal.company;

import com.placement.portal.application.Application;
import com.placement.portal.application.ApplicationRepository;
import com.placement.portal.application.DailyRoutineRepository;
import com.placement.portal.application.StudentDailyRoutineDrive;
import com.placement.portal.auth.PortalUser;
import com.placement.portal.auth.Role;
import com.placement.portal.auth.UserRepository;
import com.placement.portal.recruitment.PlacementDrive;
import com.placement.portal.recruitment.PlacementDriveRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecruiterAuthorizationService {

    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final RecruiterCompanyRepository recruiterCompanyRepository;
    private final PlacementDriveRepository driveRepository;
    private final JobCompanyRepository jobCompanyRepository;
    private final ApplicationRepository applicationRepository;
    private final DailyRoutineRepository dailyRoutineRepository;

    public RecruiterAuthorizationService(
            UserRepository userRepository,
            CompanyService companyService,
            RecruiterCompanyRepository recruiterCompanyRepository,
            PlacementDriveRepository driveRepository,
            JobCompanyRepository jobCompanyRepository,
            ApplicationRepository applicationRepository,
            DailyRoutineRepository dailyRoutineRepository
    ) {
        this.userRepository = userRepository;
        this.companyService = companyService;
        this.recruiterCompanyRepository = recruiterCompanyRepository;
        this.driveRepository = driveRepository;
        this.jobCompanyRepository = jobCompanyRepository;
        this.applicationRepository = applicationRepository;
        this.dailyRoutineRepository = dailyRoutineRepository;
    }

    public boolean isAuthorizedForCompany(Principal principal, String companyId) {
        if (principal == null) return false;
        Optional<PortalUser> userOpt = userRepository.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return false;
        PortalUser user = userOpt.get();
        if (user.getRole() == Role.ROLE_ADMIN) return true;
        if (user.getRole() != Role.ROLE_RECRUITER) return false;
        return companyService.isRecruiterAuthorizedForCompany(principal.getName(), companyId);
    }

    public void requireAuthorizedForCompany(Principal principal, String companyId) {
        if (!isAuthorizedForCompany(principal, companyId)) {
            throw new AccessDeniedException("Access denied: You are not authorized for company " + companyId + ".");
        }
    }

    public String resolveCompanyIdForDrive(String driveId) {
        if (driveId == null || driveId.isBlank()) return null;
        return driveRepository.findById(driveId)
                .flatMap(drv -> jobCompanyRepository.findById(drv.getJobTitle()))
                .map(JobCompany::getCompanyId)
                .orElse(null);
    }

    public boolean isAuthorizedForDrive(Principal principal, String driveId) {
        if (principal == null) return false;
        Optional<PortalUser> userOpt = userRepository.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return false;
        PortalUser user = userOpt.get();
        if (user.getRole() == Role.ROLE_ADMIN) return true;
        if (user.getRole() != Role.ROLE_RECRUITER) return false;

        String companyId = resolveCompanyIdForDrive(driveId);
        if (companyId == null) return false;
        return companyService.isRecruiterAuthorizedForCompany(principal.getName(), companyId);
    }

    public void requireAuthorizedForDrive(Principal principal, String driveId) {
        if (!isAuthorizedForDrive(principal, driveId)) {
            String companyId = resolveCompanyIdForDrive(driveId);
            throw new AccessDeniedException("Access denied: You are not authorized for recruitment drive " + driveId
                    + (companyId != null ? " (Company: " + companyId + ")" : ""));
        }
    }

    public String resolveDriveIdForApplication(Application app) {
        if (app == null) return null;
        if (app.getDriveId() != null && !app.getDriveId().isBlank()) {
            return app.getDriveId();
        }
        return dailyRoutineRepository.findByStudentIdAndApplyDate(app.getStudentId(), app.getApplyDate())
                .map(StudentDailyRoutineDrive::getDriveId)
                .orElse(null);
    }

    public boolean isAuthorizedForApplication(Principal principal, String applicationId) {
        if (principal == null) return false;
        Optional<PortalUser> userOpt = userRepository.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return false;
        PortalUser user = userOpt.get();
        if (user.getRole() == Role.ROLE_ADMIN) return true;
        if (user.getRole() != Role.ROLE_RECRUITER) return false;

        Application app = applicationRepository.findById(applicationId).orElse(null);
        if (app == null) return false;

        String driveId = resolveDriveIdForApplication(app);
        if (driveId == null) return false;

        return isAuthorizedForDrive(principal, driveId);
    }

    public void requireAuthorizedForApplication(Principal principal, String applicationId) {
        if (!isAuthorizedForApplication(principal, applicationId)) {
            throw new AccessDeniedException("Access denied: You are not authorized to view or modify application " + applicationId + ".");
        }
    }

    public Set<String> getAuthorizedCompanyIds(Principal principal) {
        if (principal == null) return Collections.emptySet();
        Optional<PortalUser> userOpt = userRepository.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return Collections.emptySet();
        PortalUser user = userOpt.get();
        if (user.getRole() == Role.ROLE_ADMIN) {
            return Collections.emptySet(); // empty set means unrestricted for admin
        }

        Set<String> companyIds = recruiterCompanyRepository.findByUserId(user.getUserId()).stream()
                .map(RecruiterCompany::getCompanyId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (user.getReferenceId() != null && !user.getReferenceId().isBlank() && !"ALL".equalsIgnoreCase(user.getReferenceId())) {
            companyIds.add(user.getReferenceId());
        }
        return companyIds;
    }

    public Set<String> getAuthorizedDriveIds(Principal principal) {
        Set<String> compIds = getAuthorizedCompanyIds(principal);
        if (compIds.isEmpty()) return Collections.emptySet();

        Set<String> jobTitles = new HashSet<>();
        for (String cId : compIds) {
            jobCompanyRepository.findAll().stream()
                    .filter(jc -> cId.equalsIgnoreCase(jc.getCompanyId()))
                    .map(JobCompany::getJobTitle)
                    .forEach(jobTitles::add);
        }

        return driveRepository.findAll().stream()
                .filter(drv -> jobTitles.contains(drv.getJobTitle()))
                .map(PlacementDrive::getDriveId)
                .collect(Collectors.toSet());
    }

    public boolean isAuthorizedForStudent(Principal principal, String studentId) {
        if (principal == null || studentId == null) return false;
        Optional<PortalUser> userOpt = userRepository.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return false;
        PortalUser user = userOpt.get();
        if (user.getRole() == Role.ROLE_ADMIN) return true;
        if (user.getRole() != Role.ROLE_RECRUITER) return false;

        Set<String> authorizedDrives = getAuthorizedDriveIds(principal);
        if (authorizedDrives.isEmpty()) return false;

        List<Application> studentApps = applicationRepository.findByStudentId(studentId);
        for (Application app : studentApps) {
            String driveId = resolveDriveIdForApplication(app);
            if (driveId != null && authorizedDrives.contains(driveId)) {
                return true;
            }
        }
        return false;
    }

    public void requireAuthorizedForStudent(Principal principal, String studentId) {
        if (!isAuthorizedForStudent(principal, studentId)) {
            throw new AccessDeniedException("Access denied: You may only access candidate information for applicants to your company's drives.");
        }
    }
}
