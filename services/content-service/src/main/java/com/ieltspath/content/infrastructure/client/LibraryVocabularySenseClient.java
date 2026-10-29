package com.ieltspath.content.infrastructure.client;

import com.ieltspath.content.application.exception.VocabularyRejectedException;
import com.ieltspath.content.application.exception.VocabularyServiceUnavailableException;
import com.ieltspath.content.application.port.VocabularySenseLookup;
import com.ieltspath.content.application.port.VocabularySenseWriter;
import com.ieltspath.content.domain.vo.CourseDraftStructure.NewWord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class LibraryVocabularySenseClient implements VocabularySenseLookup, VocabularySenseWriter {
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
        return describeActiveSenses(senseIds).stream().map(SenseText::senseId).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public List<SenseText> describeActiveSenses(List<UUID> senseIds) {
        if (senseIds == null || senseIds.isEmpty() || senseIds.size() > 100) return List.of();
        JwtAuthenticationToken token = signedIn();
        try {
            VocabularySense[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/content/vocabulary/senses")
                            .queryParam("ids", senseIds.stream().map(UUID::toString).collect(Collectors.joining(",")))
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getToken().getTokenValue())
                    .header("X-Correlation-Id", correlationId())
                    .retrieve().body(VocabularySense[].class);
            if (response == null) return List.of();
            return Arrays.stream(response)
                    .map(sense -> new SenseText(sense.senseId(), sense.lemma(), sense.vietnameseMeaning())).toList();
        } catch (RestClientException exception) {
            throw new VocabularyServiceUnavailableException(exception);
        }
    }

    @Override
    public Map<String, UUID> resolveOrCreate(List<NewWord> words) {
        if (words == null || words.isEmpty()) return Map.of();
        JwtAuthenticationToken token = signedIn();
        List<Entry> entries = words.stream().map(word -> new Entry(word.lemma(), word.ipa(), word.partOfSpeech(),
                word.englishDefinition(), word.vietnameseMeaning(), word.exampleSentence())).toList();
        try {
            Resolved[] response = restClient.post().uri("/api/content/admin/vocabulary/bulk")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getToken().getTokenValue())
                    .header("X-Correlation-Id", correlationId())
                    .body(new Bulk(entries)).retrieve().body(Resolved[].class);
            Map<String, UUID> senses = new LinkedHashMap<>();
            if (response != null) Arrays.stream(response).forEach(resolved -> senses.put(resolved.lemma(), resolved.senseId()));
            return senses;
        } catch (HttpClientErrorException refused) {
            throw new VocabularyRejectedException(refusal(refused));
        } catch (RestClientException exception) {
            throw new VocabularyServiceUnavailableException(exception);
        }
    }

    /** The author's own signed-in token: the library only lets authors write words. */
    private JwtAuthenticationToken signedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) || !authentication.isAuthenticated()) {
            throw new VocabularyServiceUnavailableException(new IllegalStateException("Authenticated gateway JWT required"));
        }
        return token;
    }

    private static String refusal(HttpClientErrorException refused) {
        try {
            Map<?, ?> body = refused.getResponseBodyAs(Map.class);
            Object message = body == null ? null : body.get("message");
            if (message != null && !message.toString().isBlank()) return message.toString();
        } catch (RuntimeException unreadable) {
            // fall through to the status
        }
        return "Vocabulary service refused the words (" + refused.getStatusCode().value() + ")";
    }

    private String correlationId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String value = attributes.getRequest().getHeader("X-Correlation-Id");
            if (value != null && !value.isBlank() && value.length() <= 128) return value;
        }
        return UUID.randomUUID().toString();
    }

    private record VocabularySense(UUID senseId, String lemma, String vietnameseMeaning) {}

    private record Entry(String lemma, String ipa, String partOfSpeech, String englishDefinition,
                         String vietnameseMeaning, String exampleSentence) {}

    private record Bulk(List<Entry> entries) {}

    private record Resolved(String lemma, UUID senseId) {}
}
