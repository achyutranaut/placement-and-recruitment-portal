package com.placement.portal.neo4j.service;

import com.placement.portal.neo4j.model.OfferNode;
import com.placement.portal.neo4j.model.ProgramNode;
import com.placement.portal.neo4j.repository.*;
import com.placement.portal.reports.CompanyHiringStatDto;
import com.placement.portal.reports.IReportService;
import com.placement.portal.reports.OverviewReportDto;
import com.placement.portal.reports.PlacementStatsDto;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("neo4j")
@Primary
public class Neo4jReportService implements IReportService {

    private final Neo4jClient neo4jClient;
    private final Neo4jStudentRepository studentRepository;
    private final Neo4jCompanyRepository companyRepository;
    private final Neo4jDriveRepository driveRepository;
    private final Neo4jApplicationRepository applicationRepository;
    private final Neo4jOfferRepository offerRepository;
    private final Neo4jInterviewRepository interviewRepository;
    private final Neo4jProgramRepository programRepository;

    public Neo4jReportService(
            Neo4jClient neo4jClient,
            Neo4jStudentRepository studentRepository,
            Neo4jCompanyRepository companyRepository,
            Neo4jDriveRepository driveRepository,
            Neo4jApplicationRepository applicationRepository,
            Neo4jOfferRepository offerRepository,
            Neo4jInterviewRepository interviewRepository,
            Neo4jProgramRepository programRepository
    ) {
        this.neo4jClient = neo4jClient;
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.driveRepository = driveRepository;
        this.applicationRepository = applicationRepository;
        this.offerRepository = offerRepository;
        this.interviewRepository = interviewRepository;
        this.programRepository = programRepository;
    }

    /**
     * Ported PL/SQL Function: GET_PLACEMENT_RATE as Cypher aggregation
     */
    public Double getPlacementRate(String programId) {
        String cypher;
        Map<String, Object> params = new HashMap<>();

        if (programId != null && !programId.isBlank()) {
            cypher = """
                MATCH (s:Student {programId: $programId})
                WITH count(s) AS totalStudents
                OPTIONAL MATCH (s2:Student {programId: $programId})-[:SUBMITTED_APPLICATION]->(a:Application)-[:RECEIVED_OFFER]->(o:Offer)
                WITH totalStudents, count(DISTINCT s2) AS placedCount
                RETURN CASE WHEN totalStudents > 0 THEN round((toFloat(placedCount) / totalStudents) * 10000.0) / 100.0 ELSE 0.0 END AS rate
            """;
            params.put("programId", programId);
        } else {
            cypher = """
                MATCH (s:Student)
                WITH count(s) AS totalStudents
                OPTIONAL MATCH (s2:Student)-[:SUBMITTED_APPLICATION]->(a:Application)-[:RECEIVED_OFFER]->(o:Offer)
                WITH totalStudents, count(DISTINCT s2) AS placedCount
                RETURN CASE WHEN totalStudents > 0 THEN round((toFloat(placedCount) / totalStudents) * 10000.0) / 100.0 ELSE 0.0 END AS rate
            """;
        }

        return neo4jClient.query(cypher)
                .bindAll(params)
                .fetchAs(Double.class)
                .one()
                .orElse(0.0);
    }

    /**
     * Ported PL/SQL Function: GET_AVERAGE_PACKAGE as Cypher aggregation
     */
    public Double getAveragePackage(String industry) {
        String cypher;
        Map<String, Object> params = new HashMap<>();

        if (industry != null && !industry.isBlank()) {
            cypher = """
                MATCH (c:Company {industry: $industry})-[:HOSTS_DRIVE]->(d:Drive)<-[:FOR_DRIVE]-(a:Application)-[:RECEIVED_OFFER]->(o:Offer)
                RETURN coalesce(round(avg(toFloat(o.ctcLpa)) * 100.0) / 100.0, 0.0) AS avgCtc
            """;
            params.put("industry", industry);
        } else {
            cypher = """
                MATCH (o:Offer)
                RETURN coalesce(round(avg(toFloat(o.ctcLpa)) * 100.0) / 100.0, 0.0) AS avgCtc
            """;
        }

        return neo4jClient.query(cypher)
                .bindAll(params)
                .fetchAs(Double.class)
                .one()
                .orElse(0.0);
    }

