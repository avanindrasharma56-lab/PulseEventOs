package com.eventflow.controller;

import com.eventflow.entity.*;
import com.eventflow.entity.enums.EventStatus;
import com.eventflow.entity.enums.OrderStatus;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.UserStatus;
import com.eventflow.repository.*;
import com.eventflow.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@Transactional
class EmailServiceIntegrationTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EventCategoryRepository categoryRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketTierRepository ticketTierRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Should successfully construct and dispatch HTML ticket confirmation email with embedded QR code")
    void testSendTicketConfirmationEmail() {
        Role attendeeRole = roleRepository.findByName(RoleName.ROLE_ATTENDEE).orElseThrow();
        Role orgRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER).orElseThrow();

        String runId = UUID.randomUUID().toString().substring(0, 6);

        User organizer = new User("org_mail_" + runId + "@pulse.com", passwordEncoder.encode("Password123!"), "Mail Organizer", null);
        organizer.setRoles(Collections.singleton(orgRole));
        organizer.setStatus(UserStatus.ACTIVE);
        organizer = userRepository.save(organizer);

        User buyer = new User("buyer_mail_" + runId + "@pulse.com", passwordEncoder.encode("Password123!"), "Mail Buyer", null);
        buyer.setRoles(Collections.singleton(attendeeRole));
        buyer.setStatus(UserStatus.ACTIVE);
        buyer = userRepository.save(buyer);

        EventCategory cat = categoryRepository.save(new EventCategory("Tech " + runId, "tech-" + runId, "Tech", "Cpu"));
        Venue venue = venueRepository.save(new Venue("Grand Hall " + runId, "100 Ave", "Austin", "TX", "78701", "USA", 1000, false, null));

        Event event = new Event(
                organizer,
                cat,
                venue,
                "Pulse Gala " + runId,
                "pulse-gala-" + runId,
                "Premier showcase",
                "Full descriptions",
                null,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(4)
        );
        event.setStatus(EventStatus.PUBLISHED);
        event = eventRepository.save(event);

        TicketTier tier = new TicketTier(event, "VIP Pass", "Full VIP Access", BigDecimal.valueOf(199.00), 100);
        tier = ticketTierRepository.save(tier);

        Order order = new Order("ORD-MAIL-" + runId, buyer, event, BigDecimal.valueOf(199.00), OrderStatus.CONFIRMED);
        Ticket ticket = new Ticket(order, tier, "TCK-VIP-001", "Mail Buyer", buyer.getEmail(), "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        order.addTicket(ticket);
        order = orderRepository.save(order);

        Order finalOrder = order;
        assertDoesNotThrow(() -> emailService.sendTicketConfirmationEmail(finalOrder));
    }

    @Test
    @DisplayName("Should successfully dispatch event status update email to organizer")
    void testSendEventStatusUpdateEmail() {
        Role orgRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER).orElseThrow();
        String runId = UUID.randomUUID().toString().substring(0, 6);

        User organizer = new User("org_status_" + runId + "@pulse.com", passwordEncoder.encode("Password123!"), "Status Organizer", null);
        organizer.setRoles(Collections.singleton(orgRole));
        organizer.setStatus(UserStatus.ACTIVE);
        organizer = userRepository.save(organizer);

        EventCategory cat = categoryRepository.save(new EventCategory("Dev " + runId, "dev-" + runId, "Dev", "Code"));
        Venue venue = venueRepository.save(new Venue("Center " + runId, "200 St", "Dallas", "TX", "75001", "USA", 500, false, null));

        Event event = new Event(
                organizer,
                cat,
                venue,
                "Dev Summit " + runId,
                "dev-summit-" + runId,
                "Developers unite",
                "Descriptions",
                null,
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(3).plusHours(5)
        );
        event.setStatus(EventStatus.APPROVED);
        event = eventRepository.save(event);

        Event finalEvent = event;
        assertDoesNotThrow(() -> emailService.sendEventStatusUpdateEmail(finalEvent, "Event approved by system moderator"));
    }
}
