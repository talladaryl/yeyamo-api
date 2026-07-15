package com.yeyamo_mobile.api.ingestion_service.application.source;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.yeyamo_mobile.api.ingestion_service.domain.model.ImportJob;
import com.yeyamo_mobile.api.ingestion_service.domain.model.SourceType;

@Component
public class ApiSourceStrategy implements IngestionSourceStrategy {
    private final RestClient client;
    private final JsonSourceStrategy json;
    private final Set<String> allowedHosts;
    private final int maximumResponseBytes;
    private final boolean allowHttp;
    private final boolean allowPrivateAddresses;

    public ApiSourceStrategy(JsonSourceStrategy json,
            @Value("${ingestion.api.allowed-hosts:}") String hosts,
            @Value("${ingestion.api.max-response-bytes:5000000}") int maximumResponseBytes,
            @Value("${ingestion.api.allow-http:false}") boolean allowHttp,
            @Value("${ingestion.api.allow-private-addresses:false}") boolean allowPrivateAddresses,
            @Value("${ingestion.api.connect-timeout:5s}") Duration connectTimeout,
            @Value("${ingestion.api.read-timeout:15s}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.client = RestClient.builder().requestFactory(requestFactory).build();
        this.json = json;
        this.maximumResponseBytes = maximumResponseBytes;
        this.allowHttp = allowHttp;
        this.allowPrivateAddresses = allowPrivateAddresses;
        this.allowedHosts = new HashSet<>();
        Arrays.stream(hosts.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .forEach(allowedHosts::add);
        if (maximumResponseBytes < 1) {
            throw new IllegalArgumentException("ingestion.api.max-response-bytes must be positive");
        }
    }

    @Override
    public boolean supports(SourceType type) {
        return type == SourceType.API;
    }

    @Override
    public List<RawRecord> extract(ImportJob job) {
        URI uri = validate(job.getSourceReference());
        byte[] body = client.get().uri(uri).exchange((request, response) -> {
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException("API source returned a non-success status");
            }
            byte[] bytes = response.getBody().readNBytes(maximumResponseBytes + 1);
            if (bytes.length > maximumResponseBytes) {
                throw new IllegalArgumentException("API payload exceeds the configured limit");
            }
            return bytes;
        });
        if (body == null || body.length == 0) {
            throw new IllegalArgumentException("API returned an empty payload");
        }
        return json.parse(new String(body, StandardCharsets.UTF_8));
    }

    private URI validate(String reference) {
        URI uri;
        try {
            uri = URI.create(reference);
        } catch (RuntimeException malformed) {
            throw new IllegalArgumentException("Invalid API source URL");
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (host == null || uri.getUserInfo() != null || uri.getFragment() != null
                || !("https".equalsIgnoreCase(scheme) || allowHttp && "http".equalsIgnoreCase(scheme))
                || !allowedHosts.contains(host.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("API source URL is not allowed");
        }
        if (!allowPrivateAddresses) {
            try {
                for (InetAddress address : InetAddress.getAllByName(host)) {
                    if (nonPublic(address)) {
                        throw new IllegalArgumentException("API source resolves to a non-public address");
                    }
                }
            } catch (java.net.UnknownHostException failure) {
                throw new IllegalArgumentException("API source host cannot be resolved");
            }
        }
        return uri;
    }

    private boolean nonPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            return first == 0 || first == 10 || first == 127 || first >= 224
                    || first == 169 && second == 254
                    || first == 172 && second >= 16 && second <= 31
                    || first == 192 && second == 168
                    || first == 100 && second >= 64 && second <= 127
                    || first == 198 && (second == 18 || second == 19);
        }
        return (bytes[0] & 0xfe) == 0xfc;
    }
}
