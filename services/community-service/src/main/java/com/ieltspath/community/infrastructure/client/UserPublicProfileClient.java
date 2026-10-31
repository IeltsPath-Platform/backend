package com.ieltspath.community.infrastructure.client;

import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.application.port.StreakLookup;
import com.ieltspath.community.application.result.PublicProfile;
import com.ieltspath.community.domain.exception.CommunityUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Public profiles come from the User Service. The streak shown beside each one is Learning's, fetched once for the
 * whole batch; when Learning cannot answer the profiles are still returned, without a streak.
 */
@Component
public class UserPublicProfileClient implements PublicProfileClient {
    private final RestClient client;
    private final StreakLookup streaks;

    @Autowired
    public UserPublicProfileClient(RestClient.Builder builder,
            @Value("${community.user-service.base-url:http://localhost:8085}") String baseUrl, StreakLookup streaks) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        client = builder.baseUrl(baseUrl).requestFactory(factory).build();
        this.streaks = streaks;
    }

    UserPublicProfileClient(RestClient client, StreakLookup streaks) {
        this.client = client;
        this.streaks = streaks;
    }

    @Override
    public Map<UUID, PublicProfile> findProfiles(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        if (ids.size() > 100) throw new IllegalArgumentException("At most 100 public profiles per batch");
        try {
            var response = client.post().uri("/internal/users/public-profiles").headers(ForwardedRequestHeaders::copy)
                    .body(Map.of("userIds", ids)).retrieve().body(Batch.class);
            Map<UUID, PublicProfile> result = new HashMap<>();
            if (response != null && response.profiles() != null)
                response.profiles().forEach(profile -> { if (ids.contains(profile.id())) result.put(profile.id(), profile); });
            return withStreaks(result.values()).stream().collect(Collectors.toMap(PublicProfile::id, profile -> profile));
        } catch (org.springframework.web.client.RestClientException exception) {
            throw new CommunityUnavailableException("Public profiles are temporarily unavailable");
        }
    }

    @Override
    public List<PublicProfile> searchProfiles(UUID viewerId, String query, int limit) {
        try {
            var response = client.get().uri(uri -> uri.path("/internal/users/public-profiles/search")
                    .queryParam("q", query).queryParam("limit", limit).build())
                    .headers(ForwardedRequestHeaders::copy).retrieve().body(Batch.class);
            return response == null || response.profiles() == null ? List.of() : withStreaks(response.profiles());
        } catch (org.springframework.web.client.RestClientException exception) {
            throw new CommunityUnavailableException("Public profiles are temporarily unavailable");
        }
    }

    /** One call to Learning for the batch. A learner Learning has no streak for shows 0; no answer hides the streak. */
    private List<PublicProfile> withStreaks(java.util.Collection<PublicProfile> profiles) {
        if (profiles.isEmpty()) return List.of();
        Set<UUID> ids = profiles.stream().map(PublicProfile::id).collect(Collectors.toSet());
        try {
            Map<UUID, Integer> found = streaks.streaks(ids);
            return profiles.stream().map(profile -> profile.withStreak(found.getOrDefault(profile.id(), 0))).toList();
        } catch (CommunityUnavailableException unavailable) {
            return profiles.stream().map(profile -> profile.withStreak(null)).toList();
        }
    }

    public record Batch(List<PublicProfile> profiles) { }
}
