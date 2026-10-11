package com.placement.portal.neo4j.service;

import com.placement.portal.common.ResourceNotFoundException;
import com.placement.portal.neo4j.model.ResumeNode;
import com.placement.portal.neo4j.model.StudentNode;
import com.placement.portal.neo4j.repository.Neo4jResumeEvaluationRepository;
import com.placement.portal.neo4j.repository.Neo4jResumeRepository;
import com.placement.portal.neo4j.repository.Neo4jStudentRepository;
import com.placement.portal.student.IResumeService;
import com.placement.portal.student.ResumeDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Profile("neo4j")
@Primary
public class Neo4jResumeService implements IResumeService {

    private final Neo4jResumeRepository resumeRepository;
    private final Neo4jStudentRepository studentRepository;
    private final Neo4jResumeEvaluationRepository evaluationRepository;
    private final String storageDir;

    public Neo4jResumeService(
            Neo4jResumeRepository resumeRepository,
            Neo4jStudentRepository studentRepository,
            Neo4jResumeEvaluationRepository evaluationRepository,
            @Value("${portal.resume.storage-dir:./data/resumes}") String storageDir
    ) {
        this.resumeRepository = resumeRepository;
        this.studentRepository = studentRepository;
        this.evaluationRepository = evaluationRepository;
        this.storageDir = storageDir;
    }

    @Override
    @Transactional
    public ResumeDto uploadResume(String studentId, MultipartFile file) throws IOException {
        StudentNode student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated student record not found: " + studentId));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        int nextVersion = 1;
        var latestOpt = resumeRepository.findTopByStudentIdOrderByVersionNoDesc(studentId);
        if (latestOpt.isPresent()) {
            nextVersion = latestOpt.get().getVersionNo() + 1;
        }

        // Mark previous resumes non-current
        List<ResumeNode> previousResumes = resumeRepository.findAllByStudentIdAndIsCurrent(studentId, "Y");
        for (ResumeNode prev : previousResumes) {
            prev.setIsCurrent("N");
        }
        if (!previousResumes.isEmpty()) {
            resumeRepository.saveAll(previousResumes);
        }

        String resumeId = "RES_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String contentType = file.getContentType() != null ? file.getContentType() : "application/pdf";
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf";

        // Write binary file to disk directory per Step 1 requirement
        Path dirPath = Paths.get(storageDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        byte[] bytes = file.getBytes();
        String fileExt = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".pdf";
        Path targetPath = dirPath.resolve(resumeId + fileExt);
        Files.write(targetPath, bytes);

        // Compute SHA-256 hash
        String sha256 = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            sha256 = HexFormat.of().formatHex(digest);
        } catch (Exception ignored) {}

        ResumeNode node = new ResumeNode(
                resumeId,
                studentId,
                originalFilename,
                contentType,
                file.getSize(),
                targetPath.toAbsolutePath().toString(),
                sha256,
                nextVersion,
                "Y"
        );
        ResumeNode saved = resumeRepository.save(node);
        return toDto(saved);
    }

    @Override
    public List<ResumeDto> getResumesByStudent(String studentId) {
        return resumeRepository.findByStudentIdOrderByVersionNoDesc(studentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ResumeDto getCurrentResume(String studentId) {
        return resumeRepository.findCurrentByStudentId(studentId)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    public ResumeDto getResumeById(String resumeId) {
        ResumeNode node = resumeRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));
        return toDto(node);
    }

    @Override
    public byte[] getResumeBytes(String resumeId) {
        ResumeNode node = resumeRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));
        if (node.getFilePath() != null) {
            Path path = Paths.get(node.getFilePath());
            if (Files.exists(path)) {
                try {
                    return Files.readAllBytes(path);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read resume file from disk: " + e.getMessage(), e);
                }
            }
        }
        return new byte[0];
    }

    @Override
    public String getResumeContentType(String resumeId) {
        return resumeRepository.findByResumeId(resumeId)
                .map(ResumeNode::getContentType)
                .orElse("application/pdf");
    }

    @Override
    public String getResumeFileName(String resumeId) {
        return resumeRepository.findByResumeId(resumeId)
                .map(ResumeNode::getFileName)
                .orElse("resume.pdf");
    }

    @Override
    @Transactional
    public ResumeDto deleteResume(String authenticatedStudentId, String resumeId, boolean isAdmin) {
        ResumeNode resume = resumeRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));

        if (!isAdmin && (authenticatedStudentId == null || !resume.getStudentId().equalsIgnoreCase(authenticatedStudentId))) {
            throw new AccessDeniedException("Access denied: You may only delete your own resume records.");
        }

        if ("Y".equalsIgnoreCase(resume.getIsCurrent())) {
            List<ResumeNode> studentResumes = resumeRepository.findByStudentIdOrderByVersionNoDesc(resume.getStudentId());
            if (studentResumes.size() <= 1) {
                throw new IllegalStateException("This is your active resume. Upload another resume and activate it before deleting this version.");
            } else {
                throw new IllegalStateException("Cannot delete active resume (Version " + resume.getVersionNo() + "). Please activate another resume version first before deleting this version.");
            }
        }

        // Delete from disk if present
        if (resume.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(resume.getFilePath()));
            } catch (Exception ignored) {}
        }

        try {
            evaluationRepository.deleteByResumeId(resumeId);
        } catch (Exception ignored) {}

        ResumeDto dto = toDto(resume);
        resumeRepository.delete(resume);
        return dto;
    }

    @Override
    @Transactional
    public ResumeDto activateResume(String authenticatedStudentId, String resumeId, boolean isAdmin) {
        ResumeNode target = resumeRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + resumeId));

        if (!isAdmin && (authenticatedStudentId == null || !target.getStudentId().equalsIgnoreCase(authenticatedStudentId))) {
            throw new AccessDeniedException("Access denied: You may only activate your own resume records.");
        }

        List<ResumeNode> allResumes = resumeRepository.findByStudentIdOrderByVersionNoDesc(target.getStudentId());
        for (ResumeNode r : allResumes) {
            r.setIsCurrent(r.getResumeId().equalsIgnoreCase(resumeId) ? "Y" : "N");
        }
        resumeRepository.saveAll(allResumes);
        return toDto(target);
    }

    private ResumeDto toDto(ResumeNode n) {
        return new ResumeDto(
                n.getResumeId(),
                n.getStudentId(),
                n.getFileName(),
                n.getContentType(),
                n.getFileSize(),
                n.getUploadedAt(),
                n.getVersionNo(),
                n.getIsCurrent()
        );
    }
}