    @Override
    public PlacementStatsDto getPlacementSummary() {
        long totalStudents = studentRepository.count();
        long eligibleStudents = applicationRepository.findAll().stream()
                .map(a -> a.getStudentId())
                .filter(Objects::nonNull)
                .distinct()
                .count();
        long totalCompanies = companyRepository.count();
        long activeDrives = driveRepository.count();
        long totalApps = applicationRepository.count();
        long totalOffers = offerRepository.count();

        Double rate = getPlacementRate(null);
        Double avgPkg = getAveragePackage(null);

        double maxPkg = offerRepository.findAll().stream()
                .map(OfferNode::getCtcLpa)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .max().orElse(0.0);

        List<CompanyHiringStatDto> companyStats = getCompanyHiringStats();

        return new PlacementStatsDto(
                totalStudents,
                eligibleStudents,
                totalCompanies,
                activeDrives,
                totalApps,
                totalOffers,
                rate != null ? rate : 0.0,
                avgPkg != null ? avgPkg : 0.0,
                maxPkg,
                companyStats
        );
    }

    @Override
    public OverviewReportDto getOverviewReport() {
        long totalStudents = studentRepository.count();
        long eligibleStudents = applicationRepository.findAll().stream()
                .map(a -> a.getStudentId())
                .filter(Objects::nonNull)
                .distinct()
                .count();
        long totalCompanies = companyRepository.count();
        long activeDrives = driveRepository.count();
        long totalApps = applicationRepository.count();
        long totalOffers = offerRepository.count();
        long totalInterviews = interviewRepository.count();

        Double rate = getPlacementRate(null);
        Double avgPkg = getAveragePackage(null);

        List<OfferNode> allOffers = offerRepository.findAll();
        double maxPkg = allOffers.stream()
                .map(OfferNode::getCtcLpa)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .max().orElse(0.0);

        // Unique students placed via Graph traversal
        String uniquePlacedCypher = """
            MATCH (s:Student)-[:SUBMITTED_APPLICATION]->(a:Application)-[:RECEIVED_OFFER]->(o:Offer)
            RETURN count(DISTINCT s) AS placedCount
        """;
        long uniqueStudentsPlaced = neo4jClient.query(uniquePlacedCypher)
                .fetchAs(Long.class)
                .one()
                .orElse(0L);

        // Application status breakdown via Cypher aggregation
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("APPLIED", 0L);
        statusCounts.put("SHORTLISTED", 0L);
        statusCounts.put("INTERVIEWING", 0L);
        statusCounts.put("SELECTED", 0L);
        statusCounts.put("OFFERED", 0L);
        statusCounts.put("ACCEPTED", 0L);
        statusCounts.put("DECLINED", 0L);
        statusCounts.put("REJECTED", 0L);

        String statusCypher = "MATCH (a:Application) RETURN toUpper(trim(a.status)) AS st, count(a) AS cnt";
        Collection<Map<String, Object>> rows = neo4jClient.query(statusCypher).fetch().all();
        for (Map<String, Object> r : rows) {
            String st = (String) r.get("st");
            Number cnt = (Number) r.get("cnt");
            if (st != null && cnt != null) {
                statusCounts.put(st, cnt.longValue());
            }
        }
        totalApps = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        // CTC distribution tiers
        long superDream = allOffers.stream().filter(o -> o.getCtcLpa() != null && o.getCtcLpa().doubleValue() >= 20.0).count();
        long dream = allOffers.stream().filter(o -> o.getCtcLpa() != null && o.getCtcLpa().doubleValue() >= 10.0 && o.getCtcLpa().doubleValue() < 20.0).count();
        long regular = allOffers.stream().filter(o -> o.getCtcLpa() != null && o.getCtcLpa().doubleValue() < 10.0).count();
        long offerTotal = Math.max(totalOffers, 1);

        List<OverviewReportDto.CtcTierStat> ctcDistribution = List.of(
                new OverviewReportDto.CtcTierStat("Super Dream (≥ 20 LPA)", superDream, Math.round((double) superDream / offerTotal * 100.0)),
                new OverviewReportDto.CtcTierStat("Dream (10 - 20 LPA)", dream, Math.round((double) dream / offerTotal * 100.0)),
                new OverviewReportDto.CtcTierStat("Regular (< 10 LPA)", regular, Math.round((double) regular / offerTotal * 100.0))
        );

        List<CompanyHiringStatDto> companyStats = getCompanyHiringStats();

        // Program-wise placement stats via graph traversal
        List<OverviewReportDto.ProgramPlacementStat> programStats = new ArrayList<>();
        String progStatsCypher = """
            MATCH (p:Program)<-[:ENROLLED_IN]-(s:Student)
            WITH p, count(DISTINCT s) AS enrolled
            WHERE enrolled > 0
            OPTIONAL MATCH (p)<-[:ENROLLED_IN]-(placed:Student)-[:SUBMITTED_APPLICATION]->(:Application)-[:RECEIVED_OFFER]->(:Offer)
            WITH p, enrolled, count(DISTINCT placed) AS placed
            RETURN p.programName AS programName, enrolled, placed
            ORDER BY programName ASC
        """;
        Collection<Map<String, Object>> pRows = neo4jClient.query(progStatsCypher).fetch().all();
        for (Map<String, Object> pr : pRows) {
            String pName = (String) pr.get("programName");
            long enrolled = ((Number) pr.get("enrolled")).longValue();
            long placed = ((Number) pr.get("placed")).longValue();
            double pRate = enrolled > 0 ? Math.round(((double) placed / enrolled * 100.0) * 10.0) / 10.0 : 0.0;
            programStats.add(new OverviewReportDto.ProgramPlacementStat(pName, enrolled, placed, pRate));
        }

        return new OverviewReportDto(
                rate != null ? rate : 0.0,
                avgPkg != null ? avgPkg : 0.0,
                maxPkg,
                totalOffers,
                uniqueStudentsPlaced,
                totalStudents,
                eligibleStudents,
                totalCompanies,
                activeDrives,
                totalApps,
                totalInterviews,
                statusCounts,
                ctcDistribution,
                companyStats,
                programStats
        );
    }

