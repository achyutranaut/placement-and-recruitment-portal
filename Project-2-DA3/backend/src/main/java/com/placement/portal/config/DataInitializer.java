package com.placement.portal.config;

import com.placement.portal.program.Program;
import com.placement.portal.program.ProgramRepository;
import com.placement.portal.student.Student;
import com.placement.portal.student.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Production DataInitializer:
 * Ensures only the canonical university degree programs exist as master reference data
 * in the Oracle PROGRAM table.
 * 
 * NO MOCK STUDENTS, COMPANIES, DRIVES, OFFERS, OR APPLICATIONS ARE SEEDED.
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ProgramRepository programRepository;
    private final StudentRepository studentRepository;

    public DataInitializer(
            ProgramRepository programRepository,
            StudentRepository studentRepository
    ) {
        this.programRepository = programRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Verifying university canonical academic degree programs in PROGRAM table...");
        seedCanonicalPrograms();
        ensureStudentProgramIds();
        log.info("Canonical master reference data verified. Database is ready for live operational data.");
    }

private void seedCanonicalPrograms() {
        // Ensure legacy references exist for foreign-key compatibility
        List<Program> legacyPrograms = List.of(
                new Program("BCE", "Computer Science & Engineering", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Legacy", "Computer Science", null, "^2[0-9]BCE[0-9]{4}$", "21BCE1234", "N"),
                new Program("BCT", "Information Technology", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Legacy", "Information Technology", null, "^2[0-9]BCT[0-9]{4}$", "21BCT1234", "N"),
                new Program("BEC", "Electronics & Communication", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Legacy", "Electronics", null, "^2[0-9]BEC[0-9]{4}$", "21BEC1234", "N"),
                new Program("BME", "Mechanical Engineering", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Legacy", "Mechanical", null, "^2[0-9]BME[0-9]{4}$", "21BME1234", "N"),
                new Program("BDS", "Data Science", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Legacy", "Computer Science", "Data Science", "^2[0-9]BCE[0-9]{4}$", "21BCE1234", "N"),
                new Program("BEE", "Electrical & Electronics Engineering", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Legacy", "Electrical", null, "^2[0-9]BEE[0-9]{4}$", "21BEE1234", "N"),
                new Program("BCV", "Civil Engineering", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Legacy", "Civil", null, "^2[0-9]BCV[0-9]{4}$", "21BCV1234", "N")
        );
        for (Program p : legacyPrograms) {
            if (!programRepository.existsById(p.getProgramId())) {
                programRepository.save(p);
            }
        }

        // Seed Complete Institutional Program Catalog (107 Programs)
        List<Program> catalog = getFullCatalog();
        for (Program p : catalog) {
            if (!programRepository.existsById(p.getProgramId())) {
                programRepository.save(p);
            }
        }
        log.info("Verified all {} institutional degree programs in PROGRAM table.", catalog.size());
    }

    private List<Program> getFullCatalog() {
        return List.of(
            new Program("BTECH-CSE", "B.Tech CSE", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Computer Science", null, "^2[0-9]BCE[0-9]{4}$", "21BCE1234", "Y"),
            new Program("BTECH-CSE-AIML", "B.Tech CSE (AI & ML)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Computer Science", "AI & ML", "^2[0-9]BCE[0-9]{4}$", "21BCE1234", "Y"),
            new Program("BTECH-CSE-AIDA", "B.Tech CSE (AI & Data Analytics)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Computer Science", "AI & Data Analytics", "^2[0-9]BCE[0-9]{4}$", "21BCE1234", "Y"),
            new Program("BTECH-CSE-BLOCK", "B.Tech CSE (Blockchain)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Computer Science", "Blockchain", "^2[0-9]BCE[0-9]{4}$", "21BCE1234", "Y"),
            new Program("BTECH-CSE-CYBER", "B.Tech CSE (Cyber Security)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Computer Science", "Cyber Security", "^2[0-9]BCE[0-9]{4}$", "21BCE1234", "Y"),
            new Program("BTECH-CSE-DS", "B.Tech CSE (Data Science)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Computer Science", "Data Science", "^2[0-9]BCE[0-9]{4}$", "21BCE1234", "Y"),
            new Program("BTECH-CSE-GAMING", "B.Tech CSE (Gaming Tech)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Computer Science", "Gaming Tech", "^2[0-9]BCE[0-9]{4}$", "21BCE1234", "Y"),
            new Program("BTECH-CSE-IOT", "B.Tech CSE (IoT)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Computer Science", "IoT", "^2[0-9]BCE[0-9]{4}$", "21BCE1234", "Y"),
            new Program("BTECH-CSE-SWE", "B.Tech CSE (Software Engineering)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Computer Science", "Software Engineering", "^2[0-9]BCE[0-9]{4}$", "21BCE1234", "Y"),
            new Program("BTECH-ECE", "B.Tech ECE", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Electronics", null, "^2[0-9]BEC[0-9]{4}$", "21BEC1234", "Y"),
            new Program("BTECH-ECE-AICYBER", "B.Tech ECE (AI & Cybernetics)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Electronics", "AI & Cybernetics", "^2[0-9]BEC[0-9]{4}$", "21BEC1234", "Y"),
            new Program("BTECH-ECE-EMBED", "B.Tech ECE (Embedded Systems)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Electronics", "Embedded Systems", "^2[0-9]BEC[0-9]{4}$", "21BEC1234", "Y"),
            new Program("BTECH-ECE-VLSI", "B.Tech ECE (VLSI)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Electronics", "VLSI", "^2[0-9]BEC[0-9]{4}$", "21BEC1234", "Y"),
            new Program("BTECH-ECE-BIOMED", "B.Tech ECE (Biomedical)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Electronics", "Biomedical", "^2[0-9]BEC[0-9]{4}$", "21BEC1234", "Y"),
            new Program("BTECH-EEE", "B.Tech EEE", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Electrical", null, "^2[0-9]BEE[0-9]{4}$", "21BEE1234", "Y"),
            new Program("BTECH-ELE-COMP", "B.Tech Electronics & Computer Engineering", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Electronics", "Computer Engineering", "^2[0-9]BEC[0-9]{4}$", "21BEC1234", "Y"),
            new Program("BTECH-ELE-INST", "B.Tech Electronics & Instrumentation", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Electronics", "Instrumentation", "^2[0-9]BEI[0-9]{4}$", "21BEI1234", "Y"),
            new Program("BTECH-MECH", "B.Tech Mechanical", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Mechanical", null, "^2[0-9]BME[0-9]{4}$", "21BME1234", "Y"),
            new Program("BTECH-MECH-AUTO", "B.Tech Mechanical (Automotive)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Mechanical", "Automotive", "^2[0-9]BME[0-9]{4}$", "21BME1234", "Y"),
            new Program("BTECH-MECH-EV", "B.Tech Mechanical (EV)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Mechanical", "EV", "^2[0-9]BME[0-9]{4}$", "21BME1234", "Y"),
            new Program("BTECH-MECH-MFG", "B.Tech Mechanical (Manufacturing)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Mechanical", "Manufacturing", "^2[0-9]BME[0-9]{4}$", "21BME1234", "Y"),
            new Program("BTECH-MECH-ROBOT", "B.Tech Mechanical (Robotics)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Mechanical", "Robotics", "^2[0-9]BME[0-9]{4}$", "21BME1234", "Y"),
            new Program("BTECH-CIVIL", "B.Tech Civil", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Civil", null, "^2[0-9]BCV[0-9]{4}$", "21BCV1234", "Y"),
            new Program("BTECH-CIVIL-LT", "B.Tech Civil (L&T Collaboration)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Civil", "L&T Collaboration", "^2[0-9]BCV[0-9]{4}$", "21BCV1234", "Y"),
            new Program("BTECH-CHEM", "B.Tech Chemical", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Chemical", null, "^2[0-9]BCH[0-9]{4}$", "21BCH1234", "Y"),
            new Program("BTECH-BIOTECH", "B.Tech Biotechnology", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Biotechnology", null, "^2[0-9]BBT[0-9]{4}$", "21BBT1234", "Y"),
            new Program("BTECH-AERO", "B.Tech Aerospace", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Aerospace", null, "^2[0-9]BAE[0-9]{4}$", "21BAE1234", "Y"),
            new Program("BTECH-BIOENG", "B.Tech Bioengineering", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Bioengineering", null, "^2[0-9]BBI[0-9]{4}$", "21BBI1234", "Y"),
            new Program("BTECH-IT", "B.Tech IT", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Information Technology", null, "^2[0-9]BCT[0-9]{4}$", "21BCT1234", "Y"),
            new Program("BTECH-FASHION", "B.Tech Fashion Technology", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Design", "Fashion Technology", "^2[0-9]BFT[0-9]{4}$", "21BFT1234", "Y"),
            new Program("BARCH", "B.Arch", BigDecimal.ZERO, "5 Years", "Y", "N", "N", "Bachelor's", "Architecture", null, "^2[0-9]BAR[0-9]{4}$", "21BAR1001", "Y"),
            new Program("BDES-PROD", "B.Des Industrial/Product Design", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Design", "Industrial/Product Design", "^2[0-9]BDS[0-9]{4}$", "21BDS1001", "Y"),
            new Program("BSC-CS", "B.Sc Computer Science", BigDecimal.ZERO, "3 Years", "Y", "N", "N", "Bachelor's", "Computer Science", null, "^2[0-9]BSC[0-9]{4}$", "21BSC1001", "Y"),
            new Program("BSC-ECON", "B.Sc Economics (Hons.)", BigDecimal.ZERO, "3 Years", "Y", "N", "N", "Bachelor's", "Science", "Economics", "^2[0-9]BSC[0-9]{4}$", "21BSC1001", "Y"),
            new Program("BSC-FASHION", "B.Sc Fashion Design", BigDecimal.ZERO, "3 Years", "Y", "N", "N", "Bachelor's", "Design", "Fashion Design", "^2[0-9]BSC[0-9]{4}$", "21BSC1001", "Y"),
            new Program("BSC-HOSP", "B.Sc Hospitality & Hotel Administration", BigDecimal.ZERO, "3 Years", "Y", "N", "N", "Bachelor's", "Management", "Hospitality & Hotel Administration", "^2[0-9]BSC[0-9]{4}$", "21BSC1001", "Y"),
            new Program("BSC-ANIM", "B.Sc Multimedia & Animation", BigDecimal.ZERO, "3 Years", "Y", "N", "N", "Bachelor's", "Design", "Multimedia & Animation", "^2[0-9]BSC[0-9]{4}$", "21BSC1001", "Y"),
            new Program("BSC-VISCOM", "B.Sc Visual Communication", BigDecimal.ZERO, "3 Years", "Y", "N", "N", "Bachelor's", "Design", "Visual Communication", "^2[0-9]BSC[0-9]{4}$", "21BSC1001", "Y"),
            new Program("BSC-AGRI", "B.Sc Agriculture (Hons.)", BigDecimal.ZERO, "4 Years", "Y", "N", "N", "Bachelor's", "Science", "Agriculture", "^2[0-9]BAG[0-9]{4}$", "21BAG1001", "Y"),
            new Program("BBA", "BBA", BigDecimal.ZERO, "3 Years", "Y", "N", "N", "Bachelor's", "Management", null, "^2[0-9]BBA[0-9]{4}$", "21BBA1001", "Y"),
            new Program("BBA-HONS", "BBA (Hons.)", BigDecimal.ZERO, "3 Years", "Y", "N", "N", "Bachelor's", "Management", "Honours", "^2[0-9]BBA[0-9]{4}$", "21BBA1001", "Y"),
            new Program("BBA-DIGITAL", "BBA Digital Business", BigDecimal.ZERO, "3 Years", "Y", "N", "N", "Bachelor's", "Management", "Digital Business", "^2[0-9]BBA[0-9]{4}$", "21BBA1001", "Y"),
            new Program("BCOM", "B.Com", BigDecimal.ZERO, "3 Years", "Y", "N", "N", "Bachelor's", "Commerce", null, "^2[0-9]BCM[0-9]{4}$", "21BCM1001", "Y"),
            new Program("BCOM-HONS", "B.Com (Hons.)", BigDecimal.ZERO, "3 Years", "Y", "N", "N", "Bachelor's", "Commerce", "Honours", "^2[0-9]BCM[0-9]{4}$", "21BCM1001", "Y"),
            new Program("BCA", "BCA", BigDecimal.ZERO, "3 Years", "Y", "N", "N", "Bachelor's", "Computer Science", null, "^2[0-9]BCA[0-9]{4}$", "21BCA1001", "Y"),
            new Program("BALLB-HONS", "BA LLB (Hons.)", BigDecimal.ZERO, "5 Years", "Y", "N", "N", "Bachelor's", "Law", "Honours", "^2[0-9]BLW[0-9]{4}$", "21BLW1001", "Y"),
            new Program("BBALLB-HONS", "BBA LLB (Hons.)", BigDecimal.ZERO, "5 Years", "Y", "N", "N", "Bachelor's", "Law", "Honours", "^2[0-9]BLW[0-9]{4}$", "21BLW1001", "Y"),
            new Program("MTECH-CSE", "M.Tech CSE", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Computer Science", null, "^2[0-9]MCS[0-9]{4}$", "24MCS1001", "Y"),
            new Program("MTECH-CSE-BIGDATA", "M.Tech CSE (Big Data)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Computer Science", "Big Data", "^2[0-9]MCS[0-9]{4}$", "24MCS1001", "Y"),
            new Program("MTECH-CSE-CYBER", "M.Tech CSE (Cybersecurity)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Computer Science", "Cybersecurity", "^2[0-9]MCS[0-9]{4}$", "24MCS1001", "Y"),
            new Program("MTECH-CSE-INFOSEC", "M.Tech CSE (Information Security)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Computer Science", "Information Security", "^2[0-9]MCS[0-9]{4}$", "24MCS1001", "Y"),
            new Program("MTECH-ECE-COMM", "M.Tech ECE (Communication)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Electronics", "Communication", "^2[0-9]MEC[0-9]{4}$", "24MEC1001", "Y"),
            new Program("MTECH-ECE-VLSI", "M.Tech ECE (VLSI Design)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Electronics", "VLSI Design", "^2[0-9]MEC[0-9]{4}$", "24MEC1001", "Y"),
            new Program("MTECH-EEE-PED", "M.Tech EEE (Power Electronics & Drives)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Electrical", "Power Electronics & Drives", "^2[0-9]MEE[0-9]{4}$", "24MEE1001", "Y"),
            new Program("MTECH-EEE-CA", "M.Tech EEE (Control & Automation)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Electrical", "Control & Automation", "^2[0-9]MEE[0-9]{4}$", "24MEE1001", "Y"),
            new Program("MTECH-MECH-CADCAM", "M.Tech Mechanical (CAD/CAM)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Mechanical", "CAD/CAM", "^2[0-9]MME[0-9]{4}$", "24MME1001", "Y"),
            new Program("MTECH-MECH-MFG", "M.Tech Mechanical (Manufacturing)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Mechanical", "Manufacturing", "^2[0-9]MME[0-9]{4}$", "24MME1001", "Y"),
            new Program("MTECH-MECH-MECH", "M.Tech Mechanical (Mechatronics)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Mechanical", "Mechatronics", "^2[0-9]MME[0-9]{4}$", "24MME1001", "Y"),
            new Program("MTECH-MECH-AUTO", "M.Tech Mechanical (Automotive Engineering)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Mechanical", "Automotive Engineering", "^2[0-9]MME[0-9]{4}$", "24MME1001", "Y"),
            new Program("MTECH-MECH-AUTOE", "M.Tech Mechanical (Automotive Electronics)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Mechanical", "Automotive Electronics", "^2[0-9]MME[0-9]{4}$", "24MME1001", "Y"),
            new Program("MTECH-CIVIL-CTM", "M.Tech Civil (Construction Technology & Management)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Civil", "Construction Technology & Management", "^2[0-9]MCV[0-9]{4}$", "24MCV1001", "Y"),
            new Program("MTECH-CIVIL-STRUCT", "M.Tech Civil (Structural)", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Civil", "Structural", "^2[0-9]MCV[0-9]{4}$", "24MCV1001", "Y"),
            new Program("MTECH-BIOMED", "M.Tech Biomedical", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Bioengineering", "Biomedical", "^2[0-9]MBM[0-9]{4}$", "24MBM1001", "Y"),
            new Program("MTECH-BIOTECH", "M.Tech Biotechnology", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Biotechnology", null, "^2[0-9]MBT[0-9]{4}$", "24MBT1001", "Y"),
            new Program("MTECH-CLOUD", "M.Tech Cloud Computing", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Computer Science", "Cloud Computing", "^2[0-9]MCS[0-9]{4}$", "24MCS1001", "Y"),
            new Program("MTECH-SWE", "M.Tech Software Engineering", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Computer Science", "Software Engineering", "^2[0-9]MCS[0-9]{4}$", "24MCS1001", "Y"),
            new Program("MTECH-EMBED", "M.Tech Embedded Systems", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Electronics", "Embedded Systems", "^2[0-9]MEC[0-9]{4}$", "24MEC1001", "Y"),
            new Program("MTECH-NANO", "M.Tech Nanotechnology", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Science", "Nanotechnology", "^2[0-9]MNT[0-9]{4}$", "24MNT1001", "Y"),
            new Program("MTECH-ENV", "M.Tech Energy & Environmental Engineering", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Civil", "Energy & Environmental Engineering", "^2[0-9]MEE[0-9]{4}$", "24MEE1001", "Y"),
            new Program("MTECH-IND-AUTO", "M.Tech Industrial Automation", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Mechanical", "Industrial Automation", "^2[0-9]MME[0-9]{4}$", "24MME1001", "Y"),
            new Program("MTECH-IND-IOT", "M.Tech Industrial IoT", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Computer Science", "Industrial IoT", "^2[0-9]MCS[0-9]{4}$", "24MCS1001", "Y"),
            new Program("MTECH-IOT-SENSOR", "M.Tech IoT & Sensor Systems", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Electronics", "IoT & Sensor Systems", "^2[0-9]MEC[0-9]{4}$", "24MEC1001", "Y"),
            new Program("MTECH-APPLIED-CFD", "M.Tech Applied CFD", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Mechanical", "Applied CFD", "^2[0-9]MME[0-9]{4}$", "24MME1001", "Y"),
            new Program("MTECH-SMART-MOB", "M.Tech Smart Mobility", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Mechanical", "Smart Mobility", "^2[0-9]MME[0-9]{4}$", "24MME1001", "Y"),
            new Program("MBA", "MBA", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Management", null, "^2[0-9]MBA[0-9]{4}$", "24MBA1001", "Y"),
            new Program("MCA", "MCA", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Computer Science", null, "^2[0-9]MCA[0-9]{4}$", "24MCA1001", "Y"),
            new Program("MSC-BIOTECH", "M.Sc Biotechnology", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Biotechnology", null, "^2[0-9]MSB[0-9]{4}$", "24MSB1001", "Y"),
            new Program("MSC-DATA-SCI", "M.Sc Data Science", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Computer Science", "Data Science", "^2[0-9]MSD[0-9]{4}$", "24MSD1001", "Y"),
            new Program("MSC-CHEM", "M.Sc Chemistry", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Science", "Chemistry", "^2[0-9]MSC[0-9]{4}$", "24MSC1001", "Y"),
            new Program("MSC-PHYS", "M.Sc Physics", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Science", "Physics", "^2[0-9]MSP[0-9]{4}$", "24MSP1001", "Y"),
            new Program("MSC-APPL-MICRO", "M.Sc Applied Microbiology", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Science", "Applied Microbiology", "^2[0-9]MSM[0-9]{4}$", "24MSM1001", "Y"),
            new Program("MSC-BUS-STAT", "M.Sc Business Statistics", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Management", "Business Statistics", "^2[0-9]MSB[0-9]{4}$", "24MSB1001", "Y"),
            new Program("MSC-APPL-PSYCH", "M.Sc Applied Psychology", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Science", "Applied Psychology", "^2[0-9]MSY[0-9]{4}$", "24MSY1001", "Y"),
            new Program("MDES-IND-DES", "M.Des Industrial Design", BigDecimal.ZERO, "2 Years", "N", "Y", "N", "Master's", "Design", "Industrial Design", "^2[0-9]MDS[0-9]{4}$", "24MDS1001", "Y"),
            new Program("LLM-CORP-LAW", "LLM Corporate Law", BigDecimal.ZERO, "1 Year", "N", "Y", "N", "Master's", "Law", "Corporate Law", "^2[0-9]LLM[0-9]{4}$", "24LLM1001", "Y"),
            new Program("LLM-IP-LAW", "LLM Intellectual Property Law", BigDecimal.ZERO, "1 Year", "N", "Y", "N", "Master's", "Law", "Intellectual Property Law", "^2[0-9]LLM[0-9]{4}$", "24LLM1001", "Y"),
            new Program("LLM-INT-LAW", "LLM International Law & Development", BigDecimal.ZERO, "1 Year", "N", "Y", "N", "Master's", "Law", "International Law & Development", "^2[0-9]LLM[0-9]{4}$", "24LLM1001", "Y"),
            new Program("INT-MTECH-CSE", "Integrated M.Tech CSE", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Computer Science", null, "^2[0-9]MIS[0-9]{4}$", "21MIS1001", "Y"),
            new Program("INT-MTECH-CSE-DS", "Integrated M.Tech CSE (Data Science)", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Computer Science", "Data Science", "^2[0-9]MIS[0-9]{4}$", "21MIS1001", "Y"),
            new Program("INT-MTECH-BIOTECH", "Integrated M.Tech Biotechnology", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Biotechnology", null, "^2[0-9]MIB[0-9]{4}$", "21MIB1001", "Y"),
            new Program("INT-MTECH-CDS", "Integrated M.Tech Computational & Data Science", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Computer Science", "Computational & Data Science", "^2[0-9]MIS[0-9]{4}$", "21MIS1001", "Y"),
            new Program("INT-MTECH-CTM", "Integrated M.Tech Construction Technology & Management", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Civil", "Construction Technology & Management", "^2[0-9]MIC[0-9]{4}$", "21MIC1001", "Y"),
            new Program("INT-MTECH-MECH", "Integrated M.Tech Mechanical", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Mechanical", null, "^2[0-9]MIM[0-9]{4}$", "21MIM1001", "Y"),
            new Program("INT-MTECH-SWE", "Integrated M.Tech Software Engineering", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Computer Science", "Software Engineering", "^2[0-9]MIS[0-9]{4}$", "21MIS1001", "Y"),
            new Program("INT-MTECH-AI", "Integrated M.Tech AI", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Computer Science", "AI", "^2[0-9]MIS[0-9]{4}$", "21MIS1001", "Y"),
            new Program("INT-MTECH-AI-BIO", "Integrated M.Tech AI & Bioinformatics", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Computer Science", "AI & Bioinformatics", "^2[0-9]MIS[0-9]{4}$", "21MIS1001", "Y"),
            new Program("INT-MTECH-CSE-SEC", "Integrated M.Tech CSE (Cyber Security)", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Computer Science", "Cyber Security", "^2[0-9]MIS[0-9]{4}$", "21MIS1001", "Y"),
            new Program("INT-MSC-BIOTECH", "Integrated M.Sc Biotechnology", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Biotechnology", null, "^2[0-9]IMS[0-9]{4}$", "21IMS1001", "Y"),
            new Program("INT-MSC-CSDA", "Integrated M.Sc Computational Statistics & Data Analytics", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Computer Science", "Computational Statistics & Data Analytics", "^2[0-9]IMS[0-9]{4}$", "21IMS1001", "Y"),
            new Program("INT-MSC-PHYS", "Integrated M.Sc Physics", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Science", "Physics", "^2[0-9]IMP[0-9]{4}$", "21IMP1001", "Y"),
            new Program("INT-MSC-CHEM", "Integrated M.Sc Chemistry", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Science", "Chemistry", "^2[0-9]IMC[0-9]{4}$", "21IMC1001", "Y"),
            new Program("INT-MSC-MATH", "Integrated M.Sc Mathematics", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Science", "Mathematics", "^2[0-9]IMM[0-9]{4}$", "21IMM1001", "Y"),
            new Program("INT-MSC-PSYCH", "Integrated M.Sc Applied Psychology", BigDecimal.ZERO, "5 Years", "N", "Y", "N", "Integrated", "Science", "Applied Psychology", "^2[0-9]IMY[0-9]{4}$", "21IMY1001", "Y"),
            new Program("PHD-ENG", "Ph.D. Engineering", BigDecimal.ZERO, "4 Years", "N", "N", "Y", "Doctoral", "Engineering", null, "^2[0-9]PHD[0-9]{4}$", "24PHD1001", "Y"),
            new Program("PHD-MGMT", "Ph.D. Management", BigDecimal.ZERO, "4 Years", "N", "N", "Y", "Doctoral", "Management", null, "^2[0-9]PHD[0-9]{4}$", "24PHD1001", "Y"),
            new Program("PHD-SCI-LANG", "Ph.D. Science & Languages", BigDecimal.ZERO, "4 Years", "N", "N", "Y", "Doctoral", "Science", null, "^2[0-9]PHD[0-9]{4}$", "24PHD1001", "Y"),
            new Program("INT-PHD-ENG", "Integrated Ph.D. Engineering", BigDecimal.ZERO, "5 Years", "N", "N", "Y", "Doctoral", "Engineering", null, "^2[0-9]IPH[0-9]{4}$", "24IPH1001", "Y")
        );
    }

    private void ensureStudentProgramIds() {
        List<Student> students = studentRepository.findAll();
        boolean updated = false;
        for (Student s : students) {
            if (s.getProgramId() == null || s.getProgramId().isBlank()) {
                String regNo = s.getRegistrationNo();
                String branch = s.getBranch();
                if ((regNo != null && regNo.contains("BCT")) || (branch != null && branch.contains("Information Technology"))) {
                    s.setProgramId("BCT");
                } else if ((regNo != null && regNo.contains("BEC")) || (branch != null && branch.contains("Electronics"))) {
                    s.setProgramId("BEC");
                } else if ((regNo != null && regNo.contains("BME")) || (branch != null && branch.contains("Mechanical"))) {
                    s.setProgramId("BME");
                } else if ((regNo != null && regNo.contains("BDS")) || (branch != null && branch.contains("Data Science"))) {
                    s.setProgramId("BDS");
                } else if ((regNo != null && regNo.contains("BEE")) || (branch != null && branch.contains("Electrical"))) {
                    s.setProgramId("BEE");
                } else if ((regNo != null && regNo.contains("BCL")) || (branch != null && branch.contains("Civil"))) {
                    s.setProgramId("BCV");
                } else {
                    s.setProgramId("BCE");
                }
                updated = true;
            }
        }
        if (updated) {
            studentRepository.saveAll(students);
        }
    }
}
