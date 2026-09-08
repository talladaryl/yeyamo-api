package com.yeyamo_mobile.api.auth_service.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.yeyamo_mobile.api.auth_service.exception.ApiException;

class CloudflareTurnstileVerifierTests {
    private static final URI VERIFY_URL = URI.create("https://challenges.cloudflare.test/siteverify");

    @Test
    void acceptsSuccessWithExpectedHostnameAndAction() {
        Fixture fixture = fixture(true);
        fixture.server.expect(once(), requestTo(VERIFY_URL)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(response(true, "yeyamo.com", "register", ""), MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> fixture.verifier.verify("one-use-token", AntiBotAction.REGISTER, null));
        fixture.server.verify();
    }

    @Test
    void rejectsFailedVerificationWithTheStableClientCode() {
        Fixture invalid = fixture(true);
        invalid.server.expect(once(), requestTo(VERIFY_URL))
                .andRespond(withSuccess(response(false, "", "", "invalid-input-response"), MediaType.APPLICATION_JSON));
        assertCode("TURNSTILE_VERIFICATION_FAILED", () -> invalid.verifier.verify("invalid", AntiBotAction.REGISTER, null));

        Fixture expired = fixture(true);
        expired.server.expect(once(), requestTo(VERIFY_URL))
                .andRespond(withSuccess(response(false, "", "", "timeout-or-duplicate"), MediaType.APPLICATION_JSON));
        assertCode("TURNSTILE_VERIFICATION_FAILED", () -> expired.verifier.verify("expired", AntiBotAction.REGISTER, null));
    }

    @Test
    void rejectsWrongHostnameAndAction() {
        Fixture hostname = fixture(true);
        hostname.server.expect(once(), requestTo(VERIFY_URL))
                .andRespond(withSuccess(response(true, "attacker.test", "register", ""), MediaType.APPLICATION_JSON));
        assertCode("TURNSTILE_HOSTNAME_MISMATCH",
                () -> hostname.verifier.verify("token", AntiBotAction.REGISTER, null));

        Fixture action = fixture(true);
        action.server.expect(once(), requestTo(VERIFY_URL))
                .andRespond(withSuccess(response(true, "yeyamo.com", "forgot_password", ""), MediaType.APPLICATION_JSON));
        assertCode("TURNSTILE_ACTION_MISMATCH",
                () -> action.verifier.verify("token", AntiBotAction.REGISTER, null));
    }

    @Test
    void failsOpenWhenProviderIsTechnicallyUnavailable() {
        Fixture fixture = fixture(true);
        fixture.server.expect(once(), requestTo(VERIFY_URL)).andRespond(withServerError());
        assertDoesNotThrow(() -> fixture.verifier.verify("token", AntiBotAction.REGISTER, null));
    }

    @Test
    void bypassesProviderOnlyWhenExplicitlyDisabled() {
        Fixture fixture = fixture(false);
        assertDoesNotThrow(() -> fixture.verifier.verify(null, AntiBotAction.REGISTER, null));
        fixture.server.verify();
    }

    private Fixture fixture(boolean enabled) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TurnstileProperties properties = new TurnstileProperties(
                enabled, "test-secret", VERIFY_URL, "yeyamo.com", Duration.ofSeconds(1));
        return new Fixture(new CloudflareTurnstileVerifier(builder.build(), properties), server);
    }

    private String response(boolean success, String hostname, String action, String error) {
        String errors = error.isBlank() ? "[]" : "[\"" + error + "\"]";
        return "{\"success\":" + success + ",\"hostname\":\"" + hostname
                + "\",\"action\":\"" + action + "\",\"error-codes\":" + errors + "}";
    }

    private void assertCode(String code, Runnable operation) {
        assertEquals(code, assertThrows(ApiException.class, operation::run).getCode());
    }

    private record Fixture(CloudflareTurnstileVerifier verifier, MockRestServiceServer server) {
    }
}
