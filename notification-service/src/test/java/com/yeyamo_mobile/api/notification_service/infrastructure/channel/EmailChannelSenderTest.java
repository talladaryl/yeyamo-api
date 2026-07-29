package com.yeyamo_mobile.api.notification_service.infrastructure.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmailChannelSenderTest {
    @Test
    void masksRecipient() {
        assertThat(EmailChannelSender.mask("target@example.com")).isEqualTo("ta***@example.com");
    }

    @Test
    void requiresSenderWhenDeliveryIsEnabled() {
        assertThatThrownBy(() -> new EmailSenderProperties(true, null, "YeYamo"))
                .isInstanceOf(IllegalStateException.class);
    }
}