    /**
     * Ported View / Analytical aggregation: Company hiring stats using Cypher graph traversal
     */
    @Override
    public List<CompanyHiringStatDto> getCompanyHiringStats() {
        String cypher = """
            MATCH (c:Company)-[:HOSTS_DRIVE]->(d:Drive)<-[:FOR_DRIVE]-(a:Application)-[:RECEIVED_OFFER]->(o:Offer)
            RETURN c.companyName AS companyName,
                   coalesce(c.industry, 'Technology') AS industry,
                   count(o) AS totalOffers,
                   round(avg(toFloat(o.ctcLpa)) * 100.0) / 100.0 AS averageCtc,
                   max(toFloat(o.ctcLpa)) AS highestCtc,
                   min(toFloat(o.ctcLpa)) AS lowestCtc
            ORDER BY averageCtc DESC
        """;

        Collection<Map<String, Object>> rows = neo4jClient.query(cypher).fetch().all();
        List<CompanyHiringStatDto> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String name = (String) r.get("companyName");
            String industry = (String) r.get("industry");
            long offers = ((Number) r.get("totalOffers")).longValue();
            double avg = ((Number) r.get("averageCtc")).doubleValue();
            double max = ((Number) r.get("highestCtc")).doubleValue();
            double min = ((Number) r.get("lowestCtc")).doubleValue();
            list.add(new CompanyHiringStatDto(name, industry, offers, avg, max, min));
        }
        return list;
    }
}
