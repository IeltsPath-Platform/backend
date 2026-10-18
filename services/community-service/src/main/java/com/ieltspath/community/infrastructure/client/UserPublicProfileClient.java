package com.ieltspath.community.infrastructure.client;

import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.application.result.PublicProfile;
import com.ieltspath.community.domain.exception.CommunityUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class UserPublicProfileClient implements PublicProfileClient {
    private final RestClient client;

    public UserPublicProfileClient(RestClient.Builder builder,
            @Value("${community.user-service.base-url:http://localhost:8085}") String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);
        client = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public Map<UUID, PublicProfile> findProfiles(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        if (ids.size() > 100) throw new IllegalArgumentException("At most 100 public profiles per batch");
        try {
            var response = client.post().uri("/internal/users/public-profiles").headers(this::forwardRequestHeaders)
                    .body(Map.of("userIds", ids)).retrieve().body(Batch.class);
            Map<UUID, PublicProfile> result = new HashMap<>();
            if (response != null && response.profiles() != null)
                response.profiles().forEach(profile -> { if (ids.contains(profile.id())) result.put(profile.id(), profile); });
            return result;
        } catch (org.springframework.web.client.RestClientException exception) {
            throw new CommunityUnavailableException("Public profiles are temporarily unavailable");
        }
    }

    @Override
    public List<PublicProfile> searchProfiles(UUID viewerId, String query, int limit) {
        try {
            var response = client.get().uri(uri -> uri.path("/internal/users/public-profiles/search")
                    .queryParam("q", query).queryParam("limit", limit).build())
                    .headers(this::forwardRequestHeaders).retrieve().body(Batch.class);
            return response == null || response.profiles() == null ? List.of() : response.profiles();
        } catch (org.springframework.web.client.RestClientException exception) {
            throw new CommunityUnavailableException("Public profiles are temporarily unavailable");
        }
    }

    private void forwardRequestHeaders(HttpHeaders headers) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            var request = attributes.getRequest();
            var bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
            var correlation = request.getHeader("X-Correlation-Id");
            if (bearer != null) headers.set(HttpHeaders.AUTHORIZATION, bearer);
            if (correlation != null) headers.set("X-Correlation-Id", correlation);
        }
    }

    public record Batch(List<PublicProfile> profiles) { }
}
