package com.eventflow.service.impl;

import com.eventflow.entity.Event;
import com.eventflow.entity.Order;
import com.eventflow.entity.Ticket;
import com.eventflow.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");

    private final JavaMailSender mailSender;

    @Value("${pulse.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${pulse.mail.mode:simulated}")
    private String mailMode;

    @Value("${pulse.mail.from:noreply@eventpulse.io}")
    private String fromEmail;

    @Value("${pulse.mail.from-name:PulseEvent OS}")
    private String fromName;

    public EmailServiceImpl(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendTicketConfirmationEmail(Order order) {
        if (order == null || order.getUser() == null || order.getUser().getEmail() == null) {
            return;
        }

        String toEmail = order.getUser().getEmail();
        String eventTitle = order.getEvent().getTitle();
        String subject = "Your Tickets for " + eventTitle + " [Order #" + order.getOrderNumber() + "]";
        String htmlContent = buildTicketConfirmationHtml(order);

        sendEmail(toEmail, subject, htmlContent);
    }

    @Override
    public void sendEventStatusUpdateEmail(Event event, String reason) {
        if (event == null || event.getOrganizer() == null || event.getOrganizer().getEmail() == null) {
            return;
        }

        String toEmail = event.getOrganizer().getEmail();
        String subject = "Event Update: \"" + event.getTitle() + "\" is " + event.getStatus();
        String htmlContent = buildEventStatusHtml(event, reason);

        sendEmail(toEmail, subject, htmlContent);
    }

    @Override
    public void sendEmail(String toEmail, String subject, String htmlContent) {
        if (!mailEnabled) {
            log.info("Email delivery disabled by config. Skipping email to {}", toEmail);
            return;
        }

        log.info("[PULSE EMAIL ENGINE] Dispatched email to {} | Subject: '{}' | Pass HTML Payload: {} bytes",
                toEmail, subject, htmlContent.length());

        if ("smtp".equalsIgnoreCase(mailMode) && mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail, fromName);
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("Successfully delivered SMTP message to {}", toEmail);
            } catch (Exception e) {
                log.warn("Could not deliver SMTP email to {} (retained in log audit): {}", toEmail, e.getMessage());
            }
        }
    }

    private String buildTicketConfirmationHtml(Order order) {
        Event event = order.getEvent();
        String venueText = event.getVenue() != null 
                ? (event.getVenue().getName() + ", " + event.getVenue().getAddress() + ", " + event.getVenue().getCity())
                : "Online Virtual Event";
        String dateText = event.getStartTime() != null ? event.getStartTime().format(DATE_FORMAT) : "TBA";

        StringBuilder ticketsHtml = new StringBuilder();
        List<Ticket> tickets = order.getTickets();
        if (tickets != null) {
            for (Ticket t : tickets) {
                String qrImg = (t.getQrCodeData() != null && !t.getQrCodeData().isBlank())
                        ? "<img src=\"data:image/png;base64," + t.getQrCodeData() + "\" width=\"150\" height=\"150\" alt=\"QR Pass\" style=\"border-radius: 8px; border: 1px solid #334155; margin: 10px 0;\" />"
                        : "<div style=\"padding: 15px; background: #1e293b; color: #94a3b8; font-size: 12px; border-radius: 6px;\">Digital Pass Code: " + t.getTicketCode() + "</div>";

                ticketsHtml.append("<div style=\"background: #0f172a; border: 1px solid #334155; border-radius: 12px; padding: 18px; margin-bottom: 16px; text-align: center;\">")
                        .append("<div style=\"font-size: 16px; font-weight: 700; color: #f8fafc;\">").append(t.getTicketTier().getName()).append("</div>")
                        .append("<div style=\"font-size: 13px; color: #94a3b8; margin: 4px 0;\">Attendee: <strong style=\"color: #e2e8f0;\">").append(t.getAttendeeName()).append("</strong></div>")
                        .append(qrImg)
                        .append("<div style=\"font-family: monospace; font-size: 14px; color: #6366f1; font-weight: 700; letter-spacing: 1px;\">").append(t.getTicketCode()).append("</div>")
                        .append("<div style=\"font-size: 11px; color: #64748b; margin-top: 6px;\">Scan at entrance for authorized admission</div>")
                        .append("</div>");
            }
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="margin: 0; padding: 24px; background-color: #030712; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #f8fafc;">
            <div style="max-width: 600px; margin: 0 auto; background-color: #111827; border: 1px solid #1f2937; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 40px rgba(0,0,0,0.8);">
                <!-- Header Banner -->
                <div style="background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%); padding: 32px 24px; text-align: center;">
                    <div style="font-size: 12px; text-transform: uppercase; letter-spacing: 2px; color: rgba(255,255,255,0.8); font-weight: 700; margin-bottom: 8px;">PulseEvent OS</div>
                    <h1 style="margin: 0; font-size: 24px; font-weight: 800; color: #ffffff;">Order & Pass Confirmed!</h1>
                    <div style="margin-top: 8px; font-size: 14px; color: #e0e7ff;">Order Reference: <strong>{{orderNumber}}</strong></div>
                </div>

                <!-- Event Details Body -->
                <div style="padding: 28px 24px;">
                    <div style="background: #1f2937; border-radius: 12px; padding: 20px; margin-bottom: 24px;">
                        <h2 style="margin: 0 0 12px 0; font-size: 20px; font-weight: 700; color: #ffffff;">{{eventTitle}}</h2>
                        <div style="display: flex; margin-bottom: 8px; font-size: 14px; color: #cbd5e1;">
                            <span style="color: #94a3b8; width: 80px;">When:</span>
                            <strong style="color: #f1f5f9;">{{dateText}}</strong>
                        </div>
                        <div style="display: flex; font-size: 14px; color: #cbd5e1;">
                            <span style="color: #94a3b8; width: 80px;">Where:</span>
                            <span style="color: #f1f5f9;">{{venueText}}</span>
                        </div>
                    </div>

                    <h3 style="margin: 0 0 16px 0; font-size: 16px; font-weight: 700; color: #cbd5e1; text-transform: uppercase; letter-spacing: 1px;">Your Digital Admission Passes</h3>
                    
                    {{ticketsHtml}}

                    <div style="background: rgba(99, 102, 241, 0.08); border: 1px dashed rgba(99, 102, 241, 0.3); border-radius: 12px; padding: 16px; text-align: center; margin-top: 24px;">
                        <div style="font-size: 13px; color: #cbd5e1;">Total Order Paid: <strong style="color: #10b981; font-size: 16px;">${{totalAmount}}</strong></div>
                        <div style="font-size: 12px; color: #94a3b8; margin-top: 4px;">Thank you for reserving your spot. View passes anytime in your Pulse Dashboard.</div>
                    </div>
                </div>

                <!-- Footer -->
                <div style="background-color: #0b0f19; padding: 20px 24px; text-align: center; border-top: 1px solid #1f2937; font-size: 12px; color: #64748b;">
                    Sent by PulseEvent OS Enterprise Engine &bull; Support: support@eventpulse.io
                </div>
            </div>
        </body>
        </html>
        """
        .replace("{{orderNumber}}", order.getOrderNumber())
        .replace("{{eventTitle}}", event.getTitle())
        .replace("{{dateText}}", dateText)
        .replace("{{venueText}}", venueText)
        .replace("{{ticketsHtml}}", ticketsHtml.toString())
        .replace("{{totalAmount}}", order.getTotalAmount() != null ? order.getTotalAmount().toPlainString() : "0.00");
    }

    private String buildEventStatusHtml(Event event, String reason) {
        String statusColor = "PUBLISHED".equals(event.getStatus().name()) ? "#10b981" : "#f59e0b";
        String reasonHtml = (reason != null && !reason.isBlank()) 
                ? "<div style=\"margin-top: 12px; padding: 12px; background: #1f2937; border-left: 3px solid #ef4444; color: #cbd5e1; font-size: 13px;\">" + reason + "</div>"
                : "";

        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="utf-8"></head>
        <body style="margin: 0; padding: 24px; background-color: #030712; font-family: sans-serif; color: #f8fafc;">
            <div style="max-width: 540px; margin: 0 auto; background-color: #111827; border: 1px solid #1f2937; border-radius: 14px; padding: 28px;">
                <h2 style="margin: 0 0 12px 0; color: #ffffff;">Event Status Update</h2>
                <p style="font-size: 14px; color: #94a3b8;">Your event <strong style="color: #f1f5f9;">"{{eventTitle}}"</strong> has been reviewed:</p>
                <div style="display: inline-block; padding: 6px 14px; background-color: {{statusColor}}; color: #ffffff; border-radius: 6px; font-weight: 700; font-size: 14px; margin: 12px 0;">
                    STATUS: {{eventStatus}}
                </div>
                {{reasonHtml}}
                <p style="font-size: 13px; color: #64748b; margin-top: 24px;">Manage your events from your Organizer Studio dashboard.</p>
            </div>
        </body>
        </html>
        """
        .replace("{{eventTitle}}", event.getTitle())
        .replace("{{statusColor}}", statusColor)
        .replace("{{eventStatus}}", event.getStatus().name())
        .replace("{{reasonHtml}}", reasonHtml);
    }
}
