package com.project.ewastecs.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:noreply@ewastecs.in}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an HTML email reply to a contact form submission.
     * If mail is disabled (app.mail.enabled=false), logs instead.
     */
    public void sendContactReply(String toEmail, String toName,
                                  String subject, String bodyHtml) {
        if (!mailEnabled) {
            System.out.println("[EMAIL DISABLED] Would send to: " + toEmail);
            System.out.println("[SUBJECT] " + subject);
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail, "E-Waste Recycling Manager Support");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(bodyHtml, true);
            mailSender.send(msg);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            System.err.println("Email send failed: " + e.getMessage());
        }
    }
}
