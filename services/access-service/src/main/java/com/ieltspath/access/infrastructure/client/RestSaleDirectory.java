package com.ieltspath.access.infrastructure.client;

import com.ieltspath.access.application.port.SaleDirectory;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.ResilientCall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Reads the sales accounts with {@code GET /internal/users/sales-staff}, forwarding the caller's bearer and correlation id.
 * The call runs outside any transaction; when the user service cannot answer, {@code DownstreamUnavailableException}
 * reaches the handler, which turns it into a retryable 503. Nothing is cached: a sale locked a moment ago must not be
 * offered as a recipient.
 */
@Component
public class RestSaleDirectory implements SaleDirectory {

    private final RestClient restClient;
    private final ResilientCall resilientCall;

    @Autowired
    public RestSaleDirectory(RestClient.Builder builder,
                             @Value("${access.user.base-url:http://localhost:8085}") String userBaseUrl,
                             ResilientCall resilientCall) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = builder.requestFactory(requestFactory).baseUrl(userBaseUrl).build();
        this.resilientCall = resilientCall;
    }

    RestSaleDirectory(RestClient restClient, ResilientCall resilientCall) {
        this.restClient = restClient;
        this.resilientCall = resilientCall;
    }

    @Override
    public List<SaleAccount> sales() {
        String bearer = GatewayRequestContext.bearerToken();
        String correlationId = GatewayRequestContext.correlationId();
        Item[] items = resilientCall.call("user", CallKind.READ, () -> restClient.get()
                .uri("/internal/users/sales-staff")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .header("X-Correlation-Id", correlationId)
                .retrieve().body(Item[].class));
        if (items == null) return List.of();
        return java.util.Arrays.stream(items)
                .filter(item -> item.id() != null)
                .map(item -> new SaleAccount(item.id(), item.displayName() == null ? "" : item.displayName(), item.active()))
                .toList();
    }

    private record Item(UUID id, String displayName, boolean active) {}
}
