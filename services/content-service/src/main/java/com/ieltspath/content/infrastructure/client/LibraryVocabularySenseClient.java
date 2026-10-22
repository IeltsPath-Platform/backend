package com.ieltspath.content.infrastructure.client;

import com.ieltspath.content.application.exception.VocabularyServiceUnavailableException;
import com.ieltspath.content.application.port.VocabularySenseLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class LibraryVocabularySenseClient implements VocabularySenseLookup {
    private final RestClient restClient;

    @Autowired
    public LibraryVocabularySenseClient(RestClient.Builder builder,
            @Value("${library.content.base-url:http://localhost:8082}") String libraryBaseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = builder.requestFactory(factory).baseUrl(libraryBaseUrl).build();
    }

    LibraryVocabularySenseClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Set<UUID> findActiveSenseIds(List<UUID> senseIds) {
        if (senseIds == null || senseIds.isEmpty() || senseIds.size() > 100) return Set.of();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) || !authentication.isAuthenticated()) {
            throw new VocabularyServiceUnavailableException(new IllegalStateException("Authenticated gateway JWT required"));
        }
        try {
            VocabularySense[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/content/vocabulary/senses")
                            .queryParam("ids", senseIds.stream().map(UUID::toString).collect(Collectors.joining(",")))
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getToken().getTokenValue())
                    .header("X-Correlation-Id", correlationId())
                    .retrieve().body(VocabularySense[].class);
            if (response == null) return Set.of();
            return Arrays.stream(response).map(VocabularySense::senseId).collect(Collectors.toUnmodifiableSet());
        } catch (RestClientException exception) {
            throw new VocabularyServiceUnavailableException(exception);
        }
    }

    private String correlationId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String value = attributes.getRequest().getHeader("X-Correlation-Id");
            if (value != null && !value.isBlank() && value.length() <= 128) return value;
        }
        return UUID.randomUUID().toString();
    }

    private record VocabularySense(UUID senseId) {}
}
