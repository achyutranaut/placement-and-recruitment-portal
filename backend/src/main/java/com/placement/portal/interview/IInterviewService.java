package com.placement.portal.interview;

import java.util.List;

public interface IInterviewService {
    void scheduleInterview(ScheduleInterviewDto dto);
    List<InterviewDto> getInterviewsByApplication(String applicationId);
    List<InterviewDto> getInterviewsByStudent(String studentId);
    List<InterviewDto> getAllInterviews();
    List<InterviewerRound> getAllInterviewerRounds();
    InterviewDto recordInterviewResult(String applicationId, String interviewerName, InterviewResultDto resultDto);
    boolean areAllThreeRoundsCleared(String applicationId);
}
