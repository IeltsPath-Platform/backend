package com.ieltspath.community.infrastructure.client;

import com.ieltspath.community.application.port.StreakLookup;
import com.ieltspath.community.domain.exception.CommunityUnavailableException;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
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
    private final ResilientCall resilientCall;

    @Autowired
    public LearningStreakClient(RestClient.Builder builder,
            @Value("${community.learning-service.base-url:http://localhost:8086}") String baseUrl,
            ResilientCall resilientCall) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        client = builder.baseUrl(baseUrl).requestFactory(factory).build();
        this.resilientCall = resilientCall;
    }

    LearningStreakClient(RestClient client) {
        this(client, defaultResilientCall());
    }

    LearningStreakClient(RestClient client, ResilientCall resilientCall) {
        this.client = client;
        this.resilientCall = resilientCall;
    }

    @Override
    public Map<UUID, Integer> streaks(Set<UUID> userIds) {
        Map<UUID, Integer> result = new HashMap<>();
        List<UUID> ids = new ArrayList<>(userIds);
        for (int from = 0; from < ids.size(); from += BATCH) {
            List<UUID> batch = ids.subList(from, Math.min(from + BATCH, ids.size()));
            try {
                var response = resilientCall.call("learning", CallKind.READ, () -> client.post()
                        .uri("/internal/learning/streaks")
                        .headers(ForwardedRequestHeaders::copy).body(Map.of("userIds", batch))
                        .retrieve().body(Answer.class));
                if (response != null && response.streaks() != null) {
                    response.streaks().forEach(item -> result.put(item.userId(), item.currentDays()));
                }
            } catch (DownstreamUnavailableException exception) {
                throw new CommunityUnavailableException("Streaks are temporarily unavailable");
            } catch (RestClientException exception) {
                throw new CommunityUnavailableException("Streaks are temporarily unavailable");
            }
        }
        return result;
    }

    record Answer(List<Item> streaks) {}

    record Item(UUID userId, int currentDays) {}

    private static ResilientCall defaultResilientCall() {
        return ResilientCall.isolatedDefault();
    }
}
