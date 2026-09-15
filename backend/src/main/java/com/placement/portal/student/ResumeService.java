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
public class ResumeService {

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
        if (!studentRepository.existsById(studentId)) {
            throw new IllegalArgumentException("Authenticated student record not found: " + studentId);
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        // Get latest version number
        int nextVersion = 1;
        var latestOpt = resumeRepository.findTopByStudentIdOrderByVersionNoDesc(studentId);
        if (latestOpt.isPresent()) {
            nextVersion = latestOpt.get().getVersionNo() + 1;
        }

        // Mark previous current resumes as non-current
        List<StudentResume> previousResumes = resumeRepository.findAllByStudentIdAndIsCurrent(studentId, "Y");
        for (StudentResume prev : previousResumes) {
            prev.setIsCurrent("N");
        }
        if (!previousResumes.isEmpty()) {
            resumeRepository.saveAll(previousResumes);
        }

        String resumeId = "RES_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String contentType = file.getContentType() != null ? file.getContentType() : "application/pdf";

        StudentResume resume = new StudentResume(
                resumeId,
                studentId,
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf",
                contentType,
                file.getSize(),
                file.getBytes(),
                nextVersion,
                "Y"
        );

        StudentResume saved = resumeRepository.save(resume);
        return ResumeDto.fromEntity(saved);
    }

    public List<ResumeDto> getResumesByStudent(String studentId) {
        return resumeRepository.findByStudentIdOrderByVersionNoDesc(studentId).stream()
                .map(ResumeDto::fromEntity)
                .collect(Collectors.toList());
    }

    public ResumeDto getCurrentResume(String studentId) {
        return resumeRepository.findFirstByStudentIdAndIsCurrentOrderByVersionNoDesc(studentId, "Y")
                .map(ResumeDto::fromEntity)
                .orElse(null);
    }

    public StudentResume getResumeEntity(String resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));
    }

    public ResumeDto getResumeById(String resumeId) {
        return ResumeDto.fromEntity(getResumeEntity(resumeId));
    }

    @Transactional
    public ResumeDto deleteResume(String authenticatedStudentId, String resumeId, boolean isAdmin) {
        StudentResume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));

        // Enforce ownership: reject attempts to delete another student's resume with 403
        if (!isAdmin && (authenticatedStudentId == null || !resume.getStudentId().equalsIgnoreCase(authenticatedStudentId))) {
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
            evaluationRepository.deleteByResumeId(resumeId);
        } catch (Exception ignored) {}

        ResumeDto deletedDto = ResumeDto.fromEntity(resume);
        // Deleting the entity purges the row and its associated Resume_Data BLOB in Oracle
        resumeRepository.delete(resume);
        return deletedDto;
    }

    @Transactional
    public ResumeDto activateResume(String authenticatedStudentId, String resumeId, boolean isAdmin) {
        StudentResume targetResume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));

        if (!isAdmin && (authenticatedStudentId == null || !targetResume.getStudentId().equalsIgnoreCase(authenticatedStudentId))) {
            throw new AccessDeniedException("Access denied: You may only activate your own resume records.");
        }

        List<StudentResume> allResumes = resumeRepository.findByStudentIdOrderByVersionNoDesc(targetResume.getStudentId());
        for (StudentResume r : allResumes) {
            if (r.getResumeId().equalsIgnoreCase(resumeId)) {
                r.setIsCurrent("Y");
            } else {
                r.setIsCurrent("N");
            }
        }
        resumeRepository.saveAll(allResumes);
        return ResumeDto.fromEntity(targetResume);
    }
}
