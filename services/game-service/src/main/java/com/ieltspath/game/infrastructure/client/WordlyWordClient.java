package com.ieltspath.game.infrastructure.client;

import com.ieltspath.game.application.port.WordlyWordProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.UUID;

@Component
public class WordlyWordClient implements WordlyWordProvider {
    private final RestClient client;

    public WordlyWordClient(
            RestClient.Builder builder,
            @Value("${game.library.base-url:${LIBRARY_SERVICE_URL:http://localhost:8081}}")
                    String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        client = builder.clone().requestFactory(factory).baseUrl(baseUrl).build();
    }

    private String bearer() {
        if (!(SecurityContextHolder.getContext().getAuthentication()
                        instanceof JwtAuthenticationToken auth)
                || !auth.isAuthenticated())
            throw new IllegalStateException("Authenticated user required");
        return "Bearer " + auth.getToken().getTokenValue();
    }

    private static String correlation() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            String value = attrs.getRequest().getHeader("X-Correlation-Id");
            if (value != null && !value.isBlank() && value.length() <= 128) return value;
        }
        return UUID.randomUUID().toString();
    }

    private Response request(String path, java.util.Map<String, Object> body) {
        Response response =
                client.post()
                        .uri(path)
                        .header("Authorization", bearer())
                        .header("X-Correlation-Id", correlation())
                        .body(body)
                        .retrieve()
                        .body(Response.class);
        if (response == null || response.words() == null)
            throw new IllegalStateException("Word source unavailable");
        return response;
    }

    public java.util.List<Word> random(int count) {
        return request("/internal/game-content/wordly/random", java.util.Map.of("count", count))
                .words();
    }

    public java.util.List<Word> catalog() {
        return catalog(com.ieltspath.game.domain.vo.WordFilter.ANY);
    }

    public Filters filters() {
        try {
            Filters filters = client.get().uri("/internal/game-content/wordly/filters")
                    .header("Authorization", bearer()).header("X-Correlation-Id", correlation())
                    .retrieve().body(Filters.class);
            if (filters != null && filters.topics() != null && filters.availability() != null) return filters;
        } catch (org.springframework.web.client.RestClientException e) {
            throw new com.ieltspath.game.domain.exception.SoloGameplayException(503, "WORD_FILTERS_UNAVAILABLE", "Word filters are unavailable");
        }
        throw new com.ieltspath.game.domain.exception.SoloGameplayException(503, "WORD_FILTERS_UNAVAILABLE", "Word filters are unavailable");
    }

    public java.util.List<Word> catalog(com.ieltspath.game.domain.vo.WordFilter filter) {
        java.util.List<Word> result = new java.util.ArrayList<>();
        String after = "";
        while (true) {
            var page =
                    request(
                                    "/internal/game-content/wordly/catalog",
                                    pageBody(after, filter))
                            .words();
            if (page.isEmpty()) return result;
            String next = page.get(page.size() - 1).answer();
            if (next == null || next.compareTo(after) <= 0)
                throw new IllegalStateException("Invalid catalog cursor");
            result.addAll(page);
            after = next;
            if (result.size() > 100000)
                throw new IllegalStateException("Vocabulary snapshot exceeds supported size");
        }
    }

    private static java.util.Map<String, Object> pageBody(String after, com.ieltspath.game.domain.vo.WordFilter filter) {
        var body = new java.util.HashMap<String, Object>(java.util.Map.of("after", after, "limit", 500));
        if (filter.cefrLevel() != null) body.put("cefrLevel", filter.cefrLevel());
        if (filter.topicId() != null) body.put("topicId", filter.topicId());
        return body;
    }

    private record Response(java.util.List<Word> words) {}
}
