package com.placement.portal.neo4j.service;

import com.placement.portal.neo4j.dto.GraphShowcaseDto;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Profile("neo4j")
public class Neo4jGraphAnalyticsService {

    private final Neo4jClient neo4jClient;

    public Neo4jGraphAnalyticsService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    /**
     * Graph Showcase Query 1: Eligible Drives by Multi-hop Traversal
     * Pattern: (Student)-[:ENROLLED_IN]->(Program)<-[:ELIGIBLE_FOR]-(Drive)<-[:HOSTS_DRIVE]-(Company)
     * Filters on student's CGPA vs Drive's minCgpa.
     */
    public List<GraphShowcaseDto.EligibleDriveGraphDto> getEligibleDrivesForStudent(String studentId) {
        String cypher = """
            MATCH (s:Student {studentId: $studentId})-[:ENROLLED_IN]->(p:Program)<-[:ELIGIBLE_FOR]-(d:Drive)
            OPTIONAL MATCH (c:Company)-[:HOSTS_DRIVE]->(d)
            WHERE s.cgpa >= d.minCgpa AND (d.applicationDeadline IS NULL OR d.applicationDeadline >= date())
            RETURN d.driveId AS driveId,
                   d.jobTitle AS jobTitle,
                   coalesce(c.companyName, d.companyName, 'Corporate Recruiter') AS companyName,
                   d.ctc AS ctcLpa,
                   d.minCgpa AS minCgpa,
                   d.applicationDeadline AS deadline,
                   [(s.studentId), 'ENROLLED_IN', (p.programId), 'ELIGIBLE_FOR', (d.driveId)] AS traversalPath
            ORDER BY d.ctc DESC
        """;

        Collection<Map<String, Object>> rows = neo4jClient.query(cypher)
                .bind(studentId).to("studentId")
                .fetch().all();

        List<GraphShowcaseDto.EligibleDriveGraphDto> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String driveId = (String) r.get("driveId");
            String jobTitle = (String) r.get("jobTitle");
            String companyName = (String) r.get("companyName");
            BigDecimal ctc = parseBigDecimal(r.get("ctcLpa"));
            BigDecimal minCgpa = parseBigDecimal(r.get("minCgpa"));
            LocalDate deadline = (LocalDate) r.get("deadline");
            @SuppressWarnings("unchecked")
            List<String> path = (List<String>) r.get("traversalPath");
            list.add(new GraphShowcaseDto.EligibleDriveGraphDto(driveId, jobTitle, companyName, ctc, minCgpa, deadline, path));
        }
        return list;
    }

    /**
     * Graph Showcase Query 2: Full Placement Path
     * Pattern: (Student)-[:SUBMITTED_APPLICATION]->(Application)-[:FOR_DRIVE]->(Drive)<-[:HOSTS_DRIVE]-(Company)
     *          and (Application)-[:HAS_INTERVIEW]->(Interview)
     *          and (Application)-[:RECEIVED_OFFER]->(Offer)
     */
    public List<GraphShowcaseDto.PlacementPathDto> getFullPlacementPath(String studentId) {
        String cypher = """
            MATCH (s:Student {studentId: $studentId})
            OPTIONAL MATCH (s)-[:ENROLLED_IN]->(p:Program)
            MATCH (s)-[:SUBMITTED_APPLICATION]->(a:Application)-[:FOR_DRIVE]->(d:Drive)
            OPTIONAL MATCH (c:Company)-[:HOSTS_DRIVE]->(d)
            OPTIONAL MATCH (a)-[:RECEIVED_OFFER]->(o:Offer)
            RETURN s.studentId AS studentId,
                   s.name AS studentName,
                   coalesce(p.programName, s.branch, 'Academic Program') AS programName,
                   a.applicationId AS applicationId,
                   d.driveId AS driveId,
                   d.jobTitle AS jobTitle,
                   coalesce(c.companyName, d.companyName, 'Corporate Partner') AS companyName,
                   o.offerId AS offerId,
                   o.ctcLpa AS ctcLpa,
                   o.status AS offerStatus
            ORDER BY a.applyDate DESC
        """;

        Collection<Map<String, Object>> rows = neo4jClient.query(cypher)
                .bind(studentId).to("studentId")
                .fetch().all();

        List<GraphShowcaseDto.PlacementPathDto> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            GraphShowcaseDto.PlacementPathDto dto = new GraphShowcaseDto.PlacementPathDto();
            dto.setStudentId((String) r.get("studentId"));
            dto.setStudentName((String) r.get("studentName"));
            dto.setProgramName((String) r.get("programName"));
            String appId = (String) r.get("applicationId");
            dto.setApplicationId(appId);
            dto.setDriveId((String) r.get("driveId"));
            dto.setJobTitle((String) r.get("jobTitle"));
            dto.setCompanyName((String) r.get("companyName"));
            dto.setOfferId((String) r.get("offerId"));
            dto.setCtcLpa(parseBigDecimal(r.get("ctcLpa")));
            dto.setOfferStatus((String) r.get("offerStatus"));

            // Fetch interview steps for this application
            String ivCypher = """
                MATCH (a:Application {applicationId: $appId})-[:HAS_INTERVIEW]->(iv:Interview)
                RETURN iv.roundNo AS roundNo,
                       iv.interviewerName AS interviewerName,
                       iv.result AS result,
                       coalesce(iv.oa, iv.gd, iv.hr, 0.0) AS score
                ORDER BY iv.roundNo ASC
            """;
            Collection<Map<String, Object>> ivRows = neo4jClient.query(ivCypher)
                    .bind(appId).to("appId")
                    .fetch().all();

            List<GraphShowcaseDto.InterviewStepDto> steps = new ArrayList<>();
            for (Map<String, Object> ivr : ivRows) {
                Integer rNo = parseInteger(ivr.get("roundNo"), 1);
                String iName = (String) ivr.get("interviewerName");
                String res = (String) ivr.get("result");
                BigDecimal sc = parseBigDecimal(ivr.get("score"));
                steps.add(new GraphShowcaseDto.InterviewStepDto(rNo, iName, res, sc));
            }
            dto.setInterviewSteps(steps);
            list.add(dto);
        }
        return list;
    }

    /**
     * Graph Showcase Query 3: Companies Recruiting from a Program
     * Pattern: (Program)<-[:ELIGIBLE_FOR]-(Drive)<-[:HOSTS_DRIVE]-(Company)
     * Shows recruitment relationships and conversion stats directly connected through graph edges.
     */
    public List<GraphShowcaseDto.ProgramHiringPipelineDto> getCompaniesRecruitingFromProgram(String programId) {
        String cypher = """
            MATCH (p:Program {programId: $programId})<-[:ELIGIBLE_FOR]-(d:Drive)<-[:HOSTS_DRIVE]-(c:Company)
            WITH p, c, count(DISTINCT d) AS drivesCount
            OPTIONAL MATCH (c)-[:HOSTS_DRIVE]->(d2:Drive)<-[:FOR_DRIVE]-(a:Application)-[:RECEIVED_OFFER]->(o:Offer)
            WHERE (a)<-[:SUBMITTED_APPLICATION]-(:Student)-[:ENROLLED_IN]->(p)
            RETURN p.programId AS programId,
                   p.programName AS programName,
                   c.companyId AS companyId,
                   c.companyName AS companyName,
                   drivesCount,
                   count(DISTINCT o) AS offersCount,
                   coalesce(round(avg(toFloat(o.ctcLpa)) * 100.0) / 100.0, 0.0) AS averageCtc
            ORDER BY offersCount DESC, drivesCount DESC
        """;

        Collection<Map<String, Object>> rows = neo4jClient.query(cypher)
                .bind(programId).to("programId")
                .fetch().all();

        List<GraphShowcaseDto.ProgramHiringPipelineDto> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String progId = (String) r.get("programId");
            String progName = (String) r.get("programName");
            String compId = (String) r.get("companyId");
            String compName = (String) r.get("companyName");
            long dCount = parseLong(r.get("drivesCount"));
            long oCount = parseLong(r.get("offersCount"));
            double avgCtc = parseDouble(r.get("averageCtc"));
            list.add(new GraphShowcaseDto.ProgramHiringPipelineDto(progId, progName, compId, compName, dCount, oCount, avgCtc));
        }
        return list;
    }

    /**
     * Graph Showcase Query 4: "Similar Students Placed" Recommendation & Interviewer Workload
     * Pattern: Find other students who were enrolled in the SAME academic program,
     * had similar CGPA (within +/- 1.0 CGPA points), and got placed with job offers.
     */
    public List<GraphShowcaseDto.SimilarPlacedRecommendationDto> getSimilarStudentsPlaced(String studentId) {
        String cypher = """
            MATCH (target:Student {studentId: $studentId})-[:ENROLLED_IN]->(p:Program)<-[:ENROLLED_IN]-(peer:Student)
            WHERE peer.studentId <> target.studentId
              AND abs(toFloat(peer.cgpa) - toFloat(target.cgpa)) <= 1.5
            MATCH (peer)-[:SUBMITTED_APPLICATION]->(a:Application)-[:RECEIVED_OFFER]->(o:Offer)
            MATCH (a)-[:FOR_DRIVE]->(d:Drive)
            OPTIONAL MATCH (c:Company)-[:HOSTS_DRIVE]->(d)
            RETURN peer.studentId AS studentId,
                   peer.name AS studentName,
                   peer.cgpa AS cgpa,
                   coalesce(c.companyName, d.companyName, 'Corporate Partner') AS companyName,
                   d.jobTitle AS jobTitle,
                   o.ctcLpa AS ctcLpa,
                   round(abs(toFloat(peer.cgpa) - toFloat(target.cgpa)) * 100.0) / 100.0 AS diff
            ORDER BY diff ASC, o.ctcLpa DESC
            LIMIT 10
        """;

        Collection<Map<String, Object>> rows = neo4jClient.query(cypher)
                .bind(studentId).to("studentId")
                .fetch().all();

        List<GraphShowcaseDto.SimilarPlacedRecommendationDto> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String sId = (String) r.get("studentId");
            String sName = (String) r.get("studentName");
            BigDecimal cgpa = BigDecimal.ZERO;
            if (r.get("cgpa") instanceof Number n) {
                cgpa = BigDecimal.valueOf(n.doubleValue());
            } else if (r.get("cgpa") != null) {
                cgpa = new BigDecimal(r.get("cgpa").toString());
            }

            String compName = (String) r.get("companyName");
            String jobTitle = (String) r.get("jobTitle");

            BigDecimal ctc = BigDecimal.ZERO;
            if (r.get("ctcLpa") instanceof Number n) {
                ctc = BigDecimal.valueOf(n.doubleValue());
            } else if (r.get("ctcLpa") != null) {
                ctc = new BigDecimal(r.get("ctcLpa").toString());
            }

            double diff = 0.0;
            if (r.get("diff") instanceof Number n) {
                diff = n.doubleValue();
            } else if (r.get("diff") != null) {
                diff = Double.parseDouble(r.get("diff").toString());
            }
            list.add(new GraphShowcaseDto.SimilarPlacedRecommendationDto(sId, sName, cgpa, compName, jobTitle, ctc, diff));
        }
        return list;
    }

    /**
     * Interviewer workload distribution query
     */
    public List<GraphShowcaseDto.InterviewerWorkloadDto> getInterviewerWorkloads() {
        String cypher = """
            MATCH (i:Interviewer)
            OPTIONAL MATCH (iv:Interview)-[:CONDUCTED_BY]->(i)
            RETURN i.interviewerName AS interviewerName,
                   i.interviewRoundNo AS roundNo,
                   count(iv) AS totalScheduled,
                   count(CASE WHEN iv.result IN ['CLEARED', 'PASSED'] THEN 1 END) AS clearedCount,
                   count(CASE WHEN iv.result IN ['REJECTED', 'FAILED'] THEN 1 END) AS rejectedCount
            ORDER BY roundNo ASC, totalScheduled DESC
        """;

        Collection<Map<String, Object>> rows = neo4jClient.query(cypher).fetch().all();
        List<GraphShowcaseDto.InterviewerWorkloadDto> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String name = (String) r.get("interviewerName");
            Integer rNo = parseInteger(r.get("roundNo"), 1);
            long scheduled = parseLong(r.get("totalScheduled"));
            long cleared = parseLong(r.get("clearedCount"));
            long rejected = parseLong(r.get("rejectedCount"));
            list.add(new GraphShowcaseDto.InterviewerWorkloadDto(name, rNo, scheduled, cleared, rejected));
        }
        return list;
    }

    private BigDecimal parseBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(val.toString().trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private Integer parseInteger(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(val.toString().trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private double parseDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(val.toString().trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private long parseLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(val.toString().trim());
        } catch (Exception e) {
            return 0L;
        }
    }
}
