package com.group01.game.infrastructure.client;

import com.group01.game.application.port.GameContentProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class ContentSnapshotClient implements GameContentProvider {
    private final RestClient restClient;

    public ContentSnapshotClient(RestClient.Builder builder,
                                 @Value("${game.content.base-url:http://localhost:8082}") String contentBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder.requestFactory(requestFactory).baseUrl(contentBaseUrl).build();
    }

    @Override
    public List<GameContentItem> loadSnapshot(String gameType, String learningDomain, List<UUID> contentIds) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authenticated gateway JWT is required to read game content");
        }
        ContentSnapshotResponse response = restClient.post()
                .uri("/internal/game-content/snapshots")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtAuthenticationToken.getToken().getTokenValue())
                .header("X-Correlation-Id", correlationId())
                .body(new ContentSnapshotRequest(gameType, contentIds, learningDomain))
                .retrieve()
                .body(ContentSnapshotResponse.class);
        if (response == null || response.items() == null) return List.of();
        return response.items().stream().map(item -> new GameContentItem(item.canonicalId(),
                item.vocabularySenseId(), item.questionVersionId(), item.prompt(), item.options(),
                item.answerSpecJson(), item.explanation())).toList();
    }

    private String correlationId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String value = attributes.getRequest().getHeader("X-Correlation-Id");
            if (value != null && !value.isBlank() && value.length() <= 128) return value;
        }
        return UUID.randomUUID().toString();
    }

    private record ContentSnapshotRequest(String gameType, List<UUID> contentIds, String learningDomain) {}
    private record ContentSnapshotResponse(List<Item> items) {
        private record Item(UUID canonicalId, UUID vocabularySenseId, UUID questionVersionId,
                            String prompt, List<?> options, String answerSpecJson, String explanation) {}
    }
}
