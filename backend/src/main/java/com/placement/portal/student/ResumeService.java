package com.placement.portal.student;

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

    public ResumeService(StudentResumeRepository resumeRepository, StudentRepository studentRepository) {
        this.resumeRepository = resumeRepository;
        this.studentRepository = studentRepository;
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
                .orElseThrow(() -> new IllegalArgumentException("Resume not found with ID: " + resumeId));
    }

    public ResumeDto getResumeById(String resumeId) {
        return ResumeDto.fromEntity(getResumeEntity(resumeId));
    }
}
