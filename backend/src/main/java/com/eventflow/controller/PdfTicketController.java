package com.eventflow.controller;

import com.eventflow.service.PdfTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets & Passes", description = "Ticket retrieval, QR check-in, and PDF pass exports")
public class PdfTicketController {

    private final PdfTicketService pdfTicketService;

    public PdfTicketController(PdfTicketService pdfTicketService) {
        this.pdfTicketService = pdfTicketService;
    }

    @GetMapping("/{ticketCode}/pdf")
    @Operation(summary = "Download official high-resolution PDF ticket admission pass")
    public ResponseEntity<byte[]> downloadTicketPdf(@PathVariable String ticketCode) {
        byte[] pdfBytes = pdfTicketService.generateTicketPdf(ticketCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "pass-" + ticketCode + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
