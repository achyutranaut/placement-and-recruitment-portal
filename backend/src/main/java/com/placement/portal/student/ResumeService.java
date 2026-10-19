package com.placement.portal.student;

import com.placement.portal.common.ResourceNotFoundException;
import com.placement.portal.recruitment.ResumeEvaluationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@org.springframework.context.annotation.Profile({"oracle", "h2", "default", "!neo4j"})
public class ResumeService implements IResumeService {

    private final StudentResumeRepository resumeRepository;
    private final StudentRepository studentRepository;
    private final ResumeEvaluationRepository evaluationRepository;

    public ResumeService(
            StudentResumeRepository resumeRepository,
            StudentRepository studentRepository,
            ResumeEvaluationRepository evaluationRepository) {
        this.resumeRepository = resumeRepository;
        this.studentRepository = studentRepository;
        this.evaluationRepository = evaluationRepository;
    }

    @Transactional
    public ResumeDto uploadResume(String studentId, MultipartFile file) throws IOException {
        if (studentId == null || studentId.isBlank() || !studentRepository.existsById(studentId.trim())) {
            throw new IllegalArgumentException("Authenticated student record not found: " + studentId);
        }
        String normalizedStudentId = studentId.trim();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        // Get latest version number
        int nextVersion = 1;
        var latestOpt = resumeRepository.findTopByStudentIdOrderByVersionNoDesc(normalizedStudentId);
        if (latestOpt.isPresent()) {
            nextVersion = latestOpt.get().getVersionNo() + 1;
        }

        // Atomically mark all previous resumes for this student as inactive ('N') without rewriting BLOBs
        resumeRepository.deactivateAllForStudent(normalizedStudentId);

        String resumeId = "RES_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String contentType = file.getContentType() != null ? file.getContentType() : "application/pdf";

        StudentResume resume = new StudentResume(
                resumeId,
                normalizedStudentId,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf",
                contentType,
                file.getSize(),
                file.getBytes(),
                nextVersion,
                "Y"
        );

        StudentResume saved = resumeRepository.saveAndFlush(resume);
        return ResumeDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ResumeDto> getResumesByStudent(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return List.of();
        }
        return resumeRepository.findByStudentIdOrderByVersionNoDesc(studentId.trim()).stream()
                .map(ResumeDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResumeDto getCurrentResume(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return null;
        }
        return resumeRepository.findFirstByStudentIdAndIsCurrentOrderByVersionNoDesc(studentId.trim(), "Y")
                .map(ResumeDto::fromEntity)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public StudentResume getResumeEntity(String resumeId) {
        if (resumeId == null || resumeId.isBlank()) {
            throw new IllegalArgumentException("Resume ID cannot be empty.");
        }
        return resumeRepository.findById(resumeId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getResumeBytes(String resumeId) {
        return getResumeEntity(resumeId).getResumeData();
    }

    @Override
    @Transactional(readOnly = true)
    public String getResumeContentType(String resumeId) {
        return getResumeEntity(resumeId).getContentType();
    }

    @Override
    @Transactional(readOnly = true)
    public String getResumeFileName(String resumeId) {
        return getResumeEntity(resumeId).getFileName();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeDto getResumeById(String resumeId) {
        return ResumeDto.fromEntity(getResumeEntity(resumeId));
    }

    @Transactional
    public ResumeDto deleteResume(String authenticatedStudentId, String resumeId, boolean isAdmin) {
        if (resumeId == null || resumeId.isBlank()) {
            throw new IllegalArgumentException("Resume ID is required.");
        }
        StudentResume resume = resumeRepository.findById(resumeId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));

        // Enforce ownership: reject attempts to delete another student's resume with 403
        if (!isAdmin && (authenticatedStudentId == null || !resume.getStudentId().equalsIgnoreCase(authenticatedStudentId.trim()))) {
            throw new AccessDeniedException("Access denied: You may only delete your own resume records.");
        }

        // Active resume protection:
        // Do NOT allow deletion of the currently active resume if it would leave the student with no active resume.
        if ("Y".equalsIgnoreCase(resume.getIsCurrent())) {
            List<StudentResume> studentResumes = resumeRepository.findByStudentIdOrderByVersionNoDesc(resume.getStudentId());
            if (studentResumes.size() <= 1) {
                throw new IllegalStateException(
                        "This is your active resume. Upload another resume and activate it before deleting this version."
                );
            } else {
                throw new IllegalStateException(
                        "Cannot delete active resume (Version " + resume.getVersionNo() + "). Please activate another resume version first before deleting this version."
                );
            }
        }

        // Cascade any referencing evaluation records
        try {
            evaluationRepository.deleteByResumeId(resume.getResumeId());
        } catch (Exception ignored) {}

        ResumeDto deletedDto = ResumeDto.fromEntity(resume);
        // Deleting the entity purges the row and its associated Resume_Data BLOB in Oracle
        resumeRepository.delete(resume);
        return deletedDto;
    }

    @Transactional
    public ResumeDto activateResume(String authenticatedStudentId, String resumeId, boolean isAdmin) {
        if (resumeId == null || resumeId.isBlank()) {
            throw new IllegalArgumentException("Resume ID is required for activation.");
        }
        String normalizedResumeId = resumeId.trim();

        StudentResume targetResume = resumeRepository.findById(normalizedResumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + normalizedResumeId));

        String targetStudentId = targetResume.getStudentId();
        if (!isAdmin && (authenticatedStudentId == null || authenticatedStudentId.isBlank()
                || !targetStudentId.equalsIgnoreCase(authenticatedStudentId.trim()))) {
            throw new AccessDeniedException("Access denied: You may only activate your own resume records.");
        }

        // Step 1: Mark all resume versions for this student as inactive ('N') in Oracle
        resumeRepository.deactivateAllForStudent(targetStudentId);

        // Step 2: Mark the exact selected resume version as active ('Y') in the same transaction
        int activatedRows = resumeRepository.activateResumeForStudent(targetStudentId, targetResume.getResumeId());
        if (activatedRows != 1) {
            throw new IllegalStateException(
                    "Resume activation failed: expected 1 updated row for resume " + targetResume.getResumeId() + ", but updated " + activatedRows
            );
        }

        // Step 3: Verify invariant — exactly one resume version is active for this student
        long activeCount = resumeRepository.countByStudentIdAndIsCurrent(targetStudentId, "Y");
        if (activeCount != 1) {
            throw new IllegalStateException(
                    "Database consistency check failed: expected exactly 1 active resume for student " + targetStudentId + ", but found " + activeCount
            );
        }

        // Step 4: Return the authoritative active resume re-read from the database
        StudentResume refreshed = resumeRepository.findById(targetResume.getResumeId())
                .orElseThrow(() -> new IllegalStateException("Activated resume not found after update: " + targetResume.getResumeId()));
        return ResumeDto.fromEntity(refreshed);
    }
}

