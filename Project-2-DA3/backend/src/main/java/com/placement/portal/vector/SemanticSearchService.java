package com.placement.portal.vector;

import com.placement.portal.company.Company;
import com.placement.portal.company.CompanyRepository;
import com.placement.portal.company.EmailCompany;
import com.placement.portal.company.EmailCompanyRepository;
import com.placement.portal.company.JobCompany;
import com.placement.portal.company.JobCompanyRepository;
import com.placement.portal.recruitment.PlacementDrive;
import com.placement.portal.recruitment.PlacementDriveRepository;
import com.placement.portal.student.Student;
import com.placement.portal.student.StudentRepository;
import com.placement.portal.student.StudentResume;
import com.placement.portal.student.StudentResumeRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class SemanticSearchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticSearchService.class);

    private final QdrantClientService qdrantClient;
    private final EmbeddingService embeddingService;
    private final StudentRepository studentRepository;
    private final PlacementDriveRepository driveRepository;
    private final StudentResumeRepository resumeRepository;
    private final CompanyRepository companyRepository;
    private final JobCompanyRepository jobCompanyRepository;
    private final EmailCompanyRepository emailCompanyRepository;

    @Autowired
    public SemanticSearchService(
            QdrantClientService qdrantClient,
            EmbeddingService embeddingService,
            StudentRepository studentRepository,
            PlacementDriveRepository driveRepository,
            StudentResumeRepository resumeRepository,
            CompanyRepository companyRepository,
            JobCompanyRepository jobCompanyRepository,
            EmailCompanyRepository emailCompanyRepository
    ) {
        this.qdrantClient = qdrantClient;
        this.embeddingService = embeddingService;
        this.studentRepository = studentRepository;
        this.driveRepository = driveRepository;
        this.resumeRepository = resumeRepository;
        this.companyRepository = companyRepository;
        this.jobCompanyRepository = jobCompanyRepository;
        this.emailCompanyRepository = emailCompanyRepository;
    }

    /**
     * Executes authorized semantic recruitment search against Qdrant Vector Database.
     */
    public SemanticSearchResponse search(SemanticSearchRequest request, String userRole) {
        long startTime = System.currentTimeMillis();

        if (!qdrantClient.isConnected()) {
            throw new IllegalStateException("Qdrant vector database is currently disabled or unreachable. " +
                    "Configure PORTAL_QDRANT_ENABLED=true and ensure Qdrant service is active.");
        }

        String query = request.getQuery() != null ? request.getQuery().trim() : "";
        if (query.isBlank()) {
            return new SemanticSearchResponse(query, Collections.emptyList(), true, "Qdrant",
                    qdrantClient.getCollectionName(), qdrantClient.getDimension(), 0);
        }

        // Restrict student searches strictly to placement drives to protect candidate privacy
        Map<String, Object> filter = new HashMap<>();
        String requestedType = request.getEntityType() != null ? request.getEntityType().trim().toUpperCase(Locale.ROOT) : "ALL";

        if ("ROLE_STUDENT".equalsIgnoreCase(userRole)) {
            filter.put("entityType", "DRIVE");
        } else if (!"ALL".equals(requestedType) && !requestedType.isBlank()) {
            filter.put("entityType", requestedType);
        }

        int limit = request.getLimit() != null ? Math.max(1, Math.min(request.getLimit(), 50)) : 10;
        float minScore = request.getMinScore() != null ? request.getMinScore() : 0.20f;

        // 1. Generate query embedding vector
        float[] queryVector = embeddingService.generateEmbedding(query);

        // 2. Perform vector search in Qdrant
        List<QdrantClientService.QdrantScoredPoint> scoredPoints = qdrantClient.search(
                queryVector,
                limit * 2, // oversample to allow filtering
                filter.isEmpty() ? null : filter
        );

        // 3. Enrich scored points with Oracle entity information
        List<SemanticSearchResultDto> results = new ArrayList<>();
        for (QdrantClientService.QdrantScoredPoint sp : scoredPoints) {
            if (sp.score < minScore) {
                continue;
            }

            Map<String, Object> payload = sp.payload != null ? sp.payload : Collections.emptyMap();
            String entityType = (String) payload.getOrDefault("entityType", "UNKNOWN");
            String entityId = (String) payload.getOrDefault("entityId", "");
            String title = (String) payload.getOrDefault("title", entityId);
            String subtitle = (String) payload.getOrDefault("subtitle", "");
            String summary = (String) payload.getOrDefault("summary", "");

            List<String> tags = new ArrayList<>();
            Object tagsObj = payload.get("tags");
            if (tagsObj instanceof List<?>) {
                for (Object o : (List<?>) tagsObj) {
                    if (o != null) tags.add(o.toString());
                }
            } else if (tagsObj instanceof String) {
                tags.addAll(Arrays.asList(((String) tagsObj).split(",\\s*")));
            }

            results.add(new SemanticSearchResultDto(
                    entityId,
                    entityType,
                    title,
                    subtitle,
                    roundScore(sp.score),
                    summary,
                    tags,
                    payload
            ));

            if (results.size() >= limit) {
                break;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        return new SemanticSearchResponse(
                query,
                results,
                true,
                "Qdrant Vector Database",
                qdrantClient.getCollectionName(),
                qdrantClient.getDimension(),
                duration
        );
    }

    /**
     * Reads all eligible student, drive, and resume records from Oracle
     * and synchronizes vector embeddings into Qdrant.
     */
    @Transactional(readOnly = true)
    public IndexSummaryDto reindexAll() {
        long startTime = System.currentTimeMillis();

        if (!qdrantClient.isConnected()) {
            throw new IllegalStateException("Cannot reindex: Qdrant service is disabled or unreachable. Check QDRANT_URL.");
        }

        // Ensure collection exists and is reset for fresh synchronization
        qdrantClient.clearCollection();

        List<QdrantClientService.PointRecord> points = new ArrayList<>();
        int studentCount = 0;
        int driveCount = 0;
        int resumeCount = 0;

        // 1. Index Students from Oracle
        List<Student> students = studentRepository.findAll();
        for (Student s : students) {
            String skillsText = (s.getSkills() != null && !s.getSkills().isEmpty()) ? String.join(", ", s.getSkills()) : "";
            String searchText = "Candidate Profile: " + s.getName()
                    + " | Branch: " + (s.getBranch() != null ? s.getBranch() : "")
                    + " | Degree Program: " + (s.getProgramId() != null ? s.getProgramId() : "")
                    + " | Technical Skills: " + skillsText
                    + " | CGPA: " + (s.getCgpa() != null ? s.getCgpa() : "")
                    + " | Placement Status: " + (s.getPlacementStatus() != null ? s.getPlacementStatus() : "NOT_PLACED");

            float[] vec = embeddingService.generateEmbedding(searchText);
            String pointId = generateUuid("STUDENT", s.getStudentId());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("entityType", "STUDENT");
            payload.put("entityId", s.getStudentId());
            payload.put("title", s.getName());
            payload.put("subtitle", (s.getBranch() != null ? s.getBranch() : "") + " (CGPA: " + s.getCgpa() + ")");
            payload.put("summary", "Technical Skills: " + (skillsText.isBlank() ? "General CS & IT" : skillsText));
            payload.put("skills", skillsText);
            payload.put("tags", s.getSkills() != null ? s.getSkills() : Collections.emptyList());
            payload.put("cgpa", s.getCgpa() != null ? s.getCgpa().doubleValue() : 0.0);
            payload.put("branch", s.getBranch() != null ? s.getBranch() : "");
            payload.put("sourceDb", "Oracle 23ai");

            points.add(new QdrantClientService.PointRecord(pointId, vec, payload));
            studentCount++;
        }

        // 2. Index Placement Drives from Oracle
        List<PlacementDrive> drives = driveRepository.findAll();
        for (PlacementDrive d : drives) {
            String companyName = resolveCompanyName(d.getJobTitle());
            String searchText = "Placement Recruitment Drive: " + d.getJobTitle()
                    + " | Company: " + companyName
                    + " | CTC Package: " + d.getCtc() + " LPA"
                    + " | Location: " + d.getLocation()
                    + " | Minimum CGPA: " + d.getMinCgpa()
                    + " | Job Requirements: " + d.getJobDescription()
                    + " | Eligible Disciplines: " + d.getEligibleBranches()
                    + " | Selection Rounds: " + d.getSelectionProcess();

            float[] vec = embeddingService.generateEmbedding(searchText);
            String pointId = generateUuid("DRIVE", d.getDriveId());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("entityType", "DRIVE");
            payload.put("entityId", d.getDriveId());
            payload.put("title", d.getJobTitle());
            payload.put("subtitle", companyName + " • " + d.getCtc() + " LPA (" + d.getLocation() + ")");
            payload.put("summary", d.getJobDescription() != null ? d.getJobDescription() : "");
            payload.put("companyName", companyName);
            payload.put("ctc", d.getCtc() != null ? d.getCtc().doubleValue() : 0.0);
            payload.put("location", d.getLocation() != null ? d.getLocation() : "");
            payload.put("eligibleBranches", d.getEligibleBranches() != null ? d.getEligibleBranches() : "");
            payload.put("tags", List.of(companyName, d.getLocation() != null ? d.getLocation() : "Campus"));
            payload.put("sourceDb", "Oracle 23ai");

            points.add(new QdrantClientService.PointRecord(pointId, vec, payload));
            driveCount++;
        }

        // 3. Index Resumes from Oracle
        List<StudentResume> resumes = resumeRepository.findAll();
        for (StudentResume r : resumes) {
            String resumeSnippet = extractPdfText(r.getResumeData());
            if (resumeSnippet.length() > 1000) {
                resumeSnippet = resumeSnippet.substring(0, 1000);
            }

            String searchText = "Student Resume: " + r.getFileName()
                    + " | Student ID: " + r.getStudentId()
                    + " | Content: " + resumeSnippet;

            float[] vec = embeddingService.generateEmbedding(searchText);
            String pointId = generateUuid("RESUME", r.getResumeId());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("entityType", "RESUME");
            payload.put("entityId", r.getResumeId());
            payload.put("studentId", r.getStudentId());
            payload.put("title", "Resume: " + r.getFileName());
            payload.put("subtitle", "Candidate: " + r.getStudentId());
            payload.put("summary", resumeSnippet.length() > 200 ? resumeSnippet.substring(0, 200) + "..." : resumeSnippet);
            payload.put("tags", List.of("Resume", "PDF"));
            payload.put("sourceDb", "Oracle 23ai");

            points.add(new QdrantClientService.PointRecord(pointId, vec, payload));
            resumeCount++;
        }

        // 4. Batch upsert into Qdrant
        boolean success = qdrantClient.upsertPointsBatch(points);
        long duration = System.currentTimeMillis() - startTime;
        long totalCount = qdrantClient.getPointCount();

        log.info("Synchronized {} records from Oracle into Qdrant collection '{}' in {}ms (Students: {}, Drives: {}, Resumes: {}).",
                points.size(), qdrantClient.getCollectionName(), duration, studentCount, driveCount, resumeCount);

        return new IndexSummaryDto(
                success,
                studentCount,
                driveCount,
                resumeCount,
                totalCount,
                duration,
                qdrantClient.getCollectionName(),
                success ? "Successfully indexed all Oracle records into Qdrant vector database."
                        : "Partial or failed synchronization with Qdrant."
        );
    }

    /**
     * Synchronizes a single placement drive into Qdrant.
     */
    @Transactional(readOnly = true)
    public boolean indexDrive(String driveId) {
        if (!qdrantClient.isConnected()) return false;
        return driveRepository.findById(driveId).map(d -> {
            String companyName = resolveCompanyName(d.getJobTitle());
            String searchText = "Placement Recruitment Drive: " + d.getJobTitle()
                    + " | Company: " + companyName
                    + " | CTC Package: " + d.getCtc() + " LPA"
                    + " | Location: " + d.getLocation()
                    + " | Minimum CGPA: " + d.getMinCgpa()
                    + " | Job Requirements: " + d.getJobDescription()
                    + " | Eligible Disciplines: " + d.getEligibleBranches()
                    + " | Selection Rounds: " + d.getSelectionProcess();

            float[] vec = embeddingService.generateEmbedding(searchText);
            String pointId = generateUuid("DRIVE", d.getDriveId());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("entityType", "DRIVE");
            payload.put("entityId", d.getDriveId());
            payload.put("title", d.getJobTitle());
            payload.put("subtitle", companyName + " • " + d.getCtc() + " LPA (" + d.getLocation() + ")");
            payload.put("summary", d.getJobDescription() != null ? d.getJobDescription() : "");
            payload.put("companyName", companyName);
            payload.put("ctc", d.getCtc() != null ? d.getCtc().doubleValue() : 0.0);
            payload.put("location", d.getLocation() != null ? d.getLocation() : "");
            payload.put("eligibleBranches", d.getEligibleBranches() != null ? d.getEligibleBranches() : "");
            payload.put("tags", List.of(companyName, d.getLocation() != null ? d.getLocation() : "Campus"));
            payload.put("sourceDb", "Oracle 23ai");

            return qdrantClient.upsertPoint(pointId, vec, payload);
        }).orElse(false);
    }

    private String resolveCompanyName(String jobTitle) {
        if (jobTitle == null) return "Corporate Partner";
        return jobCompanyRepository.findById(jobTitle)
                .flatMap(jc -> companyRepository.findById(jc.getCompanyId()))
                .flatMap(c -> emailCompanyRepository.findById(c.getEmail()))
                .map(EmailCompany::getCompanyName)
                .orElse("Corporate Partner");
    }

    private String extractPdfText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) return "";
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text != null ? text.trim() : "";
        } catch (Exception e) {
            try {
                return new String(pdfBytes, StandardCharsets.UTF_8).trim();
            } catch (Exception ignored) {
                return "";
            }
        }
    }

    private static String generateUuid(String entityType, String entityId) {
        return UUID.nameUUIDFromBytes((entityType + ":" + entityId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static float roundScore(float score) {
        return Math.round(score * 1000.0f) / 1000.0f;
    }
}
