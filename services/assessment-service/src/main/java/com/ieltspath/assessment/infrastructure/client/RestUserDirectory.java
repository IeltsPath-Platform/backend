package com.ieltspath.assessment.infrastructure.client;

import com.ieltspath.assessment.application.port.UserDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads display names with {@code POST /internal/users/display-names}, using the caller's bearer and correlation id.
 * The endpoint takes at most 50 ids, so a longer list is sent in batches. Any failure means no names.
 */
@Component
public class RestUserDirectory implements UserDirectory {
    private static final Logger log = LoggerFactory.getLogger(RestUserDirectory.class);
    static final int BATCH_SIZE = 50;

    private final RestClient restClient;

    @Autowired
    public RestUserDirectory(RestClient.Builder builder,
                             @Value("${assessment.user.base-url:http://localhost:8085}") String userBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = builder.requestFactory(requestFactory).baseUrl(userBaseUrl).build();
    }

    RestUserDirectory(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Map<UUID, String> displayNames(Collection<UUID> userIds) {
        Map<UUID, String> names = new HashMap<>();
        List<UUID> ids = new ArrayList<>(new LinkedHashSet<>(userIds));
        if (ids.isEmpty()) return names;
        try {
            String bearer = GatewayRequestContext.bearerToken();
            String correlationId = GatewayRequestContext.correlationId();
            for (int from = 0; from < ids.size(); from += BATCH_SIZE) {
                List<UUID> batch = ids.subList(from, Math.min(ids.size(), from + BATCH_SIZE));
                Response response = restClient.post().uri("/internal/users/display-names")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .header("X-Correlation-Id", correlationId)
                        .body(new Request(List.copyOf(batch))).retrieve().body(Response.class);
                if (response != null && response.items() != null) {
                    for (Item item : response.items()) {
                        if (item.id() != null && item.displayName() != null) names.put(item.id(), item.displayName());
                    }
                }
            }
        } catch (RuntimeException failure) {
            log.warn("Display names unavailable: {}", failure.getClass().getSimpleName());
            return new HashMap<>();
        }
        return names;
    }

    private record Request(List<UUID> ids) {}
    private record Item(UUID id, String displayName) {}
    private record Response(List<Item> items) {}
}
