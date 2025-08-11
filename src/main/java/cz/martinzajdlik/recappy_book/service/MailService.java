package cz.martinzajdlik.recappy_book.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {
    private final JavaMailSender sender;

    @Value("${app.mail.from}")
    private String from;

    public MailService(JavaMailSender sender) {
        this.sender = sender;
    }

    public void send(String to, String subject, String html) {
        try {
            MimeMessage msg = sender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
            h.setFrom(from);
            h.setTo(to);
            h.setSubject(subject);
            h.setText(html, true);
            sender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("Sending mail failed", e);
        }
    }
}
