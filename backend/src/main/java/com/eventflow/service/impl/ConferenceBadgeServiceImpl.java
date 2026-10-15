package com.eventflow.service.impl;

import com.eventflow.entity.Event;
import com.eventflow.entity.Ticket;
import com.eventflow.entity.TicketTier;
import com.eventflow.entity.User;
import com.eventflow.entity.Venue;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.TicketStatus;
import org.springframework.security.access.AccessDeniedException;
import com.eventflow.exception.BadRequestException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.TicketRepository;
import com.eventflow.repository.UserRepository;
import com.eventflow.service.ConferenceBadgeService;
import com.eventflow.service.QrCodeService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConferenceBadgeServiceImpl implements ConferenceBadgeService {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;

    public ConferenceBadgeServiceImpl(TicketRepository ticketRepository,
                                     EventRepository eventRepository,
                                     UserRepository userRepository,
                                     QrCodeService qrCodeService) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
    }

    @Override
    public byte[] generateSingleBadgePdf(String ticketCode, Long currentUserId) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "ticketCode", ticketCode));

        validateTicketAccess(ticket, currentUserId);

        return renderBadgesPdf(Collections.singletonList(ticket));
    }

    @Override
    public byte[] generateBulkBadgesPdf(Long eventId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        validateOrganizerOrAdmin(event, currentUserId);

        List<Ticket> tickets = ticketRepository.findByTicketTierEventIdOrderByCreatedAtDesc(eventId);
        if (tickets.isEmpty()) {
            throw new BadRequestException("No tickets have been issued yet for this event.");
        }

        return renderBadgesPdf(tickets);
    }

    private byte[] renderBadgesPdf(List<Ticket> tickets) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Standard 4" x 6" executive badge size (288pt x 432pt)
            Rectangle badgePageSize = new Rectangle(288f, 432f);
            Document document = new Document(badgePageSize, 14f, 14f, 14f, 14f);
            PdfWriter.getInstance(document, out);
            document.open();

            boolean isFirstPage = true;
            for (Ticket ticket : tickets) {
                if (!isFirstPage) {
                    document.newPage();
                }
                isFirstPage = false;
                renderSingleBadgePage(document, ticket);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to render printable conference badge PDF: " + ex.getMessage(), ex);
        }
    }

    private void renderSingleBadgePage(Document document, Ticket ticket) throws Exception {
        TicketTier tier = ticket.getTicketTier();
        Event event = tier.getEvent();
        Venue venue = event.getVenue();

        // 1. Top Lanyard Slot Guide
        Font slotFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new Color(148, 163, 184));
        Paragraph slotGuide = new Paragraph("— — — LANYARD CLIP / SLOT HOLE — — —", slotFont);
        slotGuide.setAlignment(Element.ALIGN_CENTER);
        slotGuide.setSpacingAfter(6f);
        document.add(slotGuide);

        // 2. Color-Coded Header Ribbon
        Color bannerColor = getTierColor(tier.getName());
        PdfPTable bannerTable = new PdfPTable(1);
        bannerTable.setWidthPercentage(100);

        PdfPCell bannerCell = new PdfPCell();
        bannerCell.setBackgroundColor(bannerColor);
        bannerCell.setPaddingTop(8f);
        bannerCell.setPaddingBottom(8f);
        bannerCell.setBorder(Rectangle.NO_BORDER);

        Font tierFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
        Paragraph tierPara = new Paragraph(tier.getName().toUpperCase(), tierFont);
        tierPara.setAlignment(Element.ALIGN_CENTER);
        bannerCell.addElement(tierPara);

        bannerTable.addCell(bannerCell);
        document.add(bannerTable);

        // 3. Event Title & Date / Location Header
        PdfPTable eventTable = new PdfPTable(1);
        eventTable.setWidthPercentage(100);
        eventTable.setSpacingBefore(8f);

        PdfPCell eventCell = new PdfPCell();
        eventCell.setBackgroundColor(new Color(248, 250, 252));
        eventCell.setBorder(Rectangle.NO_BORDER);
        eventCell.setPadding(8f);

        Font eventTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(15, 23, 42));
        Paragraph evTitle = new Paragraph(event.getTitle(), eventTitleFont);
        evTitle.setAlignment(Element.ALIGN_CENTER);
        eventCell.addElement(evTitle);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        String dateStr = event.getStartTime() != null ? event.getStartTime().format(dtf) : "Upcoming";
        String venueStr = venue != null ? venue.getCity() + ", " + venue.getName() : "Main Stage";

        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 116, 139));
        Paragraph evSub = new Paragraph(dateStr + " • " + venueStr, subFont);
        evSub.setAlignment(Element.ALIGN_CENTER);
        evSub.setSpacingBefore(2f);
        eventCell.addElement(evSub);

        eventTable.addCell(eventCell);
        document.add(eventTable);

        // 4. Attendee Showcase (Prominent Name & Subtitle)
        PdfPTable attendeeTable = new PdfPTable(1);
        attendeeTable.setWidthPercentage(100);
        attendeeTable.setSpacingBefore(12f);

        PdfPCell attCell = new PdfPCell();
        attCell.setBorder(Rectangle.NO_BORDER);
        attCell.setPadding(4f);

        Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(9, 13, 22));
        String attName = ticket.getAttendeeName() != null ? ticket.getAttendeeName().toUpperCase() : "CONFERENCE ATTENDEE";
        Paragraph namePara = new Paragraph(attName, nameFont);
        namePara.setAlignment(Element.ALIGN_CENTER);
        attCell.addElement(namePara);

        Font emailFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(100, 116, 139));
        Paragraph emailPara = new Paragraph(ticket.getAttendeeEmail() != null ? ticket.getAttendeeEmail() : "", emailFont);
        emailPara.setAlignment(Element.ALIGN_CENTER);
        emailPara.setSpacingBefore(2f);
        attCell.addElement(emailPara);

        attendeeTable.addCell(attCell);
        document.add(attendeeTable);

        // 5. High-Resolution Scannable QR Code
        String qrBase64 = qrCodeService.generateQrCodeBase64(ticket.getTicketCode(), 200, 200);
        if (qrBase64.contains(",")) {
            qrBase64 = qrBase64.split(",")[1];
        }
        byte[] qrBytes = Base64.getDecoder().decode(qrBase64);
        Image qrImage = Image.getInstance(qrBytes);
        qrImage.scaleAbsolute(95f, 95f);
        qrImage.setAlignment(Element.ALIGN_CENTER);
        qrImage.setSpacingBefore(10f);
        qrImage.setSpacingAfter(4f);
        document.add(qrImage);

        // Monospace Ticket Identifier
        Font codeFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 9, new Color(15, 23, 42));
        Paragraph codePara = new Paragraph(ticket.getTicketCode(), codeFont);
        codePara.setAlignment(Element.ALIGN_CENTER);
        document.add(codePara);

        // 6. Bottom Wi-Fi & Venue Info Box
        PdfPTable infoTable = new PdfPTable(1);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10f);

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBackgroundColor(new Color(241, 245, 249));
        infoCell.setBorder(Rectangle.BOX);
        infoCell.setBorderColor(new Color(226, 232, 240));
        infoCell.setPadding(6f);

        Font wifiFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new Color(51, 65, 85));
        Paragraph wifiPara = new Paragraph("Wi-Fi: " + (venue != null ? venue.getName().replace(" ", "") + "-Guest" : "Conference-Guest") + "  |  Password: Connect" + event.getStartTime().getYear(), wifiFont);
        wifiPara.setAlignment(Element.ALIGN_CENTER);
        infoCell.addElement(wifiPara);

        Font hashtagFont = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(100, 116, 139));
        Paragraph hashtagPara = new Paragraph("#" + event.getTitle().replaceAll("[^a-zA-Z0-9]", "") + "  •  Authorized Entry Pass", hashtagFont);
        hashtagPara.setAlignment(Element.ALIGN_CENTER);
        hashtagPara.setSpacingBefore(2f);
        infoCell.addElement(hashtagPara);

        infoTable.addCell(infoCell);
        document.add(infoTable);
    }

    private Color getTierColor(String tierName) {
        if (tierName == null) return new Color(37, 99, 235);
        String upper = tierName.toUpperCase();
        if (upper.contains("VIP") || upper.contains("ALL ACCESS") || upper.contains("PRESIDENT")) {
            return new Color(16, 185, 129); // Emerald
        } else if (upper.contains("SPEAKER") || upper.contains("KEYNOTE") || upper.contains("PANEL")) {
            return new Color(99, 102, 241); // Indigo
        } else if (upper.contains("STAFF") || upper.contains("ADMIN") || upper.contains("CREW")) {
            return new Color(244, 63, 94); // Rose
        } else if (upper.contains("WORKSHOP") || upper.contains("EARLY")) {
            return new Color(14, 165, 233); // Sky
        }
        return new Color(30, 41, 59); // Slate Dark
    }

    private void validateTicketAccess(Ticket ticket, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isOrganizer = ticket.getTicketTier().getEvent().getOrganizer().getId().equals(currentUserId);
        boolean isOwner = ticket.getOrder().getUser().getId().equals(currentUserId);

        if (!isOwner && !isOrganizer && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to view or print this ticket badge");
        }
    }

    private void validateOrganizerOrAdmin(Event event, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isOrganizer = event.getOrganizer().getId().equals(currentUserId);

        if (!isOrganizer && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to export bulk badges for this event");
        }
    }
}
