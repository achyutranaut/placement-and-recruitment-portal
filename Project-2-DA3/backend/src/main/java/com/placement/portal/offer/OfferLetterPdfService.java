package com.placement.portal.offer;

import com.placement.portal.student.Student;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class OfferLetterPdfService {

    private static final Logger log = LoggerFactory.getLogger(OfferLetterPdfService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public byte[] generateOfferLetterPdf(OfferDto offer, Student student) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float margin = 50;

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // Header Top Border Bar
                cs.setNonStrokingColor(15, 23, 42); // slate-900
                cs.addRect(margin, pageHeight - 45, pageWidth - 2 * margin, 6);
                cs.fill();

                // Institutional Title
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.setNonStrokingColor(15, 23, 42); // slate-900
                cs.newLineAtOffset(margin, pageHeight - 75);
                cs.showText("VELLORE INSTITUTE OF TECHNOLOGY");
                cs.endText();

                // Institutional Subtitle
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                cs.setNonStrokingColor(71, 85, 105); // slate-600
                cs.newLineAtOffset(margin, pageHeight - 90);
                cs.showText("CENTRE FOR CAREER PLANNING & PLACEMENTS (PAT)");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9);
                cs.setNonStrokingColor(100, 116, 139); // slate-500
                cs.newLineAtOffset(margin, pageHeight - 103);
                cs.showText("University Industry Relations & Corporate Talent Acquisition • Campus Recruitment Drive 2025-2026");
                cs.endText();

                // Divider line
                cs.setStrokingColor(203, 213, 225); // slate-300
                cs.setLineWidth(1.0f);
                cs.moveTo(margin, pageHeight - 115);
                cs.lineTo(pageWidth - margin, pageHeight - 115);
                cs.stroke();

                // Document Headline
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.setNonStrokingColor(30, 41, 59); // slate-800
                cs.newLineAtOffset(margin, pageHeight - 145);
                cs.showText("OFFICIAL LETTER OF EMPLOYMENT OFFER");
                cs.endText();

                // Meta Bar: Reference ID & Issue Date
                cs.setNonStrokingColor(241, 245, 249); // slate-100
                cs.addRect(margin, pageHeight - 185, pageWidth - 2 * margin, 26);
                cs.fill();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
                cs.setNonStrokingColor(51, 65, 85);
                cs.newLineAtOffset(margin + 12, pageHeight - 175);
                cs.showText("OFFER REFERENCE: " + offer.getOfferId());
                cs.endText();

                String formattedDate = offer.getOfferDate() != null
                        ? offer.getOfferDate().format(DATE_FORMATTER)
                        : "Current Academic Session";

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
                cs.setNonStrokingColor(51, 65, 85);
                cs.newLineAtOffset(pageWidth - margin - 180, pageHeight - 175);
                cs.showText("DATE OF ISSUE: " + formattedDate);
                cs.endText();

                // Candidate Details Box
                float y = pageHeight - 215;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.setNonStrokingColor(15, 23, 42);
                cs.newLineAtOffset(margin, y);
                cs.showText("Candidate Information:");
                cs.endText();

                y -= 18;
                String regNo = student != null && student.getRegistrationNo() != null ? student.getRegistrationNo() : "N/A";
                String branch = student != null && student.getBranch() != null ? student.getBranch() : "Engineering Discipline";
                String cgpaStr = student != null && student.getCgpa() != null ? String.format("%.2f / 10.00", student.getCgpa()) : "Verified Eligible";

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(51, 65, 85);
                cs.newLineAtOffset(margin, y);
                cs.showText("Full Name: " + offer.getStudentName());
                cs.endText();

                y -= 16;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Registration Number: " + regNo + "    |    Department: " + branch);
                cs.endText();

                y -= 16;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Institutional Email: " + (student != null ? student.getEmail() : "student@vitstudent.ac.in") + "    |    CGPA: " + cgpaStr);
                cs.endText();

                // Appointment & Compensation Section
                y -= 30;
                cs.setNonStrokingColor(248, 250, 252);
                cs.setStrokingColor(226, 232, 240);
                cs.addRect(margin, y - 75, pageWidth - 2 * margin, 85);
                cs.fill();
                cs.addRect(margin, y - 75, pageWidth - 2 * margin, 85);
                cs.stroke();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.setNonStrokingColor(15, 23, 42);
                cs.newLineAtOffset(margin + 16, y - 6);
                cs.showText("Placement Appointment Summary:");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(51, 65, 85);
                cs.newLineAtOffset(margin + 16, y - 26);
                cs.showText("Recruiting Organization: " + offer.getCompanyName());
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(51, 65, 85);
                cs.newLineAtOffset(margin + 16, y - 44);
                cs.showText("Offered Position / Job Title: " + offer.getJobTitle());
                cs.endText();

                String ctcDisplay = offer.getCtcLpa() != null
                        ? String.format("INR %.2f LPA (Lakhs Per Annum)", offer.getCtcLpa())
                        : "Competitive University Package";

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                cs.setNonStrokingColor(16, 185, 129); // emerald-600
                cs.newLineAtOffset(margin + 16, y - 62);
                cs.showText("Annual Compensation Package (CTC): " + ctcDisplay);
                cs.endText();

                // Formal Body Paragraphs
                y -= 105;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9.5f);
                cs.setNonStrokingColor(51, 65, 85);
                cs.newLineAtOffset(margin, y);
                cs.showText("Dear " + offer.getStudentName() + ",");
                cs.endText();

                y -= 18;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9.5f);
                cs.newLineAtOffset(margin, y);
                cs.showText("On behalf of the Centre for Career Planning & Placements and " + offer.getCompanyName() + ", we are");
                cs.endText();

                y -= 14;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9.5f);
                cs.newLineAtOffset(margin, y);
                cs.showText("pleased to extend this formal offer of campus placement appointment following your successful clearance");
                cs.endText();

                y -= 14;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9.5f);
                cs.newLineAtOffset(margin, y);
                cs.showText("of the multi-round recruitment evaluation process including online assessments, technical interviews,");
                cs.endText();

                y -= 14;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9.5f);
                cs.newLineAtOffset(margin, y);
                cs.showText("and corporate evaluations.");
                cs.endText();

                y -= 22;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9.5f);
                cs.newLineAtOffset(margin, y);
                cs.showText("This appointment is governed by the Placement Policy guidelines of Vellore Institute of Technology.");
                cs.endText();

                y -= 14;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9.5f);
                cs.newLineAtOffset(margin, y);
                cs.showText("Your final onboarding is contingent upon satisfactory completion of your graduating degree requirements");
                cs.endText();

                y -= 14;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9.5f);
                cs.newLineAtOffset(margin, y);
                cs.showText("without standing arrears, and pre-employment verification per corporate guidelines.");
                cs.endText();

                // Signatures Block
                y -= 65;
                cs.setStrokingColor(148, 163, 184); // slate-400
                cs.setLineWidth(0.8f);

                // Placement Cell Signature line
                cs.moveTo(margin, y);
                cs.lineTo(margin + 180, y);
                cs.stroke();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 9.5f);
                cs.setNonStrokingColor(15, 23, 42);
                cs.newLineAtOffset(margin, y - 14);
                cs.showText("Dr. Director of Placements");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8.5f);
                cs.setNonStrokingColor(100, 116, 139);
                cs.newLineAtOffset(margin, y - 26);
                cs.showText("Centre for Career Planning & Placements");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8.5f);
                cs.newLineAtOffset(margin, y - 37);
                cs.showText("Vellore Institute of Technology");
                cs.endText();

                // Corporate Representative Signature line
                float rightSignX = pageWidth - margin - 180;
                cs.moveTo(rightSignX, y);
                cs.lineTo(pageWidth - margin, y);
                cs.stroke();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 9.5f);
                cs.setNonStrokingColor(15, 23, 42);
                cs.newLineAtOffset(rightSignX, y - 14);
                cs.showText("Lead Talent Acquisition");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8.5f);
                cs.setNonStrokingColor(100, 116, 139);
                cs.newLineAtOffset(rightSignX, y - 26);
                cs.showText(offer.getCompanyName());
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8.5f);
                cs.newLineAtOffset(rightSignX, y - 37);
                cs.showText("Campus Hiring Division");
                cs.endText();

                // Footer Seal and Verification Text
                cs.setStrokingColor(226, 232, 240);
                cs.moveTo(margin, 55);
                cs.lineTo(pageWidth - margin, 55);
                cs.stroke();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                cs.setNonStrokingColor(148, 163, 184);
                cs.newLineAtOffset(margin, 42);
                cs.showText("Digitally validated through PlaceX University Placement Portal & Oracle Database Relational System.");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 8);
                cs.setNonStrokingColor(100, 116, 139);
                cs.newLineAtOffset(pageWidth - margin - 120, 42);
                cs.showText("Official Document Seal");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            log.info("Generated official Offer Letter PDF for Offer ID: {}, Candidate: {}", offer.getOfferId(), offer.getStudentName());
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate Offer Letter PDF for Offer ID: {}", offer.getOfferId(), e);
            throw new RuntimeException("Error rendering offer letter PDF: " + e.getMessage(), e);
        }
    }
}
