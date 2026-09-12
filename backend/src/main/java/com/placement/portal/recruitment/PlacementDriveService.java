package com.placement.portal.recruitment;

import com.placement.portal.application.DailyRoutineRepository;
import com.placement.portal.company.CompanyRepository;
import com.placement.portal.company.EmailCompany;
import com.placement.portal.company.EmailCompanyRepository;
import com.placement.portal.company.JobCompanyRepository;
import com.placement.portal.program.Program;
import com.placement.portal.program.ProgramRepository;
import com.placement.portal.student.Student;
import com.placement.portal.student.StudentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    public PlacementDriveService(
            PlacementDriveRepository driveRepository,
            JobCompanyRepository jobCompanyRepository,
            CompanyRepository companyRepository,
            EmailCompanyRepository emailCompanyRepository,
            DailyRoutineRepository dailyRoutineRepository,
            StudentRepository studentRepository,
            ProgramRepository programRepository,
            DriveEligibilityRepository driveEligibilityRepository
    ) {
        this.driveRepository = driveRepository;
        this.jobCompanyRepository = jobCompanyRepository;
        this.companyRepository = companyRepository;
        this.emailCompanyRepository = emailCompanyRepository;
        this.dailyRoutineRepository = dailyRoutineRepository;
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
        this.driveEligibilityRepository = driveEligibilityRepository;
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

        boolean programEligible = eligibleProgIds.contains(studentProgId)
                || ("BCE".equals(studentProgId) && eligibleProgIds.contains("BTECH-CSE"))
                || ("BTECH-CSE".equals(studentProgId) && eligibleProgIds.contains("BCE"))
                || ("BCT".equals(studentProgId) && eligibleProgIds.contains("BTECH-IT"))
                || ("BTECH-IT".equals(studentProgId) && eligibleProgIds.contains("BCT"))
                || ("BEC".equals(studentProgId) && eligibleProgIds.contains("BTECH-ECE"))
                || ("BTECH-ECE".equals(studentProgId) && eligibleProgIds.contains("BEC"))
                || ("BME".equals(studentProgId) && eligibleProgIds.contains("BTECH-MECH"))
                || ("BTECH-MECH".equals(studentProgId) && eligibleProgIds.contains("BME"));

        boolean deadlinePassed = drive.getApplicationDeadline() != null &&
                LocalDate.now().isAfter(drive.getApplicationDeadline());
        boolean driveOpen = !deadlinePassed;

        boolean eligible = cgpaEligible && programEligible && driveOpen;

        List<String> eligibleProgramNames = programRepository.findAllById(eligibleProgIds).stream()
                .map(Program::getProgramName)
                .collect(Collectors.toList());

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

    public List<PlacementDriveDto> getAllDrives() {
        return driveRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
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
        LocalDate driveDate = d.getDriveDate() != null ? d.getDriveDate() : LocalDate.now().plusDays(14);
        LocalDate deadline = d.getApplicationDeadline() != null ? d.getApplicationDeadline() : LocalDate.now().plusDays(7);
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

        return new PlacementDriveDto(
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
    }

    @org.springframework.transaction.annotation.Transactional
    public PlacementDriveDto createDrive(PlacementDriveDto dto) {
        String driveId = dto.getDriveId();
        if (driveId == null || driveId.isBlank()) {
            long count = driveRepository.count();
            driveId = "DRV" + String.format("%03d", count + 1);
        }
        PlacementDrive drive = new PlacementDrive();
        drive.setDriveId(driveId);
        drive.setJobTitle(dto.getJobTitle());
        drive.setMinCgpa(dto.getMinCgpa() != null ? dto.getMinCgpa() : BigDecimal.valueOf(7.0));
        drive.setDriveDate(dto.getDriveDate() != null ? dto.getDriveDate() : LocalDate.now().plusDays(14));
        drive.setApplicationDeadline(dto.getApplicationDeadline() != null ? dto.getApplicationDeadline() : LocalDate.now().plusDays(7));
        drive.setOpenings(dto.getOpenings() != null ? dto.getOpenings() : (dto.getOpeningsCount() != null ? dto.getOpeningsCount() : 10));
        drive.setCtc(dto.getCtc() != null ? dto.getCtc() : BigDecimal.valueOf(10.0));
        drive.setJobDescription(dto.getJobDescription());
        drive.setLocation(dto.getLocation() != null ? dto.getLocation() : "Bengaluru / Hyderabad");
        drive.setEligibleBranches(dto.getEligibleBranches() != null ? dto.getEligibleBranches() : "CSE, IT, ECE");
        drive.setSelectionProcess(dto.getSelectionProcess() != null ? dto.getSelectionProcess() : "1. Online Assessment | 2. Technical Interview | 3. HR Interview");

        PlacementDrive saved = driveRepository.save(drive);
        return toDto(saved);
    }

    @org.springframework.transaction.annotation.Transactional
    public PlacementDriveDto updateDrive(String driveId, PlacementDriveDto dto) {
        PlacementDrive drive = driveRepository.findById(driveId)
                .orElseThrow(() -> new IllegalArgumentException("Placement Drive not found: " + driveId));

        if (dto.getJobTitle() != null && !dto.getJobTitle().isBlank()) drive.setJobTitle(dto.getJobTitle());
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

        PlacementDrive saved = driveRepository.save(drive);
        return toDto(saved);
    }
}

