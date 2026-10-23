package com.placement.portal.interview;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "INTERVIEW")
@IdClass(Interview.InterviewId.class)
public class Interview {

    @Id
    @Column(name = "Application_Id", length = 20, nullable = false)
    private String applicationId;

    @Id
    @Column(name = "Interviewer_Name", length = 100, nullable = false)
    private String interviewerName;

    @Column(name = "OA", precision = 5, scale = 2)
    private BigDecimal oa;

    @Column(name = "GD", precision = 5, scale = 2)
    private BigDecimal gd;

    @Column(name = "HR", precision = 5, scale = 2)
    private BigDecimal hr;

    @Column(name = "Result", length = 30, nullable = false)
    private String result = "PENDING";

    @Column(name = "\"ONLINE\"", length = 1, nullable = false)
    private String online = "Y";

    @Column(name = "\"OFFLINE\"", length = 1, nullable = false)
    private String offline = "N";

    public Interview() {}

    public Interview(String applicationId, String interviewerName, BigDecimal oa, BigDecimal gd, BigDecimal hr, String result, String online, String offline) {
        this.applicationId = applicationId;
        this.interviewerName = interviewerName;
        this.oa = oa;
        this.gd = gd;
        this.hr = hr;
        this.result = result != null ? result : "PENDING";
        this.online = online != null ? online : "Y";
        this.offline = offline != null ? offline : "N";
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getInterviewerName() {
        return interviewerName;
    }

    public void setInterviewerName(String interviewerName) {
        this.interviewerName = interviewerName;
    }

    public BigDecimal getOa() {
        return oa;
    }

    public void setOa(BigDecimal oa) {
        this.oa = oa;
    }

    public BigDecimal getGd() {
        return gd;
    }

    public void setGd(BigDecimal gd) {
        this.gd = gd;
    }

    public BigDecimal getHr() {
        return hr;
    }

    public void setHr(BigDecimal hr) {
        this.hr = hr;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getOnline() {
        return online;
    }

    public void setOnline(String online) {
        this.online = online;
    }

    public String getOffline() {
        return offline;
    }

    public void setOffline(String offline) {
        this.offline = offline;
    }

    public static class InterviewId implements Serializable {
        private String applicationId;
        private String interviewerName;

        public InterviewId() {}

        public InterviewId(String applicationId, String interviewerName) {
            this.applicationId = applicationId;
            this.interviewerName = interviewerName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InterviewId that = (InterviewId) o;
            return Objects.equals(applicationId, that.applicationId) && Objects.equals(interviewerName, that.interviewerName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(applicationId, interviewerName);
        }
    }
}
