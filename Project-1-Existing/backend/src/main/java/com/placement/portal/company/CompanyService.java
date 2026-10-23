package com.placement.portal.company;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final EmailCompanyRepository emailCompanyRepository;
    private final JobCompanyRepository jobCompanyRepository;
    private final RecruiterCompanyRepository recruiterCompanyRepository;
    private final com.placement.portal.auth.UserRepository userRepository;

    public CompanyService(
            CompanyRepository companyRepository,
            EmailCompanyRepository emailCompanyRepository,
            JobCompanyRepository jobCompanyRepository,
            RecruiterCompanyRepository recruiterCompanyRepository,
            com.placement.portal.auth.UserRepository userRepository
    ) {
        this.companyRepository = companyRepository;
        this.emailCompanyRepository = emailCompanyRepository;
        this.jobCompanyRepository = jobCompanyRepository;
        this.recruiterCompanyRepository = recruiterCompanyRepository;
        this.userRepository = userRepository;
    }



    public List<CompanyDto> getAllCompanies() {
        return companyRepository.findAll().stream().map(c -> {
            String name = emailCompanyRepository.findById(c.getEmail())
                    .map(EmailCompany::getCompanyName).orElse("Corporate Recruiter");
            return new CompanyDto(c.getCompanyId(), c.getEmail(), name, c.getIndustry(), c.getPhoneNumbers());
        }).collect(Collectors.toList());
    }

    public CompanyDto getCompanyById(String companyId) {
        Company c = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
        String name = emailCompanyRepository.findById(c.getEmail())
                .map(EmailCompany::getCompanyName).orElse("Corporate Recruiter");
        return new CompanyDto(c.getCompanyId(), c.getEmail(), name, c.getIndustry(), c.getPhoneNumbers());
    }

    public List<JobDto> getJobsByCompany(String companyId) {
        return jobCompanyRepository.findByCompanyId(companyId).stream()
                .map(j -> new JobDto(j.getJobTitle(), j.getCompanyId()))
                .collect(Collectors.toList());
    }

    public List<JobDto> getAllJobs() {
        return jobCompanyRepository.findAll().stream()
                .map(j -> new JobDto(j.getJobTitle(), j.getCompanyId()))
                .collect(Collectors.toList());
    }

    public List<CompanyDto> getAuthorizedCompanies(String username) {
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsernameIgnoreCase(username);
        }
        if (userOpt.isEmpty()) {
            return List.of();
        }

        var user = userOpt.get();
        if (user.getRole() == com.placement.portal.auth.Role.ROLE_ADMIN) {
            return getAllCompanies();
        }

        // Collect authorized company IDs from RECRUITER_COMPANY junction table
        java.util.Set<String> authorizedCompanyIds = recruiterCompanyRepository.findByUserId(user.getUserId()).stream()
                .map(RecruiterCompany::getCompanyId)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        // Also check referenceId for backwards compatibility
        if (user.getReferenceId() != null && !user.getReferenceId().isBlank() && !"ALL".equalsIgnoreCase(user.getReferenceId())) {
            authorizedCompanyIds.add(user.getReferenceId());
        }

        return authorizedCompanyIds.stream()
                .map(companyRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(c -> {
                    String name = emailCompanyRepository.findById(c.getEmail())
                            .map(EmailCompany::getCompanyName).orElse("Corporate Recruiter");
                    return new CompanyDto(c.getCompanyId(), c.getEmail(), name, c.getIndustry(), c.getPhoneNumbers());
                })
                .collect(Collectors.toList());
    }

    public boolean isRecruiterAuthorizedForCompany(String username, String companyId) {
        if (companyId == null || companyId.isBlank() || "ALL".equalsIgnoreCase(companyId)) {
            return false;
        }

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsernameIgnoreCase(username);
        }
        if (userOpt.isEmpty()) {
            return false;
        }

        var user = userOpt.get();
        if (user.getRole() == com.placement.portal.auth.Role.ROLE_ADMIN) {
            return true;
        }

        if (recruiterCompanyRepository.existsByUserIdAndCompanyId(user.getUserId(), companyId)) {
            return true;
        }

        return user.getReferenceId() != null && user.getReferenceId().equalsIgnoreCase(companyId);
    }

    @Transactional
    public void assignCompanyToRecruiter(String userId, String companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        if (!recruiterCompanyRepository.existsByUserIdAndCompanyId(userId, companyId)) {
            recruiterCompanyRepository.save(new RecruiterCompany(userId, companyId));
        }
    }

    @Transactional
    public void revokeCompanyFromRecruiter(String userId, String companyId) {
        recruiterCompanyRepository.deleteByUserIdAndCompanyId(userId, companyId);
    }

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
}
