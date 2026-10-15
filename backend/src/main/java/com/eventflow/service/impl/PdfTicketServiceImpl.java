package com.eventflow.service.impl;

import com.eventflow.entity.Event;
import com.eventflow.entity.Ticket;
import com.eventflow.entity.TicketTier;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.TicketRepository;
import com.eventflow.service.PdfTicketService;
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

@Service
@Transactional(readOnly = true)
public class PdfTicketServiceImpl implements PdfTicketService {

    private final TicketRepository ticketRepository;

    public PdfTicketServiceImpl(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public byte[] generateTicketPdf(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "ticketCode", ticketCode));

        TicketTier tier = ticket.getTicketTier();
        Event event = tier.getEvent();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // Brand Header Banner
            PdfPTable headerTable = new PdfPTable(1);
            headerTable.setWidthPercentage(100);

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(new Color(9, 13, 22)); // #090D16 Deep Obsidian
            headerCell.setPadding(18);
            headerCell.setBorder(Rectangle.NO_BORDER);

            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(99, 102, 241)); // Indigo
            Paragraph brandPara = new Paragraph("PULSEEVENT OS — SECURE ADMISSION PASS", brandFont);
            brandPara.setAlignment(Element.ALIGN_CENTER);
            headerCell.addElement(brandPara);

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.WHITE);
            Paragraph titlePara = new Paragraph(event.getTitle(), titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingBefore(6);
            headerCell.addElement(titlePara);

            if (event.getShortSummary() != null && !event.getShortSummary().isEmpty()) {
                Font summaryFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(156, 163, 175));
                Paragraph sumPara = new Paragraph(event.getShortSummary(), summaryFont);
                sumPara.setAlignment(Element.ALIGN_CENTER);
                sumPara.setSpacingBefore(4);
                headerCell.addElement(sumPara);
            }

            headerTable.addCell(headerCell);
            document.add(headerTable);

            // Spacing
            document.add(new Paragraph(" "));

            // Details and QR Pass Split Layout
            PdfPTable bodyTable = new PdfPTable(2);
            bodyTable.setWidthPercentage(100);
            bodyTable.setWidths(new float[]{3.2f, 2.0f});

            // Left Column: Event & Attendee Details
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPaddingRight(14);

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy • hh:mm a");

            addDetailSection(leftCell, "DATE & TIME",
                    event.getStartTime() != null ? event.getStartTime().format(dtf) : "TBA",
                    event.getEndTime() != null ? "Until " + event.getEndTime().format(dtf) : "");

            String locationTitle = (event.getVenue() != null && event.getVenue().getName() != null)
                    ? event.getVenue().getName() : "Online / Virtual Auditorium";
            String locationSub = (event.getVenue() != null && event.getVenue().getAddress() != null)
                    ? event.getVenue().getAddress() + ", " + event.getVenue().getCity() : "Live Interactive Broadcast";
            addDetailSection(leftCell, "LOCATION", locationTitle, locationSub);

            addDetailSection(leftCell, "ATTENDEE",
                    ticket.getAttendeeName() != null ? ticket.getAttendeeName() : "Valued Guest",
                    ticket.getAttendeeEmail() != null ? ticket.getAttendeeEmail() : "");

            addDetailSection(leftCell, "PASS TIER & ORDER",
                    tier.getName() + " — $" + String.format("%.2f", tier.getPrice()),
                    "Order #" + ticket.getOrder().getOrderNumber() + " • Pass ID: " + ticket.getTicketCode());

            bodyTable.addCell(leftCell);

            // Right Column: High-Resolution ZXing QR Code
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBackgroundColor(new Color(248, 250, 252));
            rightCell.setBorder(Rectangle.BOX);
            rightCell.setBorderColor(new Color(226, 232, 240));
            rightCell.setPadding(12);
            rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Font qrHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(71, 85, 105));
            Paragraph qrHeader = new Paragraph("SCAN AT VENUE ENTRANCE", qrHeaderFont);
            qrHeader.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(qrHeader);

            if (ticket.getQrCodeData() != null && !ticket.getQrCodeData().isEmpty()) {
                String base64Data = ticket.getQrCodeData();
                if (base64Data.contains(",")) {
                    base64Data = base64Data.split(",")[1];
                }
                byte[] qrBytes = Base64.getDecoder().decode(base64Data);
                Image qrImage = Image.getInstance(qrBytes);
                qrImage.scaleAbsolute(140f, 140f);
                qrImage.setAlignment(Element.ALIGN_CENTER);
                qrImage.setSpacingBefore(8);
                qrImage.setSpacingAfter(8);
                rightCell.addElement(qrImage);
            }

            Font codeFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 10, new Color(15, 23, 42));
            Paragraph codePara = new Paragraph(ticket.getTicketCode(), codeFont);
            codePara.setAlignment(Element.ALIGN_CENTER);
            rightCell.addElement(codePara);

            Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(16, 185, 129));
            Paragraph statusPara = new Paragraph("STATUS: " + ticket.getStatus().name(), statusFont);
            statusPara.setAlignment(Element.ALIGN_CENTER);
            statusPara.setSpacingBefore(4);
            rightCell.addElement(statusPara);

            bodyTable.addCell(rightCell);
            document.add(bodyTable);

            // Spacing
            document.add(new Paragraph(" "));

            // Security Guidelines & Anti-Replay Footer
            PdfPTable footerTable = new PdfPTable(1);
            footerTable.setWidthPercentage(100);

            PdfPCell footerCell = new PdfPCell();
            footerCell.setBackgroundColor(new Color(241, 245, 249));
            footerCell.setPadding(10);
            footerCell.setBorder(Rectangle.NO_BORDER);

            Font footTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(51, 65, 85));
            Paragraph footTitle = new Paragraph("ENTRY POLICY & SECURITY INSTRUCTIONS", footTitleFont);
            footerCell.addElement(footTitle);

            Font footTextFont = FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(100, 116, 139));
            Paragraph footText = new Paragraph(
                    "1. This admission pass is single-use and cryptographically tracked. Duplicate scans will trigger anti-replay security alerts at the door.\n" +
                    "2. Have this PDF pass displayed on your mobile device screen at maximum brightness or print it on standard A4 paper.\n" +
                    "3. Present government-issued photo identification matching the attendee name upon request.",
                    footTextFont
            );
            footText.setSpacingBefore(3);
            footerCell.addElement(footText);

            footerTable.addCell(footerCell);
            document.add(footerTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render PDF admission pass: " + e.getMessage(), e);
        }
    }

    private void addDetailSection(PdfPCell cell, String category, String primary, String secondary) {
        Font catFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(99, 102, 241));
        Paragraph catPara = new Paragraph(category, catFont);
        catPara.setSpacingBefore(6);
        cell.addElement(catPara);

        Font primFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(15, 23, 42));
        Paragraph primPara = new Paragraph(primary, primFont);
        primPara.setSpacingBefore(2);
        cell.addElement(primPara);

        if (secondary != null && !secondary.isEmpty()) {
            Font secFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(100, 116, 139));
            Paragraph secPara = new Paragraph(secondary, secFont);
            secPara.setSpacingBefore(1);
            cell.addElement(secPara);
        }
    }
}
