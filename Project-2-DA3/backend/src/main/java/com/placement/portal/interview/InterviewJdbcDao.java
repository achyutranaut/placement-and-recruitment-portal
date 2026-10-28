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

    public static String normalizeFlag(String val, String defaultVal) {
        if (val == null || val.isBlank()) return defaultVal;
        String s = val.trim();
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("y") || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("online") || s.equals("1")) {
            return "Y";
        }
        if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("n") || s.equalsIgnoreCase("no") || s.equalsIgnoreCase("offline") || s.equals("0")) {
            return "N";
        }
        return defaultVal;
    }

    /**
     * Executes Oracle Stored Procedure: SCHEDULE_INTERVIEW
     */
    public void scheduleInterview(String applicationId, String interviewerName, String online, String offline) {
        final String normOnline = normalizeFlag(online, "Y");
        final String normOffline = normalizeFlag(offline, "N");

        jdbcTemplate.execute((java.sql.Connection conn) -> {
            boolean isOracle = conn.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle");

            if (isOracle) {
                try (CallableStatement cs = conn.prepareCall("{call SCHEDULE_INTERVIEW(?, ?, ?, ?)}")) {
                    cs.setString(1, applicationId);
                    cs.setString(2, interviewerName);
                    cs.setString(3, normOnline);
                    cs.setString(4, normOffline);
                    cs.execute();
                    log.info("Oracle PL/SQL SCHEDULE_INTERVIEW executed for app: {}", applicationId);
                    return null;
                }
            } else {
                jdbcTemplate.update(
                        "INSERT INTO INTERVIEW (Application_Id, Interviewer_Name, Result, \"ONLINE\", \"OFFLINE\") VALUES (?, ?, 'PENDING', ?, ?)",
                        applicationId, interviewerName, normOnline, normOffline
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
