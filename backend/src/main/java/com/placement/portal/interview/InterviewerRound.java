package com.placement.portal.interview;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "INTERVIEWER_ROUND")
public class InterviewerRound {

    @Id
    @Column(name = "Interviewer_Name", length = 100, nullable = false)
    private String interviewerName;

    @Column(name = "Interview_Round_No", nullable = false)
    private Integer interviewRoundNo;

    public InterviewerRound() {}

    public InterviewerRound(String interviewerName, Integer interviewRoundNo) {
        this.interviewerName = interviewerName;
        this.interviewRoundNo = interviewRoundNo;
    }

    public String getInterviewerName() {
        return interviewerName;
    }

    public void setInterviewerName(String interviewerName) {
        this.interviewerName = interviewerName;
    }

    public Integer getInterviewRoundNo() {
        return interviewRoundNo;
    }

    public void setInterviewRoundNo(Integer interviewRoundNo) {
        this.interviewRoundNo = interviewRoundNo;
    }
}
