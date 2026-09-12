package com.placement.portal.student;

import java.time.LocalDateTime;

public class ResumeDto {
    private String resumeId;
    private String studentId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private Integer versionNo;
    private String isCurrent;

    public ResumeDto() {}

    public ResumeDto(String resumeId, String studentId, String fileName, String contentType, Long fileSize, LocalDateTime uploadedAt, Integer versionNo, String isCurrent) {
        this.resumeId = resumeId;
        this.studentId = studentId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
        this.versionNo = versionNo;
        this.isCurrent = isCurrent;
    }

    public static ResumeDto fromEntity(StudentResume r) {
        return new ResumeDto(
                r.getResumeId(),
                r.getStudentId(),
                r.getFileName(),
                r.getContentType(),
                r.getFileSize(),
                r.getUploadedAt(),
                r.getVersionNo(),
                r.getIsCurrent()
        );
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

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }

    public String getIsCurrent() { return isCurrent; }
    public void setIsCurrent(String isCurrent) { this.isCurrent = isCurrent; }
}
