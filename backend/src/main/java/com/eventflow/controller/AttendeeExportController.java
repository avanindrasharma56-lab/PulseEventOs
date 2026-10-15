package com.eventflow.controller;

import com.eventflow.entity.Event;
import com.eventflow.entity.Ticket;
import com.eventflow.entity.User;
import com.eventflow.entity.enums.RoleName;
import org.springframework.security.access.AccessDeniedException;
import com.eventflow.exception.ResourceNotFoundException;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.TicketRepository;
import com.eventflow.repository.UserRepository;
import com.eventflow.security.CurrentUser;
import com.eventflow.security.CustomUserDetails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/export")
public class AttendeeExportController {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public AttendeeExportController(
            EventRepository eventRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/attendees")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<byte[]> exportAttendeesCsv(
            @PathVariable Long eventId,
            @CurrentUser CustomUserDetails currentUser
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isOwner = event.getOrganizer().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You are not authorized to export attendee data for this event");
        }

        List<Ticket> tickets = ticketRepository.findByTicketTierEventIdOrderByCreatedAtDesc(eventId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            // Write UTF-8 BOM so Excel opens it with proper encoding
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            // CSV Header
            writer.println("Ticket Code,Attendee Name,Attendee Email,Tier,Price,Order Reference,Purchase Date,Status,Checked In At");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Ticket t : tickets) {
                String checkedInStr = t.getCheckedInAt() != null ? t.getCheckedInAt().format(dtf) : "N/A";
                String purchaseDateStr = t.getCreatedAt() != null ? t.getCreatedAt().format(dtf) : "";

                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        escapeCsv(t.getTicketCode()),
                        escapeCsv(t.getAttendeeName()),
                        escapeCsv(t.getAttendeeEmail()),
                        escapeCsv(t.getTicketTier() != null ? t.getTicketTier().getName() : "General"),
                        t.getTicketTier() != null ? t.getTicketTier().getPrice().toString() : "0.00",
                        escapeCsv(t.getOrder() != null ? t.getOrder().getOrderNumber() : ""),
                        escapeCsv(purchaseDateStr),
                        escapeCsv(t.getStatus() != null ? t.getStatus().name() : "VALID"),
                        escapeCsv(checkedInStr)
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate attendee CSV export", e);
        }

        String filename = "attendees-" + (event.getSlug() != null ? event.getSlug() : "event-" + eventId) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(out.toByteArray());
    }

    private String escapeCsv(String input) {
        if (input == null) return "\"\"";
        String clean = input.replace("\"", "\"\"");
        return "\"" + clean + "\"";
    }
}
