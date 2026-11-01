package com.ieltspath.community.infrastructure.client;

import com.ieltspath.community.application.port.StreakLookup;
import com.ieltspath.community.domain.exception.CommunityUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Reads learners' streaks from the Learning Service, at most one call per hundred learners. */
@Component
public class LearningStreakClient implements StreakLookup {
    /** The most learners Learning answers for in one call. */
    private static final int BATCH = 100;

    private final RestClient client;

    @Autowired
    public LearningStreakClient(RestClient.Builder builder,
            @Value("${community.learning-service.base-url:http://localhost:8086}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        client = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    LearningStreakClient(RestClient client) {
        this.client = client;
    }

    @Override
    public Map<UUID, Integer> streaks(Set<UUID> userIds) {
        Map<UUID, Integer> result = new HashMap<>();
        List<UUID> ids = new ArrayList<>(userIds);
        for (int from = 0; from < ids.size(); from += BATCH) {
            List<UUID> batch = ids.subList(from, Math.min(from + BATCH, ids.size()));
            try {
                var response = client.post().uri("/internal/learning/streaks")
                        .headers(ForwardedRequestHeaders::copy).body(Map.of("userIds", batch))
                        .retrieve().body(Answer.class);
                if (response != null && response.streaks() != null) {
                    response.streaks().forEach(item -> result.put(item.userId(), item.currentDays()));
                }
            } catch (RestClientException exception) {
                throw new CommunityUnavailableException("Streaks are temporarily unavailable");
            }
        }
        return result;
    }

    record Answer(List<Item> streaks) {}

    record Item(UUID userId, int currentDays) {}
}
