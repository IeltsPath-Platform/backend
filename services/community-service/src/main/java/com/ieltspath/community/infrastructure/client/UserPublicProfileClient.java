package com.ieltspath.community.infrastructure.client;

import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.application.port.StreakLookup;
import com.ieltspath.community.application.result.PublicProfile;
import com.ieltspath.community.domain.exception.CommunityUnavailableException;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
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
    private final ResilientCall resilientCall;

    @Autowired
    public UserPublicProfileClient(RestClient.Builder builder,
            @Value("${community.user-service.base-url:http://localhost:8085}") String baseUrl, StreakLookup streaks,
            ResilientCall resilientCall) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        client = builder.baseUrl(baseUrl).requestFactory(factory).build();
        this.streaks = streaks;
        this.resilientCall = resilientCall;
    }

    UserPublicProfileClient(RestClient client, StreakLookup streaks) {
        this(client, streaks, defaultResilientCall());
    }

    UserPublicProfileClient(RestClient client, StreakLookup streaks, ResilientCall resilientCall) {
        this.client = client;
        this.streaks = streaks;
        this.resilientCall = resilientCall;
    }

    @Override
    public Map<UUID, PublicProfile> findProfiles(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        if (ids.size() > 100) throw new IllegalArgumentException("At most 100 public profiles per batch");
        UUID deletedId = com.ieltspath.community.infrastructure.persistence.JdbcDeletedUserStore.DELETED_AUTHOR_ID;
        boolean hasDeleted = ids.contains(deletedId);
        Set<UUID> lookupIds = hasDeleted
                ? ids.stream().filter(id -> !deletedId.equals(id)).collect(Collectors.toSet())
                : ids;
        Map<UUID, PublicProfile> result = new HashMap<>();
        if (hasDeleted) {
            result.put(deletedId, PublicProfile.deleted(deletedId));
        }
        if (lookupIds.isEmpty()) {
            return result;
        }
        try {
            var response = resilientCall.call("user", CallKind.READ, () -> client.post()
                    .uri("/internal/users/public-profiles").headers(ForwardedRequestHeaders::copy)
                    .body(Map.of("userIds", lookupIds)).retrieve().body(Batch.class));
            if (response != null && response.profiles() != null)
                response.profiles().forEach(profile -> { if (lookupIds.contains(profile.id())) result.put(profile.id(), profile); });
            Map<UUID, PublicProfile> resolved = new HashMap<>();
            if (hasDeleted) {
                resolved.put(deletedId, PublicProfile.deleted(deletedId));
            }
            withStreaks(result.values().stream().filter(p -> !deletedId.equals(p.id())).toList())
                    .forEach(profile -> resolved.put(profile.id(), profile));
            return resolved;
        } catch (DownstreamUnavailableException exception) {
            throw new CommunityUnavailableException("Hồ sơ công khai tạm thời không tải được. Vui lòng thử lại sau.");
        } catch (org.springframework.web.client.RestClientException exception) {
            throw new CommunityUnavailableException("Hồ sơ công khai tạm thời không tải được. Vui lòng thử lại sau.");
        }
    }

    @Override
    public List<PublicProfile> searchProfiles(UUID viewerId, String query, int limit) {
        try {
            var response = resilientCall.call("user", CallKind.READ, () -> client.get().uri(uri -> uri.path("/internal/users/public-profiles/search")
                    .queryParam("q", query).queryParam("limit", limit).build())
                    .headers(ForwardedRequestHeaders::copy).retrieve().body(Batch.class));
            return response == null || response.profiles() == null ? List.of() : withStreaks(response.profiles());
        } catch (DownstreamUnavailableException exception) {
            throw new CommunityUnavailableException("Hồ sơ công khai tạm thời không tải được. Vui lòng thử lại sau.");
        } catch (org.springframework.web.client.RestClientException exception) {
            throw new CommunityUnavailableException("Hồ sơ công khai tạm thời không tải được. Vui lòng thử lại sau.");
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

    private static ResilientCall defaultResilientCall() {
        return ResilientCall.isolatedDefault();
    }
}
