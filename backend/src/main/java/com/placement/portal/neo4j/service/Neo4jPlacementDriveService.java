package com.placement.portal.neo4j.service;

import com.placement.portal.neo4j.model.CompanyNode;
import com.placement.portal.neo4j.model.DriveNode;
import com.placement.portal.neo4j.model.ProgramNode;
import com.placement.portal.neo4j.model.StudentNode;
import com.placement.portal.neo4j.repository.Neo4jCompanyRepository;
import com.placement.portal.neo4j.repository.Neo4jDriveRepository;
import com.placement.portal.neo4j.repository.Neo4jProgramRepository;
import com.placement.portal.neo4j.repository.Neo4jStudentRepository;
import com.placement.portal.recruitment.DriveEligibilityDto;
import com.placement.portal.recruitment.IPlacementDriveService;
import com.placement.portal.recruitment.PlacementDriveDto;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("neo4j")
@Primary
public class Neo4jPlacementDriveService implements IPlacementDriveService {

    private final Neo4jDriveRepository driveRepository;
    private final Neo4jCompanyRepository companyRepository;
    private final Neo4jStudentRepository studentRepository;
    private final Neo4jProgramRepository programRepository;

    public Neo4jPlacementDriveService(
            Neo4jDriveRepository driveRepository,
            Neo4jCompanyRepository companyRepository,
            Neo4jStudentRepository studentRepository,
            Neo4jProgramRepository programRepository
    ) {
        this.driveRepository = driveRepository;
        this.companyRepository = companyRepository;
        this.studentRepository = studentRepository;
        this.programRepository = programRepository;
    }

    @Override
    public DriveEligibilityDto evaluateEligibility(String driveId, String studentId) {
        DriveNode drive = driveRepository.findByDriveId(driveId)
                .orElseThrow(() -> new IllegalArgumentException("Placement drive not found: " + driveId));
        StudentNode student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        BigDecimal studentCgpa = student.getCgpa() != null ? student.getCgpa() : BigDecimal.ZERO;
        BigDecimal minCgpa = drive.getMinCgpa() != null ? drive.getMinCgpa() : BigDecimal.ZERO;
        boolean cgpaEligible = studentCgpa.compareTo(minCgpa) >= 0;

        String studentProgId = student.getProgramId() != null ? student.getProgramId() : "BCE";
        List<String> eligibleProgIds = driveRepository.findEligibleProgramIds(driveId);

        boolean programEligible = eligibleProgIds.isEmpty() || eligibleProgIds.contains(studentProgId)
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

        String studentProgramName = programRepository.findByProgramId(studentProgId)
                .map(ProgramNode::getProgramName)
                .orElse(student.getBranch() != null ? student.getBranch() : studentProgId);

        List<String> eligibleProgramNames = eligibleProgIds.stream()
                .map(id -> programRepository.findByProgramId(id).map(ProgramNode::getProgramName).orElse(id))
                .collect(Collectors.toList());

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

    @Override
    public Map<String, DriveEligibilityDto> evaluateAllEligibilityForStudent(String studentId) {
        List<DriveNode> drives = driveRepository.findAllOrdered();
        Map<String, DriveEligibilityDto> result = new LinkedHashMap<>();
        for (DriveNode d : drives) {
            result.put(d.getDriveId(), evaluateEligibility(d.getDriveId(), studentId));
        }
        return result;
    }

    @Override
    public List<PlacementDriveDto> getAllDrives() {
        return driveRepository.findAllOrdered().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PlacementDriveDto getDriveById(String driveId) {
        DriveNode drive = driveRepository.findByDriveId(driveId)
                .orElseThrow(() -> new IllegalArgumentException("Placement Drive not found: " + driveId));
        return toDto(drive);
    }

    @Override
    @Transactional
    public PlacementDriveDto createDrive(PlacementDriveDto dto) {
        String driveId = dto.getDriveId();
        if (driveId == null || driveId.isBlank()) {
            long count = driveRepository.count();
            driveId = "DRV" + String.format("%03d", count + 1);
        }

        String compName = dto.getCompanyName();
        if (compName == null || compName.isBlank()) {
            if (dto.getCompanyId() != null) {
                compName = companyRepository.findByCompanyId(dto.getCompanyId())
                        .map(CompanyNode::getCompanyName).orElse(dto.getCompanyId());
            } else {
                compName = "Corporate Recruiter";
            }
        }

        DriveNode drive = new DriveNode(
                driveId,
                dto.getJobTitle(),
                dto.getCompanyId(),
                compName,
                dto.getMinCgpa() != null ? dto.getMinCgpa() : BigDecimal.valueOf(7.0),
                dto.getDriveDate() != null ? dto.getDriveDate() : LocalDate.now().plusDays(14),
                dto.getApplicationDeadline() != null ? dto.getApplicationDeadline() : LocalDate.now().plusDays(7),
                dto.getOpenings() != null ? dto.getOpenings() : (dto.getOpeningsCount() != null ? dto.getOpeningsCount() : 10),
                dto.getCtc() != null ? dto.getCtc() : BigDecimal.valueOf(10.0),
                dto.getJobDescription(),
                dto.getLocation() != null ? dto.getLocation() : "Bengaluru / Hyderabad",
                dto.getEligibleBranches() != null ? dto.getEligibleBranches() : "CSE, IT, ECE",
                dto.getSelectionProcess() != null ? dto.getSelectionProcess() : "1. Online Assessment | 2. Technical Interview | 3. HR Interview"
        );

        if (dto.getCompanyId() != null) {
            companyRepository.findByCompanyId(dto.getCompanyId()).ifPresent(drive::setCompany);
        }

        DriveNode saved = driveRepository.save(drive);
        return toDto(saved);
    }

    @Override
    @Transactional
    public PlacementDriveDto updateDrive(String driveId, PlacementDriveDto dto) {
        DriveNode drive = driveRepository.findByDriveId(driveId)
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

        DriveNode saved = driveRepository.save(drive);
        return toDto(saved);
    }

    private PlacementDriveDto toDto(DriveNode d) {
        String compName = d.getCompanyName() != null ? d.getCompanyName() : "Corporate Recruiter";
        String compId = d.getCompanyId() != null ? d.getCompanyId() : "";

        long applicantCount = driveRepository.countApplicationsByDriveId(d.getDriveId());

        return new PlacementDriveDto(
                d.getDriveId(),
                d.getJobTitle(),
                d.getMinCgpa(),
                compId,
                compName,
                d.getDriveDate(),
                d.getApplicationDeadline(),
                d.getOpenings(),
                d.getCtc(),
                applicantCount,
                d.getJobDescription(),
                d.getLocation(),
                d.getEligibleBranches(),
                d.getSelectionProcess()
        );
    }
}
