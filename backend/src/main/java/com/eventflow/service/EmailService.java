package com.eventflow.service;

import com.eventflow.entity.Event;
import com.eventflow.entity.Order;

public interface EmailService {

    void sendTicketConfirmationEmail(Order order);

    void sendEventStatusUpdateEmail(Event event, String reason);

    void sendEmail(String toEmail, String subject, String htmlContent);
}
