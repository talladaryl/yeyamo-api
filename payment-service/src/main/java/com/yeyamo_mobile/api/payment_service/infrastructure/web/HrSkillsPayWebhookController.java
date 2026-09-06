package com.yeyamo_mobile.api.payment_service.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.yeyamo_mobile.api.payment_service.domain.PaymentException;
import com.yeyamo_mobile.api.payment_service.infrastructure.aggregator.HrSkillsPayService;

@RestController
@RequestMapping("/api/webhook/payment")
public class HrSkillsPayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(HrSkillsPayWebhookController.class);
    private final HrSkillsPayService service;

    public HrSkillsPayWebhookController(HrSkillsPayService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void receiveWebhook(
            @RequestHeader(value = "X-Hub-Signature", required = false) String hubSignature,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody(required = false) byte[] rawBody) {
        String effectiveSignature = hubSignature != null && !hubSignature.isBlank() ? hubSignature : signature;
        if (effectiveSignature == null || effectiveSignature.isBlank() || rawBody == null || rawBody.length == 0) {
            log.warn("Webhook rejected: missing signature or empty body");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing signature or body");
        }

        try {
            service.processWebhook(rawBody, effectiveSignature);
        } catch (PaymentException ex) {
            if ("INVALID_WEBHOOK_SIGNATURE".equals(ex.getCode())) {
                log.warn("Webhook rejected: invalid HMAC signature");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signature");
            }
            throw ex;
        } catch (Exception ex) {
            log.error("Webhook processing error: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Processing error");
        }
    }
}
