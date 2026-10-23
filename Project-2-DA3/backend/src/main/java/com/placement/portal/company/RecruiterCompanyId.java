package com.placement.portal.company;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for RECRUITER_COMPANY junction table.
 */
public class RecruiterCompanyId implements Serializable {
    private String userId;
    private String companyId;

    public RecruiterCompanyId() {}

    public RecruiterCompanyId(String userId, String companyId) {
        this.userId = userId;
        this.companyId = companyId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecruiterCompanyId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(companyId, that.companyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, companyId);
    }
}
