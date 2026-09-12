package com.placement.portal.reports;

import com.placement.portal.application.Application;
import com.placement.portal.application.ApplicationRepository;
import com.placement.portal.company.CompanyRepository;
import com.placement.portal.interview.InterviewRepository;
import com.placement.portal.offer.OfferLetter;
import com.placement.portal.offer.OfferRepository;
import com.placement.portal.program.Program;
import com.placement.portal.program.ProgramRegistration;
import com.placement.portal.program.ProgramRegistrationRepository;
import com.placement.portal.program.ProgramRepository;
import com.placement.portal.recruitment.PlacementDriveRepository;
import com.placement.portal.student.StudentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final ReportJdbcDao reportJdbcDao;
    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final PlacementDriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;
    private final OfferRepository offerRepository;
    private final InterviewRepository interviewRepository;
    private final ProgramRepository programRepository;
    private final ProgramRegistrationRepository registrationRepository;

    public ReportService(
            ReportJdbcDao reportJdbcDao,
            StudentRepository studentRepository,
            CompanyRepository companyRepository,
            PlacementDriveRepository driveRepository,
            ApplicationRepository applicationRepository,
            OfferRepository offerRepository,
            InterviewRepository interviewRepository,
            ProgramRepository programRepository,
            ProgramRegistrationRepository registrationRepository
    ) {
        this.reportJdbcDao = reportJdbcDao;
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.driveRepository = driveRepository;
        this.applicationRepository = applicationRepository;
        this.offerRepository = offerRepository;
        this.interviewRepository = interviewRepository;
        this.programRepository = programRepository;
        this.registrationRepository = registrationRepository;
    }

    public PlacementStatsDto getPlacementSummary() {
        long totalStudents = studentRepository.count();
        long eligibleStudents = applicationRepository.findAll().stream()
                .map(Application::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .filter(studentRepository::existsById)
                .count();
        long totalCompanies = companyRepository.count();
        long activeDrives = driveRepository.count();
        long totalApps = applicationRepository.count();
        long totalOffers = offerRepository.count();

        Double rate = reportJdbcDao.getPlacementRate(null);
        Double avgPkg = reportJdbcDao.getAveragePackage(null);

        double maxPkg = offerRepository.findAll().stream()
                .map(OfferLetter::getCtcLpa)
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

    public OverviewReportDto getOverviewReport() {
        long totalStudents = studentRepository.count();
        long eligibleStudents = applicationRepository.findAll().stream()
                .map(Application::getStudentId)
                .filter(Objects::nonNull)
                .distinct()
                .filter(studentRepository::existsById)
                .count();
        long totalCompanies = companyRepository.count();
        long activeDrives = driveRepository.count();
        long totalApps = applicationRepository.count();
        long totalOffers = offerRepository.count();
        long totalInterviews = interviewRepository.count();

        // Calculate rate via Oracle PL/SQL Function
        Double rate = reportJdbcDao.getPlacementRate(null);
        Double avgPkg = reportJdbcDao.getAveragePackage(null);

        List<OfferLetter> allOffers = offerRepository.findAll();
        double maxPkg = allOffers.stream()
                .map(OfferLetter::getCtcLpa)
                .mapToDouble(BigDecimal::doubleValue)
                .max().orElse(0.0);

        // Calculate unique students placed
        Set<String> placedStudentIds = new HashSet<>();
        for (OfferLetter o : allOffers) {
            applicationRepository.findById(o.getApplicationId()).ifPresent(a -> placedStudentIds.add(a.getStudentId()));
        }
        long uniqueStudentsPlaced = placedStudentIds.size();

        // Application status breakdown
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("APPLIED", 0L);
        statusCounts.put("SHORTLISTED", 0L);
        statusCounts.put("INTERVIEWING", 0L);
        statusCounts.put("SELECTED", 0L);
        statusCounts.put("OFFERED", 0L);
        statusCounts.put("REJECTED", 0L);

        for (Application app : applicationRepository.findAll()) {
            String st = app.getStatus() != null ? app.getStatus().toUpperCase() : "APPLIED";
            statusCounts.put(st, statusCounts.getOrDefault(st, 0L) + 1);
        }

        // CTC distribution
        long superDream = allOffers.stream().filter(o -> o.getCtcLpa().doubleValue() >= 20.0).count();
        long dream = allOffers.stream().filter(o -> o.getCtcLpa().doubleValue() >= 10.0 && o.getCtcLpa().doubleValue() < 20.0).count();
        long regular = allOffers.stream().filter(o -> o.getCtcLpa().doubleValue() < 10.0).count();
        long offerTotal = Math.max(totalOffers, 1);

        List<OverviewReportDto.CtcTierStat> ctcDistribution = List.of(
                new OverviewReportDto.CtcTierStat("Super Dream (≥ 20 LPA)", superDream, Math.round((double) superDream / offerTotal * 100.0)),
                new OverviewReportDto.CtcTierStat("Dream (10 - 20 LPA)", dream, Math.round((double) dream / offerTotal * 100.0)),
                new OverviewReportDto.CtcTierStat("Regular (< 10 LPA)", regular, Math.round((double) regular / offerTotal * 100.0))
        );

        // Company hiring stats
        List<CompanyHiringStatDto> companyStats = getCompanyHiringStats();

        // Program-wise placement stats (only include active programs with enrolled students)
        List<OverviewReportDto.ProgramPlacementStat> programStats = new ArrayList<>();
        for (Program prog : programRepository.findAll()) {
            long enrolled = registrationRepository.findAll().stream()
                    .filter(r -> r.getProgramId().equals(prog.getProgramId()))
                    .count();
            if (enrolled == 0) continue;
            // Count how many enrolled students have an offer
            long placed = registrationRepository.findAll().stream()
                    .filter(r -> r.getProgramId().equals(prog.getProgramId()))
                    .filter(r -> placedStudentIds.contains(r.getStudentId()))
                    .count();
            double pRate = enrolled > 0 ? Math.round(((double) placed / enrolled * 100.0) * 10.0) / 10.0 : 0.0;
            programStats.add(new OverviewReportDto.ProgramPlacementStat(prog.getProgramName(), enrolled, placed, pRate));
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

    public List<CompanyHiringStatDto> getCompanyHiringStats() {
        List<Map<String, Object>> rows = reportJdbcDao.getCompanyHiringStats();
        List<CompanyHiringStatDto> list = new ArrayList<>();

        for (Map<String, Object> r : rows) {
            String name = (String) r.get("COMPANY_NAME");
            String industry = (String) r.get("INDUSTRY");
            long offers = ((Number) r.get("TOTAL_OFFERS")).longValue();
            double avg = ((Number) r.get("AVERAGE_CTC")).doubleValue();
            double max = ((Number) r.get("HIGHEST_CTC")).doubleValue();
            double min = ((Number) r.get("LOWEST_CTC")).doubleValue();

            list.add(new CompanyHiringStatDto(name, industry, offers, avg, max, min));
        }

        return list;
    }
}
