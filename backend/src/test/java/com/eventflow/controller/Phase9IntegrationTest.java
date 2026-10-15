package com.eventflow.controller;

import com.eventflow.dto.CurrencyConvertRequest;
import com.eventflow.entity.*;
import com.eventflow.entity.enums.*;
import com.eventflow.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Phase9IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private TicketTierRepository ticketTierRepository;

    @Autowired
    private DynamicPricingRuleRepository dynamicPricingRuleRepository;

    @Autowired
    private EventCategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Event testEvent;
    private TicketTier testTier;

    @BeforeEach
    void setUp() {
        User organizer = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User u = new User("organizer.p9@example.com", passwordEncoder.encode("password123"), "P9 Organizer", "+155599901");
            return userRepository.save(u);
        });

        EventCategory category = categoryRepository.findAll().stream().findFirst().orElseGet(() ->
                categoryRepository.save(new EventCategory("Finance", "finance-summit", "Finance events", "dollar")));

        Venue venue = venueRepository.findAll().stream().findFirst().orElseGet(() -> {
            Venue v = new Venue();
            v.setName("Grand Arena P9");
            v.setCity("San Francisco");
            v.setCapacity(500);
            return venueRepository.save(v);
        });

        testEvent = new Event();
        testEvent.setTitle("FinTech Summit 2026");
        testEvent.setSlug("fintech-summit-2026-" + System.currentTimeMillis());
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent.setOrganizer(organizer);
        testEvent.setCategory(category);
        testEvent.setVenue(venue);
        testEvent.setStartTime(LocalDateTime.now().plusDays(30));
        testEvent.setEndTime(LocalDateTime.now().plusDays(32));
        testEvent = eventRepository.save(testEvent);

        testTier = new TicketTier();
        testTier.setEvent(testEvent);
        testTier.setName("VIP All Access");
        testTier.setPrice(new BigDecimal("100.00"));
        testTier.setCapacity(100);
        testTier.setSoldCount(85); // 85% sold! Should trigger surge!
        testTier = ticketTierRepository.save(testTier);
    }

    @Test
    void testCurrenciesListAndConversion() throws Exception {
        // 1. Currencies endpoint
        mockMvc.perform(get("/api/pricing/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.currencyCode == 'USD')]").exists())
                .andExpect(jsonPath("$.data[?(@.currencyCode == 'EUR')]").exists())
                .andExpect(jsonPath("$.data[?(@.currencyCode == 'GBP')]").exists())
                .andExpect(jsonPath("$.data[?(@.currencyCode == 'INR')]").exists());

        // 2. Conversion endpoint: 100 USD to EUR
        CurrencyConvertRequest req = new CurrencyConvertRequest(new BigDecimal("100.00"), "USD", "EUR");
        mockMvc.perform(post("/api/pricing/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fromCurrency").value("USD"))
                .andExpect(jsonPath("$.data.toCurrency").value("EUR"))
                .andExpect(jsonPath("$.data.symbol").value("€"))
                .andExpect(jsonPath("$.data.convertedAmount").value(92.50));
    }

    @Test
    void testDynamicSurgePricingQuote() throws Exception {
        // testTier has 100 total, 15 available => 85% sold.
        // Default surge rule triggers +15% surge (effective USD = $115.00).
        mockMvc.perform(get("/api/pricing/tiers/" + testTier.getId() + "/quote")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tierId").value(testTier.getId()))
                .andExpect(jsonPath("$.data.basePriceUsd").value(100.00))
                .andExpect(jsonPath("$.data.effectivePriceUsd").value(115.00))
                .andExpect(jsonPath("$.data.surgeActive").value(true))
                .andExpect(jsonPath("$.data.surgeMultiplier").value(1.15));

        // Test in EUR: $115 * 0.925 = 106.38 EUR
        mockMvc.perform(get("/api/pricing/tiers/" + testTier.getId() + "/quote")
                        .param("currency", "EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currency").value("EUR"))
                .andExpect(jsonPath("$.data.currencySymbol").value("€"))
                .andExpect(jsonPath("$.data.convertedPrice").value(106.38));
    }
}
