package com.placement.portal.offer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;

@Repository
public class OfferJdbcDao {

    private static final Logger log = LoggerFactory.getLogger(OfferJdbcDao.class);
    private final JdbcTemplate jdbcTemplate;

    public OfferJdbcDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes Oracle Stored Procedure: ISSUE_OFFER
     */
    public String issueOffer(String applicationId, BigDecimal ctcLpa, LocalDate offerDate) {
        return jdbcTemplate.execute((java.sql.Connection conn) -> {
            boolean isOracle = conn.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle");
            LocalDate date = offerDate != null ? offerDate : LocalDate.now();

            if (isOracle) {
                try (CallableStatement cs = conn.prepareCall("{call ISSUE_OFFER(?, ?, ?, ?)}")) {
                    cs.setString(1, applicationId);
                    cs.setBigDecimal(2, ctcLpa);
                    cs.setDate(3, Date.valueOf(date));
                    cs.registerOutParameter(4, Types.VARCHAR);
                    cs.execute();
                    String offerId = cs.getString(4);
                    log.info("Oracle PL/SQL ISSUE_OFFER generated Offer ID: {}", offerId);
                    return offerId;
                }
            } else {
                Integer maxNum = 0;
                try {
                    maxNum = jdbcTemplate.queryForObject(
                            "SELECT NVL(MAX(CAST(SUBSTRING(Offer_Id, 4) AS INT)), 0) FROM OFFER_LETTER", Integer.class);
                } catch (Exception e) {
                    maxNum = jdbcTemplate.queryForObject(
                            "SELECT NVL(COUNT(*), 0) FROM OFFER_LETTER", Integer.class);
                }
                String offerId = "OFF" + String.format("%03d", (maxNum != null ? maxNum : 0) + 1);

                jdbcTemplate.update(
                        "INSERT INTO OFFER_LETTER (Offer_Id, Application_Id, Offer_Date, CTC_LPA, Status) VALUES (?, ?, ?, ?, ?)",
                        offerId, applicationId, Date.valueOf(date), ctcLpa, "OFFERED"
                );
                // Trigger simulation on H2
                jdbcTemplate.update(
                        "UPDATE APPLICATION SET Status = 'OFFERED' WHERE Application_Id = ?",
                        applicationId
                );
                log.info("H2 simulated ISSUE_OFFER generated Offer ID: {}", offerId);
                return offerId;
            }
        });
    }
}
