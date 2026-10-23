package com.placement.portal.reports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

@Repository
public class ReportJdbcDao {

    private static final Logger log = LoggerFactory.getLogger(ReportJdbcDao.class);
    private final JdbcTemplate jdbcTemplate;

    public ReportJdbcDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Calls PL/SQL Function: GET_PLACEMENT_RATE
     */
    public Double getPlacementRate(String programId) {
        try {
            return jdbcTemplate.execute((java.sql.Connection conn) -> {
                try (CallableStatement cs = conn.prepareCall("{? = call GET_PLACEMENT_RATE(?)}")) {
                    cs.registerOutParameter(1, Types.NUMERIC);
                    cs.setString(2, programId);
                    cs.execute();
                    return cs.getDouble(1);
                }
            });
        } catch (Exception e) {
            log.warn("PL/SQL function GET_PLACEMENT_RATE not available in dialect. Calculating via SQL query.");
            Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM STUDENT", Integer.class);
            Integer placed = jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT a.Student_Id) FROM OFFER_LETTER ol JOIN APPLICATION a ON ol.Application_Id = a.Application_Id", Integer.class);
            if (total != null && total > 0 && placed != null) {
                return Math.round(((double) placed / total * 100.0) * 100.0) / 100.0;
            }
            return 0.0;
        }
    }

    /**
     * Calls PL/SQL Function: GET_AVERAGE_PACKAGE
     */
    public Double getAveragePackage(String industry) {
        try {
            return jdbcTemplate.execute((java.sql.Connection conn) -> {
                try (CallableStatement cs = conn.prepareCall("{? = call GET_AVERAGE_PACKAGE(?)}")) {
                    cs.registerOutParameter(1, Types.NUMERIC);
                    cs.setString(2, industry);
                    cs.execute();
                    return cs.getDouble(1);
                }
            });
        } catch (Exception e) {
            log.warn("PL/SQL function GET_AVERAGE_PACKAGE not available in dialect. Calculating via SQL query.");
            Double avg = jdbcTemplate.queryForObject("SELECT NVL(ROUND(AVG(CTC_LPA), 2), 0) FROM OFFER_LETTER", Double.class);
            return avg != null ? avg : 0.0;
        }
    }

    /**
     * Calls PL/SQL Function: GET_STUDENT_APPLICATION_COUNT
     */
    public Integer getStudentApplicationCount(String studentId) {
        try {
            return jdbcTemplate.execute((java.sql.Connection conn) -> {
                try (CallableStatement cs = conn.prepareCall("{? = call GET_STUDENT_APPLICATION_COUNT(?)}")) {
                    cs.registerOutParameter(1, Types.INTEGER);
                    cs.setString(2, studentId);
                    cs.execute();
                    return cs.getInt(1);
                }
            });
        } catch (Exception e) {
            log.warn("PL/SQL function GET_STUDENT_APPLICATION_COUNT fallback.");
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM APPLICATION WHERE Student_Id = ?", Integer.class, studentId);
        }
    }

    /**
     * Queries company hiring stats using DQL aggregation (GROUP BY / HAVING)
     */
    public List<Map<String, Object>> getCompanyHiringStats() {
        String sql = """
            SELECT 
                ec.Company_Name AS COMPANY_NAME,
                c.Industry AS INDUSTRY,
                COUNT(ol.Offer_Id) AS TOTAL_OFFERS,
                ROUND(AVG(ol.CTC_LPA), 2) AS AVERAGE_CTC,
                MAX(ol.CTC_LPA) AS HIGHEST_CTC,
                MIN(ol.CTC_LPA) AS LOWEST_CTC
            FROM OFFER_LETTER ol
            JOIN APPLICATION a ON ol.Application_Id = a.Application_Id
            JOIN STUDENT_DAILY_ROUTINE_DRIVE sdr ON a.Student_Id = sdr.Student_Id AND a.Apply_Date = sdr.Apply_Date
            JOIN PLACEMENT_DRIVE d ON sdr.Drive_Id = d.Drive_Id
            JOIN JOB_COMPANY jc ON d.Job_Title = jc.Job_Title
            JOIN COMPANY c ON jc.Company_Id = c.Company_Id
            JOIN EMAIL_COMPANY ec ON c.Email = ec.Email
            GROUP BY ec.Company_Name, c.Industry
            ORDER BY AVERAGE_CTC DESC
        """;
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Aggregates application counts by status directly from the database using SQL GROUP BY.
     * Pre-populates all canonical statuses defined in CHK_APP_STATUS (V15 migration).
     */
    public Map<String, Long> getApplicationStatusCounts() {
        Map<String, Long> statusCounts = new java.util.LinkedHashMap<>();
        // All canonical statuses per CHK_APP_STATUS constraint
        statusCounts.put("APPLIED", 0L);
        statusCounts.put("SHORTLISTED", 0L);
        statusCounts.put("INTERVIEWING", 0L);
        statusCounts.put("SELECTED", 0L);
        statusCounts.put("OFFERED", 0L);
        statusCounts.put("ACCEPTED", 0L);
        statusCounts.put("DECLINED", 0L);
        statusCounts.put("REJECTED", 0L);

        String sql = "SELECT UPPER(TRIM(Status)) AS APP_STATUS, COUNT(*) AS STATUS_COUNT FROM APPLICATION GROUP BY UPPER(TRIM(Status))";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                String status = null;
                Number count = null;
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if ("APP_STATUS".equalsIgnoreCase(entry.getKey())) {
                        status = entry.getValue() != null ? entry.getValue().toString().trim().toUpperCase() : null;
                    } else if ("STATUS_COUNT".equalsIgnoreCase(entry.getKey())) {
                        count = (Number) entry.getValue();
                    }
                }
                if (status != null && count != null) {
                    statusCounts.put(status, count.longValue());
                }
            }
        } catch (Exception e) {
            log.error("Failed to query application status counts via SQL GROUP BY: {}", e.getMessage());
        }
        return statusCounts;
    }
}

