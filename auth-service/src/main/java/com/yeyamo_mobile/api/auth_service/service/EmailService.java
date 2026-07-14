package com.yeyamo_mobile.api.auth_service.service;

import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmailVerificationOtp(String email, String otp, int expirationMinutes) {
        send(email, "Verification de votre email YeYamo",
                "Votre code de verification YeYamo est : " + otp + "\n\nCe code expire dans "
                        + expirationMinutes + " minutes.");
    }

    public void sendPasswordResetOtp(String email, String otp, int expirationMinutes) {
        send(email, "Reinitialisation de votre mot de passe YeYamo",
                "Votre code de reinitialisation YeYamo est : " + otp + "\n\nCe code expire dans "
                        + expirationMinutes + " minutes.");
    }

    private void send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (MailException exception) {
            throw new ApiException("EMAIL_SEND_FAILED", "Impossible d'envoyer l'email", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
