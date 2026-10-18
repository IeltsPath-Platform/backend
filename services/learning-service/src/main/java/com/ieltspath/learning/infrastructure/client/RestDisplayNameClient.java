package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.learning.application.port.DisplayNameClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Reads display names from the User Service with the caller's bearer token; any failure means no names. */
@Component
public class RestDisplayNameClient implements DisplayNameClient {
    private static final Logger log = LoggerFactory.getLogger(RestDisplayNameClient.class);
    private final RestClient restClient;

    @Autowired
    public RestDisplayNameClient(RestClient.Builder builder,
                                 @Value("${learning.user.base-url:http://localhost:8081}") String userBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        restClient = builder.requestFactory(requestFactory).baseUrl(userBaseUrl).build();
    }

    RestDisplayNameClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Map<UUID, String> names(Collection<UUID> userIds) {
        Map<UUID, String> names = new HashMap<>();
        if (userIds.isEmpty()) return names;
        try {
            Optional<String> bearer = GatewayRequestHeaders.bearerToken();
            if (bearer.isEmpty()) return names;
            Response response = restClient.post().uri("/internal/users/display-names")
                    .headers((HttpHeaders headers) -> {
                        headers.setBearerAuth(bearer.get());
                        headers.set(SecurityHeaders.CORRELATION_ID, GatewayRequestHeaders.correlationId());
                    }).body(new Request(List.copyOf(userIds))).retrieve().body(Response.class);
            if (response != null && response.items() != null) {
                for (Item item : response.items()) {
                    if (item.id() != null && item.displayName() != null) names.put(item.id(), item.displayName());
                }
            }
        } catch (RuntimeException failure) {
            log.warn("Display names unavailable", failure);
        }
        return names;
    }

    private record Request(List<UUID> ids) {}
    private record Item(UUID id, String displayName) {}
    private record Response(List<Item> items) {}
}
