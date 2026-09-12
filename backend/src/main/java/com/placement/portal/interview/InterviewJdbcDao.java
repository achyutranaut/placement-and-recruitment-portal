package com.placement.portal.interview;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;

@Repository
public class InterviewJdbcDao {

    private static final Logger log = LoggerFactory.getLogger(InterviewJdbcDao.class);
    private final JdbcTemplate jdbcTemplate;

    public InterviewJdbcDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes Oracle Stored Procedure: SCHEDULE_INTERVIEW
     */
    public void scheduleInterview(String applicationId, String interviewerName, String online, String offline) {
        jdbcTemplate.execute((java.sql.Connection conn) -> {
            boolean isOracle = conn.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle");

            if (isOracle) {
                try (CallableStatement cs = conn.prepareCall("{call SCHEDULE_INTERVIEW(?, ?, ?, ?)}")) {
                    cs.setString(1, applicationId);
                    cs.setString(2, interviewerName);
                    cs.setString(3, online != null ? online : "Y");
                    cs.setString(4, offline != null ? offline : "N");
                    cs.execute();
                    log.info("Oracle PL/SQL SCHEDULE_INTERVIEW executed for app: {}", applicationId);
                    return null;
                }
            } else {
                jdbcTemplate.update(
                        "INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, Result, \"ONLINE\", \"OFFLINE\") VALUES (?, ?, 'PENDING', ?, ?)",
                        applicationId, interviewerName, online != null ? online : "Y", offline != null ? offline : "N"
                );
                jdbcTemplate.update(
                        "UPDATE APPLICATION SET Status = 'INTERVIEWING' WHERE Application_Id = ?",
                        applicationId
                );
                log.info("H2 simulated SCHEDULE_INTERVIEW executed for app: {}", applicationId);
                return null;
            }
        });
    }
}
