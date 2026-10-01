package com.group01.learning.infrastructure.client;

import com.group01.commonsecurity.header.SecurityHeaders;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearningContentClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class RestLearningContentClient implements LearningContentClient {
    private static final String INTERNAL_PATH = "/internal/learning-content";
    private final RestClient restClient;

    @Autowired
    public RestLearningContentClient(RestClient.Builder builder,
                                     @Value("${learning.content.base-url:http://localhost:8082}") String contentBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        restClient = builder.requestFactory(requestFactory).baseUrl(contentBaseUrl).build();
    }

    RestLearningContentClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<Topic> getTopicSequence() {
        return get("/topic-sequence", new ParameterizedTypeReference<>() { });
    }

    @Override
    public List<LessonSummary> getTopicLessons(UUID topicId) {
        return get("/topics/{id}/lessons", new ParameterizedTypeReference<>() { }, topicId);
    }

    @Override
    public Lesson getLesson(UUID lessonId) {
        return get("/lessons/{id}", new ParameterizedTypeReference<>() { }, lessonId);
    }

    @Override
    public List<TestPackage> getTopicTestPackages(UUID topicId) {
        return get("/topics/{id}/test-packages", new ParameterizedTypeReference<>() { }, topicId);
    }

    @Override
    public List<PracticeSet> searchPracticeSets(UUID knowledgePointId, List<UUID> excludePackageIds,
                                               int minQuestions, int limit) {
        return response(() -> restClient.post()
                .uri(INTERNAL_PATH + "/practice-sets/search")
                .headers(this::forwardRequestHeaders)
                .body(new PracticeSearch(knowledgePointId, excludePackageIds, minQuestions, limit))
                .retrieve()
                .body(new ParameterizedTypeReference<List<PracticeSet>>() { }));
    }

    @Override
    public PackageVersion getPackageVersion(UUID versionId) {
        return get("/package-versions/{id}", new ParameterizedTypeReference<>() { }, versionId);
    }

    private <T> T get(String path, ParameterizedTypeReference<T> type, Object... uriVariables) {
        return response(() -> restClient.get()
                .uri(INTERNAL_PATH + path, uriVariables)
                .headers(this::forwardRequestHeaders)
                .retrieve()
                .body(type));
    }

    private <T> T response(Supplier<T> request) {
        try {
            T body = request.get();
            if (body == null) throw contentFailure();
            return body;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new LearningRequestException(404, "NOT_FOUND", "Learning content was not found");
            }
            if (exception.getStatusCode().value() == 503) throw contentUnavailable();
            throw contentFailure();
        } catch (ResourceAccessException exception) {
            throw contentUnavailable();
        } catch (RestClientException exception) {
            throw contentFailure();
        }
    }

    private void forwardRequestHeaders(HttpHeaders headers) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) || !authentication.isAuthenticated()) {
            throw new LearningRequestException(401, "UNAUTHORIZED", "Authenticated gateway JWT required");
        }
        headers.setBearerAuth(token.getToken().getTokenValue());
        headers.set(SecurityHeaders.CORRELATION_ID, correlationId());
    }

    private String correlationId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String value = attributes.getRequest().getHeader(SecurityHeaders.CORRELATION_ID);
            if (value != null && !value.isBlank() && value.length() <= 128
                    && value.chars().allMatch(character -> character >= 32 && character <= 126)) {
                return value;
            }
        }
        return UUID.randomUUID().toString();
    }

    private LearningRequestException contentUnavailable() {
        return new LearningRequestException(503, "CONTENT_UNAVAILABLE", "Learning content is unavailable");
    }

    private LearningRequestException contentFailure() {
        return new LearningRequestException(502, "CONTENT_FAILURE", "Learning content request failed");
    }

    private record PracticeSearch(UUID knowledgePointId, List<UUID> excludePackageIds,
                                  int minQuestions, int limit) {
    }
}
