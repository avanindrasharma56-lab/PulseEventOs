package com.eventflow.service;

public interface PdfTicketService {
    byte[] generateTicketPdf(String ticketCode);
}
