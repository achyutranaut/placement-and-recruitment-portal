package com.placement.portal.program;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Date;
import java.time.LocalDate;

@Repository
public class ProgramJdbcDao {

    private static final Logger log = LoggerFactory.getLogger(ProgramJdbcDao.class);
    private final JdbcTemplate jdbcTemplate;

    public ProgramJdbcDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes Oracle Stored Procedure: REGISTER_STUDENT_PROGRAM
     */
    public void registerStudentForProgram(String studentId, String programId, LocalDate regDate) {
        jdbcTemplate.execute((java.sql.Connection conn) -> {
            boolean isOracle = conn.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle");
            LocalDate date = regDate != null ? regDate : LocalDate.now();

            if (isOracle) {
                try (CallableStatement cs = conn.prepareCall("{call REGISTER_STUDENT_PROGRAM(?, ?, ?)}")) {
                    cs.setString(1, studentId);
                    cs.setString(2, programId);
                    cs.setDate(3, Date.valueOf(date));
                    cs.execute();
                    log.info("Oracle PL/SQL procedure REGISTER_STUDENT_PROGRAM executed for student: {}", studentId);
                    return null;
                }
            } else {
                Integer existing = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM REGISTERS WHERE Student_Id = ? AND Program_Id = ?",
                        Integer.class, studentId, programId
                );
                if (existing != null && existing > 0) {
                    throw new IllegalArgumentException("Student is already enrolled in this training program.");
                }

                jdbcTemplate.update(
                        "INSERT INTO REGISTERS (Student_Id, Program_Id, Reg_Date) VALUES (?, ?, ?)",
                        studentId, programId, Date.valueOf(date)
                );
                log.info("H2 simulated REGISTER_STUDENT_PROGRAM executed for student: {}", studentId);
                return null;
            }
        });
    }
}
