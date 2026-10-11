package com.placement.portal.neo4j.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Node("Resume")
public class ResumeNode {

    @Id
    private String resumeId;

    private String studentId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String filePath;
    private String sha256Hash;
    private LocalDateTime uploadedAt = LocalDateTime.now();
    private Integer versionNo = 1;
    private String isCurrent = "Y";

// student and evaluations mapped via ID/Repository

    public ResumeNode() {}

    public ResumeNode(String resumeId, String studentId, String fileName, String contentType,
                      Long fileSize, String filePath, String sha256Hash, Integer versionNo, String isCurrent) {
        this.resumeId = resumeId;
        this.studentId = studentId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.filePath = filePath;
        this.sha256Hash = sha256Hash;
        this.uploadedAt = LocalDateTime.now();
        this.versionNo = versionNo;
        this.isCurrent = isCurrent;
    }

    public String getResumeId() { return resumeId; }
    public void setResumeId(String resumeId) { this.resumeId = resumeId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }

    public String getIsCurrent() { return isCurrent; }
    public void setIsCurrent(String isCurrent) { this.isCurrent = isCurrent; }


}
