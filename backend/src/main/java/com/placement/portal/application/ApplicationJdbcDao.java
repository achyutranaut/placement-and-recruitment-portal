package com.placement.portal.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;

@Repository
public class ApplicationJdbcDao {

    private static final Logger log = LoggerFactory.getLogger(ApplicationJdbcDao.class);
    private final JdbcTemplate jdbcTemplate;

    public ApplicationJdbcDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes Oracle Stored Procedure: APPLY_FOR_DRIVE
     * Detects Oracle database product at runtime and executes native PL/SQL,
     * with transparent SQL fallback for H2 in-memory test profile.
     */
    public String applyForDrive(String studentId, String driveId, LocalDate applyDate) {
        return jdbcTemplate.execute((java.sql.Connection conn) -> {
            boolean isOracle = conn.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle");
            LocalDate date = applyDate != null ? applyDate : LocalDate.now();

            if (isOracle) {
                try (CallableStatement cs = conn.prepareCall("{call APPLY_FOR_DRIVE(?, ?, ?, ?)}")) {
                    cs.setString(1, studentId);
                    cs.setString(2, driveId);
                    cs.setDate(3, Date.valueOf(date));
                    cs.registerOutParameter(4, Types.VARCHAR);
                    cs.execute();
                    String appId = cs.getString(4);
                    log.info("Oracle PL/SQL APPLY_FOR_DRIVE generated Application ID: {}", appId);
                    return appId;
                } catch (java.sql.SQLException sqle) {
                    String msg = sqle.getMessage();
                    if (msg != null && (msg.contains("ORA-20011") || msg.contains("Daily limit reached"))) {
                        throw new IllegalArgumentException("Daily limit reached: A student may apply to at most one placement drive per calendar day.");
                    } else if (msg != null && (msg.contains("ORA-20010") || msg.contains("below minimum required CGPA"))) {
                        throw new IllegalArgumentException("Ineligible: Student CGPA is below the minimum required CGPA for this drive.");
                    } else if (msg != null && msg.contains("ORA-20")) {
                        int start = msg.indexOf("ORA-20");
                        int end = msg.indexOf("\n", start);
                        String clean = end != -1 ? msg.substring(start, end) : msg.substring(start);
                        clean = clean.replaceAll("ORA-20[0-9]{3}:\\s*", "").trim();
                        throw new IllegalArgumentException(clean);
                    }
                    throw sqle;
                }
            } else {
                // Offline / H2 execution engine matching exact PL/SQL procedure logic
                Integer dailyCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM STUDENT_DAILY_ROUTINE_DRIVE WHERE Student_Id = ? AND Apply_Date = ?",
                        Integer.class, studentId, Date.valueOf(date)
                );
                if (dailyCount != null && dailyCount > 0) {
                    throw new IllegalArgumentException("Daily limit reached: A student may apply to at most one drive per day.");
                }

                Double studentCgpa = jdbcTemplate.queryForObject(
                        "SELECT CGPA FROM STUDENT WHERE Student_Id = ?", Double.class, studentId);
                Double minCgpa = jdbcTemplate.queryForObject(
                        "SELECT Min_CGPA FROM PLACEMENT_DRIVE WHERE Drive_Id = ?", Double.class, driveId);
                if (studentCgpa != null && minCgpa != null && studentCgpa < minCgpa) {
                    throw new IllegalArgumentException("Ineligible: Student CGPA (" + studentCgpa + ") is below minimum required CGPA (" + minCgpa + ").");
                }

                // Verify Application Deadline
                Date deadline = null;
                try {
                    deadline = jdbcTemplate.queryForObject(
                            "SELECT Application_Deadline FROM PLACEMENT_DRIVE WHERE Drive_Id = ?", Date.class, driveId);
                } catch (Exception ignored) {}
                if (deadline != null && Date.valueOf(date).after(deadline)) {
                    throw new IllegalArgumentException("Ineligible: Application deadline has passed for drive " + driveId);
                }

                // Verify Program Eligibility via DRIVE_ELIGIBILITY junction
                try {
                    String progId = jdbcTemplate.queryForObject(
                            "SELECT Program_Id FROM STUDENT WHERE Student_Id = ?", String.class, studentId);
                    String p = progId != null ? progId : "BTECH-CSE";
                    Integer eligibleCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM DRIVE_ELIGIBILITY WHERE Drive_Id = ? AND (" +
                                    "Program_Id = ? OR " +
                                    "(Program_Id = 'BTECH-CSE' AND ? = 'BCE') OR (Program_Id = 'BCE' AND ? = 'BTECH-CSE') OR " +
                                    "(Program_Id = 'BTECH-IT' AND ? = 'BCT') OR (Program_Id = 'BCT' AND ? = 'BTECH-IT') OR " +
                                    "(Program_Id = 'BTECH-ECE' AND ? = 'BEC') OR (Program_Id = 'BEC' AND ? = 'BTECH-ECE') OR " +
                                    "(Program_Id = 'BTECH-MECH' AND ? = 'BME') OR (Program_Id = 'BME' AND ? = 'BTECH-MECH'))",
                            Integer.class, driveId, p, p, p, p, p, p, p, p, p);
                    if (eligibleCount != null && eligibleCount == 0) {
                        throw new IllegalArgumentException("Ineligible: Student academic program (" + progId + ") is not eligible for drive " + driveId);
                    }
                } catch (IllegalArgumentException iae) {
                    throw iae;
                } catch (Exception ignored) {
                    // If table or column not yet loaded in dynamic context, skip
                }

                Integer maxNum = jdbcTemplate.queryForObject(
                        "SELECT NVL(COUNT(*), 0) FROM APPLICATION", Integer.class);
                String appId = "APP" + String.format("%03d", (maxNum != null ? maxNum : 0) + 1);

                jdbcTemplate.update(
                        "INSERT INTO STUDENT_DAILY_ROUTINE_DRIVE (Student_Id, Apply_Date, Drive_Id) VALUES (?, ?, ?)",
                        studentId, Date.valueOf(date), driveId
                );
                jdbcTemplate.update(
                        "INSERT INTO APPLICATION (Application_Id, Student_Id, Drive_Id, Apply_Date, Status) VALUES (?, ?, ?, ?, 'APPLIED')",
                        appId, studentId, driveId, Date.valueOf(date)
                );
                log.info("H2 simulated APPLY_FOR_DRIVE generated Application ID: {}", appId);
                return appId;
            }
        });
    }
}
