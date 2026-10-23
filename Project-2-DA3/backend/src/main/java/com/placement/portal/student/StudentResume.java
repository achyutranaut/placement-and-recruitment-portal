package com.placement.portal.student;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "STUDENT_RESUME")
public class StudentResume {

    @Id
    @Column(name = "Resume_Id", length = 50, nullable = false)
    private String resumeId;

    @Column(name = "Student_Id", length = 20, nullable = false)
    private String studentId;

    @Column(name = "File_Name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "Content_Type", length = 100, nullable = false)
    private String contentType;

    @Column(name = "File_Size", nullable = false)
    private Long fileSize;

    @Lob
    @Column(name = "Resume_Data", nullable = false)
    private byte[] resumeData;

    @Column(name = "Uploaded_At", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "Version_No", nullable = false)
    private Integer versionNo = 1;

    @Column(name = "Is_Current", length = 1, nullable = false)
    private String isCurrent = "Y";

    public StudentResume() {}

    public StudentResume(String resumeId, String studentId, String fileName, String contentType, Long fileSize, byte[] resumeData, Integer versionNo, String isCurrent) {
        this.resumeId = resumeId;
        this.studentId = studentId;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.resumeData = resumeData;
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

    public byte[] getResumeData() { return resumeData; }
    public void setResumeData(byte[] resumeData) { this.resumeData = resumeData; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }

    public String getIsCurrent() { return isCurrent; }
    public void setIsCurrent(String isCurrent) { this.isCurrent = isCurrent; }
}
