package com.yeyamo_mobile.api.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

class EmailServiceTests {

    @Test
    void buildsPasswordResetEmailWithoutHardcodedExpiration() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(message);
        EmailService service = new EmailService(sender, properties(true));

        service.sendPasswordResetOtp("target@example.com", "123456", 7);

        verify(sender).send(message);
        message.saveChanges();
        assertThat(message.getSubject()).isEqualTo("Réinitialisation de votre mot de passe YeYamo");
        assertThat(message.getContent().toString()).contains("123456", "7 minutes", "Ne partagez jamais ce code");
        assertThat(message.getFrom()[0].toString()).contains("YeYamo", "sender@example.com");
    }

    @Test
    void mapsSmtpFailureToPublicApplicationError() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("535 secret provider detail")).when(sender).send(any(MimeMessage.class));
        EmailService service = new EmailService(sender, properties(true));

        assertThatThrownBy(() -> service.sendEmailVerificationOtp("target@example.com", "123456", 10))
                .isInstanceOfSatisfying(ApiException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("EMAIL_DELIVERY_FAILED");
                    assertThat(error.getMessage()).doesNotContain("535", "secret");
                });
    }

    @Test
    void masksRecipientAndRequiresSenderWhenEnabled() {
        assertThat(EmailService.mask("target@example.com")).isEqualTo("ta***@example.com");
        assertThatThrownBy(() -> new EmailSenderProperties(true, "", "YeYamo"))
                .isInstanceOf(IllegalStateException.class);
    }

    private EmailSenderProperties properties(boolean enabled) {
        return new EmailSenderProperties(enabled, "sender@example.com", "YeYamo");
    }
}
