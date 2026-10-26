package com.yeyamo_mobile.api.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class GatewayFallbackIntegrationTest {

    private static final String AUTH_PATH = "/api/v1/auth/login";
    private static final String AVAILABLE_PATH = "/api/v1/countries/integration-available";
    private static final String LB_COUNTRIES_PATH = "/api/v1/countries/integration-lb-available";
    private static final String SLOW_PATH = "/api/v1/countries/integration-slow";
    private static final String UNAVAILABLE_PATH = "/api/v1/countries/integration-unavailable";
    private static final String OPEN_PATH = "/api/v1/countries/integration-open";
    private static final String RECOVERY_PATH = "/api/v1/countries/integration-recovery";

    private static HttpServer downstream;
    private static ExecutorService downstreamExecutor;
    private static final AtomicInteger recoveryCalls = new AtomicInteger();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @LocalServerPort
    private int gatewayPort;

    @BeforeAll
    static void startDownstream() throws IOException {
        downstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        downstream.createContext("/", GatewayFallbackIntegrationTest::handleDownstreamRequest);
        downstreamExecutor = Executors.newCachedThreadPool();
        downstream.setExecutor(downstreamExecutor);
        downstream.start();
    }

    @AfterAll
    static void stopDownstream() {
        if (downstream != null) {
            downstream.stop(0);
        }
        if (downstreamExecutor != null) {
            downstreamExecutor.shutdownNow();
        }
    }

    @DynamicPropertySource
    static void gatewayRoutes(DynamicPropertyRegistry registry) {
        startDownstreamBeforeContext();
        registerHttpRoute(registry, 0, "integration-available", AVAILABLE_PATH, "availableCircuit");
        registerHttpRoute(registry, 1, "integration-slow", SLOW_PATH, "slowCircuit");
        registerLoadBalancerRoute(registry, 2, "integration-unavailable", UNAVAILABLE_PATH,
                "unavailableCircuit", "unavailable-service");
        registerHttpRoute(registry, 3, "integration-open", OPEN_PATH, "openCircuit");
        registerLoadBalancerRoute(registry, 4, "integration-auth-lb", AUTH_PATH, "authCircuit", "auth-service");
        registerLoadBalancerRoute(registry, 5, "integration-countries-lb", LB_COUNTRIES_PATH,
                "countriesCircuit", "country-config-service");
        registerHttpRoute(registry, 6, "integration-recovery", RECOVERY_PATH, "recoveryCircuit");
        registry.add("spring.cloud.discovery.client.simple.instances.auth-service[0].uri",
                GatewayFallbackIntegrationTest::downstreamUri);
        registry.add("spring.cloud.discovery.client.simple.instances.country-config-service[0].uri",
                GatewayFallbackIntegrationTest::downstreamUri);
        registry.add("resilience4j.timelimiter.instances.slowCircuit.timeout-duration", () -> "100ms");
        registry.add("resilience4j.timelimiter.instances.openCircuit.timeout-duration", () -> "100ms");
        registry.add("resilience4j.circuitbreaker.instances.openCircuit.minimum-number-of-calls", () -> "1");
        registry.add("resilience4j.circuitbreaker.instances.openCircuit.sliding-window-size", () -> "1");
        registry.add("resilience4j.circuitbreaker.instances.openCircuit.failure-rate-threshold", () -> "1");
        registry.add("resilience4j.circuitbreaker.instances.openCircuit.wait-duration-in-open-state", () -> "30s");
        registry.add("resilience4j.circuitbreaker.instances.recoveryCircuit.minimum-number-of-calls", () -> "1");
        registry.add("resilience4j.circuitbreaker.instances.recoveryCircuit.sliding-window-size", () -> "1");
        registry.add("resilience4j.circuitbreaker.instances.recoveryCircuit.failure-rate-threshold", () -> "1");
        registry.add("resilience4j.circuitbreaker.instances.recoveryCircuit.wait-duration-in-open-state", () -> "100ms");
        registry.add(routeKey(6, "filters[0].args.statusCodes[0]"), () -> "INTERNAL_SERVER_ERROR");
    }

    @Test
    void authRouteUsesLoadBalancerAndPreservesSecurityHeaders() throws Exception {
        HttpResponse<String> response = request(AUTH_PATH, "integration-auth", "POST");

        assertSuccessfulResponse(response, "integration-auth");
    }

    @Test
    void countriesRouteUsesLoadBalancerAndPreservesCorrelationId() throws Exception {
        HttpResponse<String> response = request(LB_COUNTRIES_PATH, "integration-countries", "GET");

        assertSuccessfulResponse(response, "integration-countries");
    }

    @Test
    void availableServiceKeepsSuccessfulResponse() throws Exception {
        HttpResponse<String> response = request(AVAILABLE_PATH, "integration-available");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("available");
    }

    @Test
    void slowServiceReachesFallbackAndDiagnosesTimeout(CapturedOutput output) throws Exception {
        HttpResponse<String> response = request(SLOW_PATH, "integration-slow");

        assertFallbackResponse(response, "integration-slow");
        assertThat(output.toString()).contains("exceptionType=TimeoutException")
                .contains("circuitState=TIME_LIMITER_TIMEOUT");
    }

    @Test
    void unavailableLoadBalancerServiceReachesFallback(CapturedOutput output) throws Exception {
        HttpResponse<String> response = request(UNAVAILABLE_PATH, "integration-unavailable");

        assertFallbackResponse(response, "integration-unavailable");
        assertThat(output.toString()).contains("service=integration-unavailable")
                .contains("exceptionType=HttpServerErrorException");
    }

    @Test
    void openCircuitReachesFallbackWithoutCallingDownstream(CapturedOutput output) throws Exception {
        HttpResponse<String> first = request(OPEN_PATH, "integration-open-first");
        HttpResponse<String> second = request(OPEN_PATH, "integration-open-second");

        assertFallbackResponse(first, "integration-open");
        assertFallbackResponse(second, "integration-open");
        assertThat(output.toString()).contains("circuitState=OPEN")
                .contains("exceptionType=CallNotPermittedException");
    }

    @Test
    void serviceRecoversAfterCircuitWaitDuration() throws Exception {
        recoveryCalls.set(0);

        HttpResponse<String> failed = request(RECOVERY_PATH, "integration-recovery-failed");
        assertFallbackResponse(failed, "integration-recovery");

        Thread.sleep(250);

        HttpResponse<String> recovered = request(RECOVERY_PATH, "integration-recovery-success");
        assertSuccessfulResponse(recovered, "integration-recovery-success");
    }

    private HttpResponse<String> request(String path, String correlationId) throws Exception {
        return request(path, correlationId, "GET");
    }

    private HttpResponse<String> request(String path, String correlationId, String method) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + gatewayPort + path))
                .header("X-Correlation-ID", correlationId)
                .header("Origin", "http://localhost:3000")
                .timeout(Duration.ofSeconds(5));
        if ("POST".equals(method)) {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }
        else {
            builder.GET();
        }
        HttpRequest request = builder.build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void assertSuccessfulResponse(HttpResponse<String> response, String correlationId) {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("available");
        assertThat(response.headers().firstValue("X-Correlation-ID")).hasValue(correlationId);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
                .contains("http://localhost:3000");
    }

    private void assertFallbackResponse(HttpResponse<String> response, String service) {
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).contains("\"code\":\"SERVICE_UNAVAILABLE\"")
                .contains("\"service\":\"" + service + "\"")
                .contains("\"correlationId\"");
    }

    private static void registerHttpRoute(DynamicPropertyRegistry registry, int index, String routeId,
            String path, String circuitId) {
        registerRoute(registry, index, routeId, path, circuitId);
        registry.add(routeKey(index, "uri"), GatewayFallbackIntegrationTest::downstreamUri);
    }

    private static void registerLoadBalancerRoute(DynamicPropertyRegistry registry, int index, String routeId,
            String path, String circuitId, String serviceId) {
        registerRoute(registry, index, routeId, path, circuitId);
        registry.add(routeKey(index, "uri"), () -> "lb://" + serviceId);
    }

    private static void registerRoute(DynamicPropertyRegistry registry, int index, String routeId, String path,
            String circuitId) {
        registry.add(routeKey(index, "id"), () -> routeId);
        registry.add(routeKey(index, "predicates[0]"), () -> "Path=" + path);
        registry.add(routeKey(index, "filters[0].name"), () -> "CircuitBreaker");
        registry.add(routeKey(index, "filters[0].args.id"), () -> circuitId);
        registry.add(routeKey(index, "filters[0].args.fallbackPath"), () -> "/fallback/" + routeId);
    }

    private static String routeKey(int index, String suffix) {
        return "spring.cloud.gateway.server.webmvc.routes[" + index + "]." + suffix;
    }

    private static String downstreamUri() {
        return "http://127.0.0.1:" + downstream.getAddress().getPort();
    }

    private static void startDownstreamBeforeContext() {
        if (downstream != null) {
            return;
        }
        try {
            startDownstream();
        }
        catch (IOException exception) {
            throw new IllegalStateException("Unable to start downstream test server", exception);
        }
    }

    private static void handleDownstreamRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.contains("integration-recovery") && recoveryCalls.getAndIncrement() == 0) {
            byte[] response = "temporarily unavailable".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, response.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(response);
            }
            return;
        }
        if (path.contains("integration-slow") || path.contains("integration-open")) {
            try {
                Thread.sleep(400);
            }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] response = "available".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream body = exchange.getResponseBody()) {
            body.write(response);
        }
    }
}
