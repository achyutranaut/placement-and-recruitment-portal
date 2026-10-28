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

import com.placement.portal.company.EmailCompany;
import com.placement.portal.company.EmailCompanyRepository;
import com.placement.portal.company.JobCompany;
import com.placement.portal.company.JobCompanyRepository;
import com.placement.portal.recruitment.PlacementDrive;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

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
    private final JobCompanyRepository jobCompanyRepository;
    private final EmailCompanyRepository emailCompanyRepository;

    public ReportService(
            ReportJdbcDao reportJdbcDao,
            StudentRepository studentRepository,
            CompanyRepository companyRepository,
            PlacementDriveRepository driveRepository,
            ApplicationRepository applicationRepository,
            OfferRepository offerRepository,
            InterviewRepository interviewRepository,
            ProgramRepository programRepository,
            ProgramRegistrationRepository registrationRepository,
            JobCompanyRepository jobCompanyRepository,
            EmailCompanyRepository emailCompanyRepository
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
        this.jobCompanyRepository = jobCompanyRepository;
        this.emailCompanyRepository = emailCompanyRepository;
    }

    public PlacementStatsDto getPlacementSummary() {
        long totalStudents = studentRepository.count();
        long eligibleStudents = calculateEligibleStudents();
        long totalCompanies = companyRepository.count();
        long activeDrives = driveRepository.count();
        long totalApps = applicationRepository.count();
        long totalOffers = offerRepository.count();

        Double rate = reportJdbcDao.getPlacementRate(null);
        Double avgPkg = reportJdbcDao.getAveragePackage(null);

        double maxPkg = offerRepository.findAll().stream()
                .filter(o -> o.getStatus() == null || !"DECLINED".equalsIgnoreCase(o.getStatus()))
                .map(OfferLetter::getCtcLpa)
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

    public OverviewReportDto getOverviewReport() {
        long totalStudents = studentRepository.count();
        long eligibleStudents = calculateEligibleStudents();
        long totalCompanies = companyRepository.count();
        long activeDrives = driveRepository.count();
        long totalApps = applicationRepository.count();
        long totalOffers = offerRepository.count();
        long totalInterviews = interviewRepository.count();

        // Calculate rate via Oracle PL/SQL Function
        Double rate = reportJdbcDao.getPlacementRate(null);
        Double avgPkg = reportJdbcDao.getAveragePackage(null);

        List<OfferLetter> allOffers = offerRepository.findAll();
        List<OfferLetter> validOffers = allOffers.stream()
                .filter(o -> o.getStatus() == null || !"DECLINED".equalsIgnoreCase(o.getStatus()))
                .toList();

        double maxPkg = validOffers.stream()
                .map(OfferLetter::getCtcLpa)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .max().orElse(0.0);

        // Calculate unique students placed (candidates with accepted or issued placement offers)
        Set<String> placedStudentIds = new HashSet<>();
        for (OfferLetter o : validOffers) {
            applicationRepository.findById(o.getApplicationId()).ifPresent(a -> placedStudentIds.add(a.getStudentId()));
        }
        long uniqueStudentsPlaced = placedStudentIds.size();

        // Application status breakdown aggregated directly from Oracle Database via SQL GROUP BY
        Map<String, Long> statusCounts = reportJdbcDao.getApplicationStatusCounts();
        totalApps = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        // CTC distribution
        long superDream = validOffers.stream().filter(o -> o.getCtcLpa() != null && o.getCtcLpa().doubleValue() >= 20.0).count();
        long dream = validOffers.stream().filter(o -> o.getCtcLpa() != null && o.getCtcLpa().doubleValue() >= 10.0 && o.getCtcLpa().doubleValue() < 20.0).count();
        long regular = validOffers.stream().filter(o -> o.getCtcLpa() != null && o.getCtcLpa().doubleValue() < 10.0).count();
        long offerTotal = Math.max(validOffers.size(), 1);

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

        // Monthly placement timeline derived from real OFFER_LETTER.Offer_Date
        List<OverviewReportDto.MonthlyPlacementStat> monthlyTimeline = new ArrayList<>();
        Map<YearMonth, List<OfferLetter>> offersByMonth = new TreeMap<>();
        for (OfferLetter o : allOffers) {
            LocalDate dt = o.getOfferDate() != null ? o.getOfferDate() : LocalDate.now();
            YearMonth ym = YearMonth.from(dt);
            offersByMonth.computeIfAbsent(ym, k -> new ArrayList<>()).add(o);
        }

        long cumulativeOffers = 0;
        Set<String> cumulativePlacedStudents = new HashSet<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

        for (Map.Entry<YearMonth, List<OfferLetter>> entry : offersByMonth.entrySet()) {
            YearMonth ym = entry.getKey();
            List<OfferLetter> monthOffers = entry.getValue();
            long count = monthOffers.size();
            cumulativeOffers += count;

            Set<String> monthPlacedStudents = new HashSet<>();
            for (OfferLetter o : monthOffers) {
                applicationRepository.findById(o.getApplicationId()).ifPresent(a -> {
                    monthPlacedStudents.add(a.getStudentId());
                    cumulativePlacedStudents.add(a.getStudentId());
                });
            }

            double monthAvgPkg = monthOffers.stream()
                    .map(OfferLetter::getCtcLpa)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average().orElse(0.0);

            monthlyTimeline.add(new OverviewReportDto.MonthlyPlacementStat(
                    ym.atDay(1).format(monthFormatter),
                    ym.toString(),
                    count,
                    cumulativeOffers,
                    monthPlacedStudents.size(),
                    cumulativePlacedStudents.size(),
                    Math.round(monthAvgPkg * 100.0) / 100.0
            ));
        }

        // Company Placement Performance comparison
        Map<String, String> jobToCompanyId = jobCompanyRepository.findAll().stream()
                .collect(Collectors.toMap(JobCompany::getJobTitle, JobCompany::getCompanyId, (a, b) -> a));
        Map<String, String> compIdToEmail = companyRepository.findAll().stream()
                .collect(Collectors.toMap(com.placement.portal.company.Company::getCompanyId, com.placement.portal.company.Company::getEmail, (a, b) -> a));
        Map<String, String> emailToName = emailCompanyRepository.findAll().stream()
                .collect(Collectors.toMap(EmailCompany::getEmail, EmailCompany::getCompanyName, (a, b) -> a));

        List<PlacementDrive> allDrivesList = driveRepository.findAll();
        List<Application> allAppsList = applicationRepository.findAll();

        Map<String, String> driveIdToCompanyId = new HashMap<>();
        for (PlacementDrive d : allDrivesList) {
            String cId = jobToCompanyId.get(d.getJobTitle());
            if (cId != null) {
                driveIdToCompanyId.put(d.getDriveId(), cId);
            }
        }

        Map<String, String> appIdToCompanyId = new HashMap<>();
        for (Application a : allAppsList) {
            String cId = driveIdToCompanyId.get(a.getDriveId());
            if (cId != null) {
                appIdToCompanyId.put(a.getApplicationId(), cId);
            }
        }

        List<OverviewReportDto.CompanyPlacementAnalyticsDto> companyAnalytics = new ArrayList<>();
        for (com.placement.portal.company.Company comp : companyRepository.findAll()) {
            String cId = comp.getCompanyId();
            String cName = emailToName.getOrDefault(comp.getEmail(), cId);
            String industry = comp.getIndustry() != null ? comp.getIndustry() : "Technology";

            long totalDrives = allDrivesList.stream().filter(d -> cId.equals(jobToCompanyId.get(d.getJobTitle()))).count();
            List<Application> compApps = allAppsList.stream().filter(a -> cId.equals(appIdToCompanyId.get(a.getApplicationId()))).toList();
            long appsCount = compApps.size();

            List<OfferLetter> compOffers = allOffers.stream().filter(o -> cId.equals(appIdToCompanyId.get(o.getApplicationId()))).toList();
            long offersCount = compOffers.size();

            List<OfferLetter> validCompOffers = compOffers.stream()
                    .filter(o -> o.getStatus() == null || !"DECLINED".equalsIgnoreCase(o.getStatus()))
                    .toList();

            Set<String> compPlacedStudents = new HashSet<>();
            for (OfferLetter o : validCompOffers) {
                applicationRepository.findById(o.getApplicationId()).ifPresent(a -> compPlacedStudents.add(a.getStudentId()));
            }
            long placedCount = compPlacedStudents.size();

            double convRate = appsCount > 0 ? Math.round(((double) placedCount / appsCount * 100.0) * 10.0) / 10.0 : 0.0;
            double avgCtc = validCompOffers.stream().map(OfferLetter::getCtcLpa).mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
            double maxCtc = validCompOffers.stream().map(OfferLetter::getCtcLpa).mapToDouble(BigDecimal::doubleValue).max().orElse(0.0);

            companyAnalytics.add(new OverviewReportDto.CompanyPlacementAnalyticsDto(
                    cName,
                    industry,
                    totalDrives,
                    appsCount,
                    placedCount,
                    offersCount,
                    convRate,
                    Math.round(avgCtc * 100.0) / 100.0,
                    Math.round(maxCtc * 100.0) / 100.0
            ));
        }
        companyAnalytics.sort((a, b) -> Long.compare(b.getOffersReleased(), a.getOffersReleased()));

        OverviewReportDto overviewReportDto = new OverviewReportDto(
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
        overviewReportDto.setMonthlyTimeline(monthlyTimeline);
        overviewReportDto.setCompanyAnalytics(companyAnalytics);
        return overviewReportDto;
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

    private long calculateEligibleStudents() {
        BigDecimal minDriveCgpa = driveRepository.findAll().stream()
                .map(PlacementDrive::getMinCgpa)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.valueOf(6.50));

        Set<String> applicantStudentIds = applicationRepository.findAll().stream()
                .map(Application::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return studentRepository.findAll().stream()
                .filter(s -> (s.getCgpa() != null && s.getCgpa().compareTo(minDriveCgpa) >= 0) || applicantStudentIds.contains(s.getStudentId()))
                .count();
    }
}
