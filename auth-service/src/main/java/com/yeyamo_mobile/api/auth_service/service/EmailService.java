package com.yeyamo_mobile.api.auth_service.service;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailSenderProperties properties;

    public EmailService(JavaMailSender mailSender, EmailSenderProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendEmailVerificationOtp(String email, String otp, int expirationMinutes) {
        send(email, "Votre code de vérification YeYamo", otpTemplate(otp, expirationMinutes, false));
    }

    public void sendPasswordResetOtp(String email, String otp, int expirationMinutes) {
        send(email, "Réinitialisation de votre mot de passe YeYamo", otpTemplate(otp, expirationMinutes, true));
    }

    private void send(String to, String subject, String html) {
        if (!properties.deliveryEnabled()) {
            log.info("Email delivery disabled. recipient={} subject={}", mask(to), subject);
            return;
        }
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(properties.fromAddress(), properties.fromName()));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MailException | MessagingException exception) {
            log.error("Email delivery failed. recipient={} errorType={}", mask(to), exception.getClass().getSimpleName());
            throw new ApiException("EMAIL_DELIVERY_FAILED",
                    "Impossible d'envoyer l'email pour le moment. Veuillez réessayer.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        } catch (java.io.UnsupportedEncodingException exception) {
            throw new IllegalStateException("Invalid email sender name encoding", exception);
        }
    }

    static String mask(String email) {
        if (email == null || email.isBlank()) return "***";
        int separator = email.indexOf('@');
        if (separator <= 1) return "***" + (separator >= 0 ? email.substring(separator) : "");
        return email.substring(0, 2) + "***" + email.substring(separator);
    }

    private String otpTemplate(String otp, int expirationMinutes, boolean passwordReset) {
        String purpose = passwordReset ? "réinitialisation de mot de passe" : "vérification";
        return """
                <!doctype html><html lang="fr"><body style="font-family:Arial,sans-serif;color:#172033">
                <div style="max-width:560px;margin:auto;padding:32px;border:1px solid #e5e7eb;border-radius:12px">
                <h1 style="color:#ff6b35">YeYamo</h1><p>Bonjour,</p>
                <p>Votre code de %s YeYamo est :</p>
                <p style="font-size:32px;font-weight:700;letter-spacing:8px">%s</p>
                <p>Ce code expire dans %d minutes.</p>
                <p><strong>Ne partagez jamais ce code.</strong></p>
                <p>Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.</p>
                <p>L'équipe YeYamo</p></div></body></html>
                """.formatted(purpose, otp, expirationMinutes);
    }
}
