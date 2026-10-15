package com.eventflow.service.impl;

import com.eventflow.dto.CertificateClaimRequest;
import com.eventflow.dto.CertificateDto;
import com.eventflow.dto.CertificateVerificationDto;
import com.eventflow.entity.Certificate;
import com.eventflow.entity.Event;
import com.eventflow.entity.Ticket;
import com.eventflow.entity.User;
import com.eventflow.entity.enums.CertificateType;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.CertificateRepository;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.TicketRepository;
import com.eventflow.repository.UserRepository;
import com.eventflow.service.CertificateService;
import com.eventflow.service.QrCodeService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final QrCodeService qrCodeService;

    public CertificateServiceImpl(CertificateRepository certificateRepository,
                                  EventRepository eventRepository,
                                  UserRepository userRepository,
                                  TicketRepository ticketRepository,
                                  QrCodeService qrCodeService) {
        this.certificateRepository = certificateRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.qrCodeService = qrCodeService;
    }

    @Override
    public CertificateDto claimCertificate(Long eventId, String username, CertificateClaimRequest request) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", username));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        // If certificate already issued, return existing
        return certificateRepository.findByEventIdAndUserId(eventId, user.getId())
                .map(this::mapToDto)
                .orElseGet(() -> createCertificateForUser(event, user, request));
    }

    @Override
    public CertificateDto issueCertificateForUser(Long eventId, Long targetUserId, CertificateClaimRequest request) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        return certificateRepository.findByEventIdAndUserId(eventId, user.getId())
                .map(this::mapToDto)
                .orElseGet(() -> createCertificateForUser(event, user, request));
    }

    private CertificateDto createCertificateForUser(Event event, User user, CertificateClaimRequest request) {
        // Find matching ticket for event
        List<Ticket> userTickets = ticketRepository.findByOrderUserId(user.getId());
        Ticket matchingTicket = userTickets.stream()
                .filter(t -> t.getTicketTier().getEvent().getId().equals(event.getId()))
                .findFirst()
                .orElse(null);

        // Calculate hours completed based on event duration
        int hours = 8;
        if (event.getStartTime() != null && event.getEndTime() != null) {
            long durationHours = Duration.between(event.getStartTime(), event.getEndTime()).toHours();
            if (durationHours > 0) {
                hours = (int) Math.min(durationHours, 80);
            }
        }

        String code = "CERT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        CertificateType certType = (request != null && request.getCertificateType() != null)
                ? request.getCertificateType()
                : CertificateType.COMPLETION;

        String recipientName = (user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName()
                : user.getEmail();

        Certificate certificate = new Certificate(
                code,
                event,
                user,
                matchingTicket,
                recipientName,
                event.getTitle(),
                LocalDate.now(),
                certType,
                hours
        );

        Certificate saved = certificateRepository.save(certificate);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateCertificatePdf(String certificateCode) {
        Certificate cert = certificateRepository.findByCertificateCode(certificateCode)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", "certificateCode", certificateCode));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Landscape A4 (842 x 595 pt)
            Rectangle landscapeA4 = PageSize.A4.rotate();
            Document document = new Document(landscapeA4, 28, 28, 28, 28);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Fonts
            Font headerSmallFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(100, 116, 139));
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(15, 23, 42));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(100, 116, 139));
            Font recipientFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, new Color(30, 27, 75));
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new Color(51, 65, 85));
            Font eventFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(99, 102, 241));
            Font metaLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(100, 116, 139));
            Font metaValueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(30, 41, 59));
            Font codeFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 10, new Color(15, 23, 42));

            // Outer Frame Table
            PdfPTable outerTable = new PdfPTable(1);
            outerTable.setWidthPercentage(100);

            PdfPCell outerCell = new PdfPCell();
            outerCell.setBorderColor(new Color(217, 119, 6)); // Amber / Gold border
            outerCell.setBorderWidth(4f);
            outerCell.setPadding(18f);
            outerCell.setBackgroundColor(new Color(254, 252, 232, 20)); // Subtle ivory tint

            // Inner Content Table
            PdfPTable innerTable = new PdfPTable(1);
            innerTable.setWidthPercentage(100);

            // 1. Top Organization Header
            PdfPCell orgCell = new PdfPCell(new Phrase("EVENTFLOW ACADEMY & GLOBAL SUMMITS", headerSmallFont));
            orgCell.setBorder(Rectangle.NO_BORDER);
            orgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            orgCell.setPaddingBottom(6f);
            innerTable.addCell(orgCell);

            // 2. Main Certificate Title
            String titleText = "CERTIFICATE OF " + cert.getCertificateType().name();
            PdfPCell titleCell = new PdfPCell(new Phrase(titleText, titleFont));
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.setPaddingBottom(12f);
            innerTable.addCell(titleCell);

            // 3. Subtitle / Certifies That
            PdfPCell subCell = new PdfPCell(new Phrase("THIS IS PROUDLY PRESENTED TO", subtitleFont));
            subCell.setBorder(Rectangle.NO_BORDER);
            subCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            subCell.setPaddingBottom(10f);
            innerTable.addCell(subCell);

            // 4. Recipient Name
            PdfPCell nameCell = new PdfPCell(new Phrase(cert.getRecipientName(), recipientFont));
            nameCell.setBorder(Rectangle.NO_BORDER);
            nameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            nameCell.setPaddingBottom(12f);
            innerTable.addCell(nameCell);

            // Decorative underline bar
            PdfPTable lineTable = new PdfPTable(1);
            lineTable.setWidthPercentage(45);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBackgroundColor(new Color(217, 119, 6));
            lineCell.setFixedHeight(2f);
            lineCell.setBorder(Rectangle.NO_BORDER);
            lineTable.addCell(lineCell);
            innerTable.addCell(lineTable);

            // 5. Body Text
            String bodyText = "\nfor successfully attending and completing all professional sessions, architectural keynotes,\nand technical tracks for:";
            PdfPCell bodyCell = new PdfPCell(new Phrase(bodyText, bodyFont));
            bodyCell.setBorder(Rectangle.NO_BORDER);
            bodyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            bodyCell.setPaddingBottom(8f);
            innerTable.addCell(bodyCell);

            // 6. Event Name
            PdfPCell eventCell = new PdfPCell(new Phrase(cert.getEventTitle(), eventFont));
            eventCell.setBorder(Rectangle.NO_BORDER);
            eventCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            eventCell.setPaddingBottom(18f);
            innerTable.addCell(eventCell);

            // 7. Footer 3-Column Layout: [Signatures] | [Official Seal Badge] | [QR Code & Code]
            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{35f, 35f, 30f});

            // Column 1: Signatures & Hours
            PdfPCell sigCell = new PdfPCell();
            sigCell.setBorder(Rectangle.NO_BORDER);
            sigCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
            sigCell.addElement(new Paragraph("Issue Date: " + cert.getIssueDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")), metaValueFont));
            sigCell.addElement(new Paragraph("Hours Accredited: " + cert.getHoursCompleted() + " CPD Units", metaValueFont));
            sigCell.addElement(new Paragraph("\n__________________________________", metaLabelFont));
            sigCell.addElement(new Paragraph("Academic & Program Committee Chair", metaLabelFont));
            footerTable.addCell(sigCell);

            // Column 2: Center Seal
            PdfPCell sealCell = new PdfPCell();
            sealCell.setBorder(Rectangle.NO_BORDER);
            sealCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            sealCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Paragraph sealText = new Paragraph("★ OFFICIAL CREDENTIAL ★\nVERIFIED ATTESTATION\nEVENTFLOW OS", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(217, 119, 6)));
            sealText.setAlignment(Element.ALIGN_CENTER);
            sealCell.addElement(sealText);
            footerTable.addCell(sealCell);

            // Column 3: Verification QR Code & Certificate Code
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            qrCell.setVerticalAlignment(Element.ALIGN_BOTTOM);

            String verificationUrl = "http://localhost:5173/verify-certificate/" + cert.getCertificateCode();
            String qrBase64 = qrCodeService.generateQrCodeBase64(verificationUrl, 110, 110);
            if (qrBase64 != null) {
                if (qrBase64.contains(",")) {
                    qrBase64 = qrBase64.split(",")[1];
                }
                byte[] qrBytes = Base64.getDecoder().decode(qrBase64);
                Image qrImg = Image.getInstance(qrBytes);
                qrImg.scaleToFit(75, 75);
                qrImg.setAlignment(Element.ALIGN_RIGHT);
                qrCell.addElement(qrImg);
            }

            Paragraph codePara = new Paragraph(cert.getCertificateCode(), codeFont);
            codePara.setAlignment(Element.ALIGN_RIGHT);
            qrCell.addElement(codePara);

            Paragraph scanNote = new Paragraph("Scan QR to verify on-chain authenticity", metaLabelFont);
            scanNote.setAlignment(Element.ALIGN_RIGHT);
            qrCell.addElement(scanNote);

            footerTable.addCell(qrCell);
            innerTable.addCell(footerTable);

            outerCell.addElement(innerTable);
            outerTable.addCell(outerCell);
            document.add(outerTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate certificate PDF: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateVerificationDto verifyCertificate(String certificateCode) {
        return certificateRepository.findByCertificateCode(certificateCode)
                .map(cert -> {
                    CertificateVerificationDto dto = new CertificateVerificationDto();
                    dto.setValid(true);
                    dto.setCertificateCode(cert.getCertificateCode());
                    dto.setRecipientName(cert.getRecipientName());
                    dto.setEventTitle(cert.getEventTitle());
                    dto.setIssueDate(cert.getIssueDate());
                    dto.setCertificateType(cert.getCertificateType());
                    dto.setHoursCompleted(cert.getHoursCompleted());
                    dto.setOrganizerName(cert.getEvent().getOrganizer() != null ? cert.getEvent().getOrganizer().getFullName() : "PulseEvent Platform");
                    dto.setVenueName(cert.getEvent().getVenue() != null ? cert.getEvent().getVenue().getName() : "Online");
                    dto.setVerifiedAt(LocalDateTime.now());
                    dto.setMessage("Credential is authentic and registered in EventFlow ledger.");
                    return dto;
                })
                .orElseGet(() -> CertificateVerificationDto.invalid(certificateCode, "No certificate found with code " + certificateCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateDto> getMyCertificates(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", username));

        return certificateRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateDto> getEventCertificates(Long eventId, String requesterUsername) {
        User requester = userRepository.findByEmail(requesterUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", requesterUsername));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        boolean isAdmin = requester.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isOrganizer = event.getOrganizer() != null && event.getOrganizer().getId().equals(requester.getId());

        if (!isAdmin && !isOrganizer) {
            throw new AccessDeniedException("Only the event organizer or an admin may view all event certificates.");
        }

        return certificateRepository.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private CertificateDto mapToDto(Certificate c) {
        CertificateDto dto = new CertificateDto();
        dto.setId(c.getId());
        dto.setCertificateCode(c.getCertificateCode());
        dto.setEventId(c.getEvent().getId());
        dto.setEventTitle(c.getEventTitle());
        dto.setUserId(c.getUser().getId());
        dto.setRecipientName(c.getRecipientName());
        dto.setTicketCode(c.getTicket() != null ? c.getTicket().getTicketCode() : null);
        dto.setIssueDate(c.getIssueDate());
        dto.setCertificateType(c.getCertificateType());
        dto.setHoursCompleted(c.getHoursCompleted());
        dto.setVerificationUrl("http://localhost:5173/verify-certificate/" + c.getCertificateCode());
        dto.setDownloadPdfUrl("/api/certificates/" + c.getCertificateCode() + "/pdf");
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }
}
