package com.placement.portal.student;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IResumeService {
    ResumeDto uploadResume(String studentId, MultipartFile file) throws IOException;
    List<ResumeDto> getResumesByStudent(String studentId);
    ResumeDto getCurrentResume(String studentId);
    ResumeDto getResumeById(String resumeId);
    byte[] getResumeBytes(String resumeId);
    String getResumeContentType(String resumeId);
    String getResumeFileName(String resumeId);
    ResumeDto deleteResume(String authenticatedStudentId, String resumeId, boolean isAdmin);
    ResumeDto activateResume(String authenticatedStudentId, String resumeId, boolean isAdmin);
}
