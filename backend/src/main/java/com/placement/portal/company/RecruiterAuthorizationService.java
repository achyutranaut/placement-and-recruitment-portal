package com.placement.portal.company;

import com.placement.portal.application.ApplicationDto;
import com.placement.portal.application.IApplicationService;
import com.placement.portal.auth.IUserContextService;
import com.placement.portal.auth.PortalUserDto;
import com.placement.portal.auth.Role;
import com.placement.portal.recruitment.IPlacementDriveService;
import com.placement.portal.recruitment.PlacementDriveDto;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecruiterAuthorizationService {

    private final IUserContextService userContextService;
    private final ICompanyService companyService;
    private final IPlacementDriveService driveService;
    private final IApplicationService applicationService;

    public RecruiterAuthorizationService(
            IUserContextService userContextService,
            ICompanyService companyService,
            @Lazy IPlacementDriveService driveService,
            @Lazy IApplicationService applicationService
    ) {
        this.userContextService = userContextService;
        this.companyService = companyService;
        this.driveService = driveService;
        this.applicationService = applicationService;
    }

    public boolean isAuthorizedForCompany(Principal principal, String companyId) {
        if (principal == null) return false;
        Optional<PortalUserDto> userOpt = userContextService.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return false;
        PortalUserDto user = userOpt.get();
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
        try {
            PlacementDriveDto drive = driveService.getDriveById(driveId);
            return drive != null ? drive.getCompanyId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isAuthorizedForDrive(Principal principal, String driveId) {
        if (principal == null) return false;
        Optional<PortalUserDto> userOpt = userContextService.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return false;
        PortalUserDto user = userOpt.get();
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

    public boolean isAuthorizedForApplication(Principal principal, String applicationId) {
        if (principal == null) return false;
        Optional<PortalUserDto> userOpt = userContextService.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return false;
        PortalUserDto user = userOpt.get();
        if (user.getRole() == Role.ROLE_ADMIN) return true;
        if (user.getRole() != Role.ROLE_RECRUITER) return false;

        try {
            ApplicationDto app = applicationService.getApplicationById(applicationId);
            if (app == null || app.getDriveId() == null) return false;
            return isAuthorizedForDrive(principal, app.getDriveId());
        } catch (Exception e) {
            return false;
        }
    }

    public void requireAuthorizedForApplication(Principal principal, String applicationId) {
        if (!isAuthorizedForApplication(principal, applicationId)) {
            throw new AccessDeniedException("Access denied: You are not authorized to view or modify application " + applicationId + ".");
        }
    }

    public Set<String> getAuthorizedCompanyIds(Principal principal) {
        if (principal == null) return Collections.emptySet();
        Optional<PortalUserDto> userOpt = userContextService.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return Collections.emptySet();
        PortalUserDto user = userOpt.get();
        if (user.getRole() == Role.ROLE_ADMIN) {
            return Collections.emptySet(); // empty set means unrestricted for admin
        }

        return companyService.getAuthorizedCompanies(principal.getName()).stream()
                .map(CompanyDto::getCompanyId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<String> getAuthorizedDriveIds(Principal principal) {
        Set<String> compIds = getAuthorizedCompanyIds(principal);
        if (compIds.isEmpty()) return Collections.emptySet();

        return driveService.getAllDrives().stream()
                .filter(d -> d.getCompanyId() != null && compIds.contains(d.getCompanyId()))
                .map(PlacementDriveDto::getDriveId)
                .collect(Collectors.toSet());
    }

    public boolean isAuthorizedForStudent(Principal principal, String studentId) {
        if (principal == null || studentId == null) return false;
        Optional<PortalUserDto> userOpt = userContextService.findByUsername(principal.getName());
        if (userOpt.isEmpty()) return false;
        PortalUserDto user = userOpt.get();
        if (user.getRole() == Role.ROLE_ADMIN) return true;
        if (user.getRole() != Role.ROLE_RECRUITER) return false;

        Set<String> authorizedDrives = getAuthorizedDriveIds(principal);
        if (authorizedDrives.isEmpty()) return false;

        List<ApplicationDto> studentApps = applicationService.getApplicationsByStudent(studentId);
        for (ApplicationDto app : studentApps) {
            if (app.getDriveId() != null && authorizedDrives.contains(app.getDriveId())) {
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
