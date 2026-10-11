package com.placement.portal.neo4j.service;

import com.placement.portal.company.CompanyDto;
import com.placement.portal.company.ICompanyService;
import com.placement.portal.company.JobDto;
import com.placement.portal.company.RecruiterAuthorizationDto;
import com.placement.portal.neo4j.model.CompanyNode;
import com.placement.portal.neo4j.model.DriveNode;
import com.placement.portal.neo4j.model.PortalUserNode;
import com.placement.portal.neo4j.repository.Neo4jCompanyRepository;
import com.placement.portal.neo4j.repository.Neo4jDriveRepository;
import com.placement.portal.neo4j.repository.Neo4jUserRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("neo4j")
@Primary
public class Neo4jCompanyService implements ICompanyService {

    private final Neo4jCompanyRepository companyRepository;
    private final Neo4jDriveRepository driveRepository;
    private final Neo4jUserRepository userRepository;

    public Neo4jCompanyService(
            Neo4jCompanyRepository companyRepository,
            Neo4jDriveRepository driveRepository,
            Neo4jUserRepository userRepository
    ) {
        this.companyRepository = companyRepository;
        this.driveRepository = driveRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<CompanyDto> getAllCompanies() {
        return companyRepository.findAllOrdered().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompanyDto getCompanyById(String companyId) {
        CompanyNode c = companyRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
        return toDto(c);
    }

    @Override
    public List<JobDto> getJobsByCompany(String companyId) {
        return driveRepository.findByCompanyId(companyId).stream()
                .map(d -> new JobDto(d.getJobTitle(), companyId))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<JobDto> getAllJobs() {
        return driveRepository.findAllOrdered().stream()
                .map(d -> new JobDto(d.getJobTitle(), d.getCompanyId() != null ? d.getCompanyId() : ""))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<CompanyDto> getAuthorizedCompanies(String username) {
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return List.of();
        }
        PortalUserNode user = userOpt.get();
        if (user.getRole() == com.placement.portal.auth.Role.ROLE_ADMIN) {
            return getAllCompanies();
        }

        List<CompanyNode> authorized = companyRepository.findAuthorizedCompaniesForUser(user.getUserId());
        if (authorized.isEmpty() && user.getReferenceId() != null && !"ALL".equalsIgnoreCase(user.getReferenceId())) {
            companyRepository.findByCompanyId(user.getReferenceId()).ifPresent(authorized::add);
        }

        return authorized.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public boolean isRecruiterAuthorizedForCompany(String username, String companyId) {
        if (companyId == null || companyId.isBlank() || "ALL".equalsIgnoreCase(companyId)) {
            return false;
        }

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }

        PortalUserNode user = userOpt.get();
        if (user.getRole() == com.placement.portal.auth.Role.ROLE_ADMIN) {
            return true;
        }

        if (companyId.equalsIgnoreCase(user.getReferenceId())) {
            return true;
        }

        return companyRepository.findAuthorizedCompaniesForUser(user.getUserId()).stream()
                .anyMatch(c -> c.getCompanyId().equalsIgnoreCase(companyId));
    }

    @Override
    @Transactional
    public void assignCompanyToRecruiter(String userId, String companyId) {
        userRepository.assignCompany(userId, companyId);
    }

    @Override
    @Transactional
    public void revokeCompanyFromRecruiter(String userId, String companyId) {
        userRepository.revokeCompany(userId, companyId);
    }

    @Override
    public List<RecruiterAuthorizationDto> getAllRecruitersWithAuthorizations() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == com.placement.portal.auth.Role.ROLE_RECRUITER)
                .map(u -> {
                    List<CompanyDto> authorized = getAuthorizedCompanies(u.getUsername());
                    return new RecruiterAuthorizationDto(
                            u.getUserId(),
                            u.getUsername(),
                            u.getFullName() != null ? u.getFullName() : u.getUsername(),
                            u.getEmail(),
                            authorized
                    );
                })
                .collect(Collectors.toList());
    }

    private CompanyDto toDto(CompanyNode c) {
        return new CompanyDto(
                c.getCompanyId(),
                c.getEmail(),
                c.getCompanyName(),
                c.getIndustry(),
                c.getPhoneNumbers() != null ? c.getPhoneNumbers() : List.of()
        );
    }
}
