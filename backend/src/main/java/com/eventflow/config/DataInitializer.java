package com.eventflow.config;

import com.eventflow.entity.Event;
import com.eventflow.entity.Role;
import com.eventflow.entity.TicketTier;
import com.eventflow.entity.User;
import com.eventflow.entity.enums.RoleName;
import com.eventflow.entity.enums.UserStatus;
import com.eventflow.repository.EventRepository;
import com.eventflow.repository.RoleRepository;
import com.eventflow.repository.TicketTierRepository;
import com.eventflow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            EventRepository eventRepository,
            TicketTierRepository ticketTierRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.eventRepository = eventRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedRoleIfMissing(RoleName.ROLE_ADMIN);
        seedRoleIfMissing(RoleName.ROLE_ORGANIZER);
        seedRoleIfMissing(RoleName.ROLE_ATTENDEE);

        // Seed Default Admin
        if (!userRepository.existsByEmail("admin@pulsevent.io")) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
            User admin = new User(
                    "admin@pulsevent.io",
                    passwordEncoder.encode("AdminPassword@123"),
                    "Chief Administrator",
                    "+18005550199"
            );
            admin.setStatus(UserStatus.ACTIVE);
            admin.setRoles(Collections.singleton(adminRole));
            userRepository.save(admin);
            log.info("Initialized default administrator: admin@pulsevent.io");
        }

        // Seed Default Organizer
        if (!userRepository.existsByEmail("organizer@pulsevent.io")) {
            Role orgRole = roleRepository.findByName(RoleName.ROLE_ORGANIZER).orElseThrow();
            User organizer = new User(
                    "organizer@pulsevent.io",
                    passwordEncoder.encode("OrganizerPassword@123"),
                    "Pulse Host Organizer",
                    "+18005550188"
            );
            organizer.setStatus(UserStatus.ACTIVE);
            organizer.setRoles(Collections.singleton(orgRole));
            userRepository.save(organizer);
            log.info("Initialized default organizer: organizer@pulsevent.io");
        }

        // Seed default ticket tiers for events that don't have any
        List<Event> events = eventRepository.findAll();
        for (Event event : events) {
            List<TicketTier> tiers = ticketTierRepository.findByEventIdOrderByPriceAsc(event.getId());
            if (tiers.isEmpty()) {
                TicketTier early = new TicketTier(event, "Early Bird Pass", "Discounted priority entry with full access", new BigDecimal("49.00"), 50);
                TicketTier standard = new TicketTier(event, "General Admission", "Full standard conference and workshop access", new BigDecimal("99.00"), 200);
                TicketTier vip = new TicketTier(event, "VIP Founder Access", "Includes keynote front row, backstage networking, and catering", new BigDecimal("199.00"), 25);
                ticketTierRepository.save(early);
                ticketTierRepository.save(standard);
                ticketTierRepository.save(vip);
                log.info("Initialized 3 default ticket tiers for event: {}", event.getTitle());
            }
        }
    }

    private void seedRoleIfMissing(RoleName roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            roleRepository.save(new Role(roleName));
        }
    }
}
