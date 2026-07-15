package com.yeyamo_mobile.api.payment_service.infrastructure.web;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yeyamo_mobile.api.payment_service.infrastructure.webhook.PaymentWebhookService;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class WebhookController {
    private final PaymentWebhookService service;

    public WebhookController(PaymentWebhookService service) {
        this.service = service;
    }

    @PostMapping("/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void receive(
            @PathVariable @Pattern(regexp = "[a-z0-9_-]{2,32}") String provider,
            @RequestHeader("X-Payment-Timestamp") @Pattern(regexp = "[0-9]{10,13}") String timestamp,
            @RequestHeader("X-Payment-Signature") @Pattern(regexp = "[a-fA-F0-9]{64}") String signature,
            @RequestBody @Size(min = 2, max = 262_144) String payload) throws Exception {
        service.process(provider, timestamp, signature, payload);
    }
}
