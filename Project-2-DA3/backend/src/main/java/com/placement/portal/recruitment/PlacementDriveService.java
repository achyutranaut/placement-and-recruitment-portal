package com.placement.portal.recruitment;

import com.placement.portal.application.DailyRoutineRepository;
import com.placement.portal.company.CompanyRepository;
import com.placement.portal.company.EmailCompany;
import com.placement.portal.company.EmailCompanyRepository;
import com.placement.portal.company.JobCompany;
import com.placement.portal.company.JobCompanyRepository;
import com.placement.portal.program.Program;
import com.placement.portal.program.ProgramRepository;
import com.placement.portal.student.Student;
import com.placement.portal.student.StudentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlacementDriveService {

    private final PlacementDriveRepository driveRepository;
    private final JobCompanyRepository jobCompanyRepository;
    private final CompanyRepository companyRepository;
    private final EmailCompanyRepository emailCompanyRepository;
    private final DailyRoutineRepository dailyRoutineRepository;
    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final DriveEligibilityRepository driveEligibilityRepository;
    private final Clock clock;

    public PlacementDriveService(
            PlacementDriveRepository driveRepository,
            JobCompanyRepository jobCompanyRepository,
            CompanyRepository companyRepository,
            EmailCompanyRepository emailCompanyRepository,
            DailyRoutineRepository dailyRoutineRepository,
            StudentRepository studentRepository,
            ProgramRepository programRepository,
            DriveEligibilityRepository driveEligibilityRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) Clock clock
    ) {
        this.driveRepository = driveRepository;
        this.jobCompanyRepository = jobCompanyRepository;
        this.companyRepository = companyRepository;
        this.emailCompanyRepository = emailCompanyRepository;
        this.dailyRoutineRepository = dailyRoutineRepository;
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.driveEligibilityRepository = driveEligibilityRepository;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public boolean isDeadlinePassed(LocalDate deadline) {
        if (deadline == null) return false;
        return getToday().isAfter(deadline);
    }

    public LocalDate getToday() {
        return LocalDate.now(clock);
    }

    public Clock getClock() {
        return clock;
    }

    public DriveEligibilityDto evaluateEligibility(String driveId, String studentId) {
        PlacementDrive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new IllegalArgumentException("Placement drive not found: " + driveId));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        BigDecimal studentCgpa = student.getCgpa() != null ? student.getCgpa() : BigDecimal.ZERO;
        BigDecimal minCgpa = drive.getMinCgpa() != null ? drive.getMinCgpa() : BigDecimal.ZERO;
        boolean cgpaEligible = studentCgpa.compareTo(minCgpa) >= 0;

        String studentProgId = student.getProgramId();
        if (studentProgId == null && student.getRegistrationNo() != null) {
            studentProgId = resolveCanonicalProgramId(student.getRegistrationNo(), student.getBranch());
        }
        if (studentProgId == null) {
            studentProgId = "BCE";
        }

        List<DriveEligibility> eligibilities = driveEligibilityRepository.findByDriveId(driveId);
        List<String> eligibleProgIds = eligibilities.stream()
                .map(DriveEligibility::getProgramId)
                .collect(Collectors.toList());

        boolean programEligible;
        if (!eligibleProgIds.isEmpty()) {
            programEligible = eligibleProgIds.contains(studentProgId)
                    || ("BCE".equals(studentProgId) && eligibleProgIds.contains("BTECH-CSE"))
                    || ("BTECH-CSE".equals(studentProgId) && eligibleProgIds.contains("BCE"))
                    || ("BCT".equals(studentProgId) && eligibleProgIds.contains("BTECH-IT"))
                    || ("BTECH-IT".equals(studentProgId) && eligibleProgIds.contains("BCT"))
                    || ("BEC".equals(studentProgId) && eligibleProgIds.contains("BTECH-ECE"))
                    || ("BTECH-ECE".equals(studentProgId) && eligibleProgIds.contains("BEC"))
                    || ("BME".equals(studentProgId) && eligibleProgIds.contains("BTECH-MECH"))
                    || ("BTECH-MECH".equals(studentProgId) && eligibleProgIds.contains("BME"));
        } else {
            String branches = drive.getEligibleBranches();
            if (branches == null || branches.isBlank()) {
                // A drive without junction rows and without explicit branch rules is unconfigured and ineligible
                programEligible = false;
            } else if (branches.equalsIgnoreCase("ALL") || branches.equalsIgnoreCase("ANY")
                    || branches.toUpperCase().contains("ALL ENGINEERING")
                    || branches.toUpperCase().contains("ALL DISCIPLINES")) {
                programEligible = true;
            } else {
                String upperBranches = branches.toUpperCase();
                String upperProg = studentProgId.toUpperCase();
                String upperBranch = student.getBranch() != null ? student.getBranch().toUpperCase() : "";
                programEligible = upperBranches.contains(upperProg)
                        || (upperProg.contains("CSE") && upperBranches.contains("CSE"))
                        || (upperProg.contains("BCE") && upperBranches.contains("CSE"))
                        || (upperProg.contains("IT") && upperBranches.contains("IT"))
                        || (upperProg.contains("BCT") && upperBranches.contains("IT"))
                        || (upperProg.contains("ECE") && upperBranches.contains("ECE"))
                        || (upperProg.contains("BEC") && upperBranches.contains("ECE"))
                        || (upperProg.contains("MECH") && upperBranches.contains("MECH"))
                        || (upperProg.contains("BME") && upperBranches.contains("MECH"))
                        || (!upperBranch.isBlank() && upperBranches.contains(upperBranch));
            }
        }

        boolean deadlinePassed = isDeadlinePassed(drive.getApplicationDeadline());
        boolean driveOpen = !deadlinePassed;

        boolean eligible = cgpaEligible && programEligible && driveOpen;

        List<String> eligibleProgramNames = programRepository.findAllById(eligibleProgIds).stream()
                .map(Program::getProgramName)
                .collect(Collectors.toList());
        if (eligibleProgramNames.isEmpty() && drive.getEligibleBranches() != null && !drive.getEligibleBranches().isBlank()) {
            eligibleProgramNames = List.of(drive.getEligibleBranches());
        }

        String studentProgramName = programRepository.findById(studentProgId)
                .map(Program::getProgramName)
                .orElse(student.getBranch() != null ? student.getBranch() : studentProgId);

        String message;
        if (eligible) {
            message = "Student satisfies all academic and institutional eligibility requirements.";
        } else if (!cgpaEligible) {
            message = "Ineligible: Student CGPA (" + studentCgpa.setScale(2) +
                    ") is below required minimum CGPA (" + minCgpa.setScale(2) + ").";
        } else if (!programEligible) {
            message = "Ineligible: Student academic program (" + studentProgramName +
                    ") is not eligible for this drive. Eligible programs: " + String.join(", ", eligibleProgramNames) + ".";
        } else {
            message = "Ineligible: Application deadline passed on " + drive.getApplicationDeadline() + ".";
        }

        return new DriveEligibilityDto(
                eligible,
                cgpaEligible,
                programEligible,
                deadlinePassed,
                driveOpen,
                studentCgpa,
                minCgpa,
                studentProgramName,
                studentProgId,
                eligibleProgramNames,
                eligibleProgIds,
                drive.getApplicationDeadline(),
                message
        );
    }

    private String resolveCanonicalProgramId(String regNo, String branch) {
        if (regNo != null) {
            String upper = regNo.toUpperCase();
            if (upper.contains("BCE")) return "BCE";
            if (upper.contains("BCT")) return "BCT";
            if (upper.contains("BEC")) return "BEC";
            if (upper.contains("BME")) return "BME";
            if (upper.contains("BDS")) return "BDS";
            if (upper.contains("BEE")) return "BEE";
            if (upper.contains("BCV")) return "BCV";
        }
        if (branch != null) {
            String b = branch.toLowerCase();
            if (b.contains("computer")) return "BCE";
            if (b.contains("information")) return "BCT";
            if (b.contains("electronic")) return "BEC";
            if (b.contains("mechanical")) return "BME";
            if (b.contains("data")) return "BDS";
        }
        return "BCE";
    }

    public Map<String, DriveEligibilityDto> evaluateAllEligibilityForStudent(String studentId) {
        List<PlacementDrive> drives = driveRepository.findAll();
        Map<String, DriveEligibilityDto> result = new LinkedHashMap<>();
        for (PlacementDrive d : drives) {
            result.put(d.getDriveId(), evaluateEligibility(d.getDriveId(), studentId));
        }
        return result;
    }

    public List<PlacementDriveDto> getAllDrives(boolean includeExpired) {
        List<PlacementDrive> drives = driveRepository.findAll();
        if (drives.isEmpty()) return java.util.Collections.emptyList();

        Map<String, String> jobToCompanyId = jobCompanyRepository.findAll().stream()
                .collect(Collectors.toMap(JobCompany::getJobTitle, JobCompany::getCompanyId, (a, b) -> a));

        Map<String, String> compIdToEmail = companyRepository.findAll().stream()
                .collect(Collectors.toMap(com.placement.portal.company.Company::getCompanyId, com.placement.portal.company.Company::getEmail, (a, b) -> a));

        Map<String, String> emailToName = emailCompanyRepository.findAll().stream()
                .collect(Collectors.toMap(EmailCompany::getEmail, EmailCompany::getCompanyName, (a, b) -> a));

        Map<String, String> progIdToName = programRepository.findAll().stream()
                .collect(Collectors.toMap(Program::getProgramId, Program::getProgramName, (a, b) -> a));

        Map<String, List<String>> driveToProgIds = driveEligibilityRepository.findAll().stream()
                .collect(Collectors.groupingBy(DriveEligibility::getDriveId,
                        Collectors.mapping(DriveEligibility::getProgramId, Collectors.toList())));

        Map<String, Long> driveApplicantCounts = dailyRoutineRepository.findAll().stream()
                .filter(r -> r.getDriveId() != null)
                .collect(Collectors.groupingBy(com.placement.portal.application.StudentDailyRoutineDrive::getDriveId, Collectors.counting()));

        return drives.stream().map(d -> {
            String companyId = jobToCompanyId.getOrDefault(d.getJobTitle(), "");
            String email = compIdToEmail.getOrDefault(companyId, "");
            String companyName = emailToName.getOrDefault(email, "Recruiter");

            long applicantCount = driveApplicantCounts.getOrDefault(d.getDriveId(), 0L);
            LocalDate driveDate = d.getDriveDate() != null ? d.getDriveDate() : getToday().plusDays(14);
            LocalDate deadline = d.getApplicationDeadline() != null ? d.getApplicationDeadline() : getToday().plusDays(7);
            int openings = d.getOpenings() != null ? d.getOpenings() : 15;
            BigDecimal pkg = d.getCtc();
            String desc = d.getJobDescription();
            String loc = d.getLocation();

            List<String> progIds = driveToProgIds.getOrDefault(d.getDriveId(), java.util.Collections.emptyList());
            List<String> progNames = progIds.stream().map(id -> progIdToName.getOrDefault(id, id)).collect(Collectors.toList());
            String branches = !progNames.isEmpty() ? String.join(", ", progNames) : d.getEligibleBranches();
            String process = d.getSelectionProcess();

            boolean expired = isDeadlinePassed(deadline);
            String status = expired ? "EXPIRED" : "ACTIVE";

            PlacementDriveDto dto = new PlacementDriveDto(
                    d.getDriveId(),
                    d.getJobTitle(),
                    d.getMinCgpa(),
                    companyId,
                    companyName,
                    driveDate,
                    deadline,
                    openings,
                    pkg,
                    applicantCount,
                    desc,
                    loc,
                    branches,
                    process
            );
            dto.setEligibleProgramIds(progIds);
            dto.setEligiblePrograms(progNames);
            dto.setExpired(expired);
            dto.setStatus(status);
            return dto;
        })
        .filter(dto -> includeExpired || !Boolean.TRUE.equals(dto.isExpired()))
        .collect(Collectors.toList());
    }

    public List<PlacementDriveDto> getAllDrives() {
        return getAllDrives(false);
    }

    public PlacementDriveDto getDriveById(String driveId) {
        PlacementDrive d = driveRepository.findById(driveId)
                .orElseThrow(() -> new IllegalArgumentException("Placement Drive not found: " + driveId));
        return toDto(d);
    }

    private PlacementDriveDto toDto(PlacementDrive d) {
        String companyId = "";
        String companyName = "Recruiter";
        var jobOpt = jobCompanyRepository.findById(d.getJobTitle());
        if (jobOpt.isPresent()) {
            companyId = jobOpt.get().getCompanyId();
            var compOpt = companyRepository.findById(companyId);
            if (compOpt.isPresent()) {
                companyName = emailCompanyRepository.findById(compOpt.get().getEmail())
                        .map(EmailCompany::getCompanyName).orElse("Recruiter");
            }
        }
        long applicantCount = dailyRoutineRepository.countByDriveId(d.getDriveId());
        LocalDate driveDate = d.getDriveDate() != null ? d.getDriveDate() : getToday().plusDays(14);
        LocalDate deadline = d.getApplicationDeadline() != null ? d.getApplicationDeadline() : getToday().plusDays(7);
        int openings = d.getOpenings() != null ? d.getOpenings() : 15;
        BigDecimal pkg = d.getCtc();
        String desc = d.getJobDescription();
        String loc = d.getLocation();

        // Authoritative resolution from DRIVE_ELIGIBILITY junction table
        List<DriveEligibility> eligibilities = driveEligibilityRepository.findByDriveId(d.getDriveId());
        List<String> progIds = eligibilities.stream()
                .map(DriveEligibility::getProgramId)
                .collect(Collectors.toList());
        List<String> progNames = programRepository.findAllById(progIds).stream()
                .map(Program::getProgramName)
                .collect(Collectors.toList());

        String branches = !progNames.isEmpty()
                ? String.join(", ", progNames)
                : d.getEligibleBranches();
        String process = d.getSelectionProcess();

        boolean expired = isDeadlinePassed(deadline);
        String status = expired ? "EXPIRED" : "ACTIVE";

        PlacementDriveDto dto = new PlacementDriveDto(
                d.getDriveId(),
                d.getJobTitle(),
                d.getMinCgpa(),
                companyId,
                companyName,
                driveDate,
                deadline,
                openings,
                pkg,
                applicantCount,
                desc,
                loc,
                branches,
                process
        );
        dto.setEligibleProgramIds(progIds);
        dto.setEligiblePrograms(progNames);
        dto.setExpired(expired);
        dto.setStatus(status);
        return dto;
    }

    @org.springframework.transaction.annotation.Transactional
    public PlacementDriveDto createDrive(PlacementDriveDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Placement drive data is required");
        }
        if (dto.getJobTitle() == null || dto.getJobTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Job title is required and cannot be blank");
        }
        String jobTitle = dto.getJobTitle().trim();
        if (jobTitle.length() > 100) {
            throw new IllegalArgumentException("Job title must not exceed 100 characters");
        }

        if (dto.getCompanyId() == null || dto.getCompanyId().trim().isEmpty()) {
            throw new IllegalArgumentException("Company ID is required");
        }
        String companyId = dto.getCompanyId().trim();
        if (!companyRepository.existsById(companyId)) {
            throw new IllegalArgumentException("Company does not exist in registry: " + companyId);
        }

        // Validate CGPA
        BigDecimal minCgpa = dto.getMinCgpa();
        if (minCgpa == null) {
            minCgpa = BigDecimal.valueOf(7.00);
        } else if (minCgpa.compareTo(BigDecimal.ZERO) < 0 || minCgpa.compareTo(BigDecimal.valueOf(10.00)) > 0) {
            throw new IllegalArgumentException("Minimum CGPA must be between 0.00 and 10.00");
        }

        // Validate Dates
        LocalDate deadline = dto.getApplicationDeadline();
        if (deadline == null) {
            throw new IllegalArgumentException("Application deadline is required");
        }
        if (deadline.isBefore(getToday())) {
            throw new IllegalArgumentException("Application deadline must be today or a future date");
        }

        LocalDate driveDate = dto.getDriveDate();
        if (driveDate == null) {
            driveDate = deadline.plusDays(7);
        } else if (driveDate.isBefore(deadline)) {
            throw new IllegalArgumentException("Drive date must be on or after the application deadline");
        }

        // Validate Openings
        Integer openings = dto.getOpenings() != null ? dto.getOpenings() : dto.getOpeningsCount();
        if (openings == null || openings <= 0) {
            throw new IllegalArgumentException("Openings must be a positive integer");
        }

        // Validate Compensation (CTC)
        BigDecimal ctc = dto.getCtc() != null ? dto.getCtc() : dto.getPackageLpa();
        if (ctc == null || ctc.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Compensation (CTC LPA) must be a positive number");
        }

        // Validate Location
        String location = dto.getLocation() != null ? dto.getLocation().trim() : "Bengaluru / Hyderabad";
        if (location.isEmpty()) {
            throw new IllegalArgumentException("Location cannot be blank");
        }
        if (location.length() > 100) {
            location = location.substring(0, 100);
        }

        // Validate Selection Process
        String selectionProcess = dto.getSelectionProcess() != null ? dto.getSelectionProcess().trim() : "1. Online Assessment | 2. Technical Interview | 3. HR Interview";
        if (selectionProcess.isEmpty()) {
            throw new IllegalArgumentException("Selection process cannot be blank");
        }
        if (selectionProcess.length() > 500) {
            selectionProcess = selectionProcess.substring(0, 500);
        }

        // Validate Description
        String description = dto.getJobDescription();
        if (description != null && description.length() > 1000) {
            description = description.substring(0, 1000);
        }

        // Validate and resolve Eligible Programs
        List<String> programIds = dto.getEligibleProgramIds();
        if (programIds == null || programIds.isEmpty()) {
            if (dto.getEligibleBranches() != null && !dto.getEligibleBranches().isBlank()) {
                programIds = resolveProgramIdsFromBranchString(dto.getEligibleBranches());
            }
        }
        if (programIds == null || programIds.isEmpty()) {
            throw new IllegalArgumentException("At least one eligible academic program must be selected");
        }

        // Verify each program exists in PROGRAM table
        for (String pid : programIds) {
            if (!programRepository.existsById(pid)) {
                throw new IllegalArgumentException("Academic program ID not recognized in database catalog: " + pid);
            }
        }

        // Enforce/persist JOB_COMPANY relationship
        var existingJobCompanyOpt = jobCompanyRepository.findById(jobTitle);
        if (existingJobCompanyOpt.isPresent()) {
            String existingCompanyId = existingJobCompanyOpt.get().getCompanyId();
            if (!existingCompanyId.equalsIgnoreCase(companyId)) {
                throw new IllegalArgumentException("Job title '" + jobTitle + "' is already registered to another organization (" + existingCompanyId + "). Please provide a distinct role title.");
            }
        } else {
            jobCompanyRepository.save(new JobCompany(jobTitle, companyId));
        }

        // Generate next unique Drive_Id
        String driveId = dto.getDriveId();
        if (driveId == null || driveId.isBlank()) {
            int maxId = 0;
            for (PlacementDrive d : driveRepository.findAll()) {
                String id = d.getDriveId();
                if (id != null && id.toUpperCase().startsWith("DRV")) {
                    try {
                        int num = Integer.parseInt(id.substring(3).trim());
                        if (num > maxId) maxId = num;
                    } catch (NumberFormatException ignored) {}
                }
            }
            driveId = String.format("DRV%03d", maxId + 1);
            while (driveRepository.existsById(driveId)) {
                maxId++;
                driveId = String.format("DRV%03d", maxId + 1);
            }
        }

        // Compute human-readable eligible branches string
        List<String> programNames = programRepository.findAllById(programIds).stream()
                .map(Program::getProgramName)
                .collect(Collectors.toList());
        String eligibleBranchesStr = String.join(", ", programNames);
        if (eligibleBranchesStr.length() > 200) {
            eligibleBranchesStr = eligibleBranchesStr.substring(0, 197) + "...";
        }

        PlacementDrive drive = new PlacementDrive();
        drive.setDriveId(driveId);
        drive.setJobTitle(jobTitle);
        drive.setMinCgpa(minCgpa);
        drive.setDriveDate(driveDate);
        drive.setApplicationDeadline(deadline);
        drive.setOpenings(openings);
        drive.setCtc(ctc);
        drive.setJobDescription(description);
        drive.setLocation(location);
        drive.setEligibleBranches(eligibleBranchesStr);
        drive.setSelectionProcess(selectionProcess);

        PlacementDrive saved = driveRepository.save(drive);

        // Persist DRIVE_ELIGIBILITY junction entries
        for (String pid : programIds) {
            driveEligibilityRepository.save(new DriveEligibility(saved.getDriveId(), pid));
        }

        return toDto(saved);
    }

    private List<String> resolveProgramIdsFromBranchString(String branches) {
        String upper = branches.toUpperCase();
        List<String> pids = new ArrayList<>();
        if (upper.contains("CSE") || upper.contains("COMPUTER")) {
            if (programRepository.existsById("BTECH-CSE")) pids.add("BTECH-CSE");
            else if (programRepository.existsById("BCE")) pids.add("BCE");
        }
        if (upper.contains("IT") || upper.contains("INFORMATION")) {
            if (programRepository.existsById("BTECH-IT")) pids.add("BTECH-IT");
            else if (programRepository.existsById("BCT")) pids.add("BCT");
        }
        if (upper.contains("ECE") || upper.contains("ELECTRONIC")) {
            if (programRepository.existsById("BTECH-ECE")) pids.add("BTECH-ECE");
            else if (programRepository.existsById("BEC")) pids.add("BEC");
        }
        if (upper.contains("MECH")) {
            if (programRepository.existsById("BTECH-MECH")) pids.add("BTECH-MECH");
            else if (programRepository.existsById("BME")) pids.add("BME");
        }
        return pids;
    }

    @org.springframework.transaction.annotation.Transactional
    public PlacementDriveDto updateDrive(String driveId, PlacementDriveDto dto) {
        PlacementDrive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new IllegalArgumentException("Placement Drive not found: " + driveId));

        if (dto.getJobTitle() != null && !dto.getJobTitle().isBlank()) drive.setJobTitle(dto.getJobTitle().trim());
        if (dto.getMinCgpa() != null) drive.setMinCgpa(dto.getMinCgpa());
        if (dto.getDriveDate() != null) drive.setDriveDate(dto.getDriveDate());
        if (dto.getApplicationDeadline() != null) drive.setApplicationDeadline(dto.getApplicationDeadline());
        if (dto.getOpenings() != null) drive.setOpenings(dto.getOpenings());
        else if (dto.getOpeningsCount() != null) drive.setOpenings(dto.getOpeningsCount());
        if (dto.getCtc() != null) drive.setCtc(dto.getCtc());
        if (dto.getJobDescription() != null) drive.setJobDescription(dto.getJobDescription());
        if (dto.getLocation() != null) drive.setLocation(dto.getLocation());
        if (dto.getEligibleBranches() != null) drive.setEligibleBranches(dto.getEligibleBranches());
        if (dto.getSelectionProcess() != null) drive.setSelectionProcess(dto.getSelectionProcess());

        if (dto.getEligibleProgramIds() != null && !dto.getEligibleProgramIds().isEmpty()) {
            for (String pid : dto.getEligibleProgramIds()) {
                if (!programRepository.existsById(pid)) {
                    throw new IllegalArgumentException("Academic program ID not recognized: " + pid);
                }
            }
            List<DriveEligibility> current = driveEligibilityRepository.findByDriveId(driveId);
            driveEligibilityRepository.deleteAll(current);
            for (String pid : dto.getEligibleProgramIds()) {
                driveEligibilityRepository.save(new DriveEligibility(driveId, pid));
            }
            List<String> progNames = programRepository.findAllById(dto.getEligibleProgramIds()).stream()
                    .map(Program::getProgramName).collect(Collectors.toList());
            String branchesStr = String.join(", ", progNames);
            if (branchesStr.length() > 200) branchesStr = branchesStr.substring(0, 197) + "...";
            drive.setEligibleBranches(branchesStr);
        }

        PlacementDrive saved = driveRepository.save(drive);
        return toDto(saved);
    }
}

