package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public List<TestPackage> getCourseTestPackages(UUID courseId) {
        return get("/courses/{id}/test-packages", new ParameterizedTypeReference<>() { }, courseId);
    }

    @Override
    public List<TestPackage> getPlacementTestPackages() {
        return get("/placement-packages", new ParameterizedTypeReference<>() { });
    }

    @Override
    public List<LessonPracticeSet> lessonPracticeSets(UUID lessonId) {
        return get("/lessons/{id}/practice-sets", new ParameterizedTypeReference<>() { }, lessonId);
    }

    @Override
    public List<LessonPracticeSet> lessonPracticeSets(UUID lessonId, Optional<LearningSkill> skill) {
        if (skill.isEmpty()) return lessonPracticeSets(lessonId);
        return get("/lessons/{id}/practice-sets?skill={skill}", new ParameterizedTypeReference<>() { }, lessonId,
                skill.get().name());
    }

    @Override
    public TopicPracticeSets topicPracticeSets(UUID topicId) {
        return get("/topics/{id}/practice-sets", new ParameterizedTypeReference<>() { }, topicId);
    }

    @Override
    public Map<UUID, Integer> practiceSetAvailability(List<UUID> knowledgePointIds,
                                                       List<UUID> excludePackageIds, int minQuestions) {
        Availability response = response(() -> restClient.post()
                .uri(INTERNAL_PATH + "/practice-sets/availability")
                .headers(this::forwardRequestHeaders)
                .body(new AvailabilityRequest(knowledgePointIds, excludePackageIds, minQuestions))
                .retrieve().body(Availability.class));
        return response.counts();
    }

    @Override
    public List<PracticeSet> searchPracticeSets(UUID knowledgePointId, List<UUID> excludePackageIds,
                                               int minQuestions, int limit) {
        return searchPracticeSets(knowledgePointId, excludePackageIds, minQuestions, limit, null);
    }

    @Override
    public List<PracticeSet> searchPracticeSets(UUID knowledgePointId, List<UUID> excludePackageIds,
                                               int minQuestions, int limit, UUID preferredLessonId) {
        return response(() -> restClient.post()
                .uri(INTERNAL_PATH + "/practice-sets/search")
                .headers(this::forwardRequestHeaders)
                .body(new PracticeSearch(knowledgePointId, excludePackageIds, minQuestions, limit, preferredLessonId))
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
        headers.setBearerAuth(GatewayRequestHeaders.bearerToken().orElseThrow(
                () -> new LearningRequestException(401, "UNAUTHORIZED", "Authenticated gateway JWT required")));
        headers.set(SecurityHeaders.CORRELATION_ID, GatewayRequestHeaders.correlationId());
    }

    private LearningRequestException contentUnavailable() {
        return new LearningRequestException(503, "CONTENT_UNAVAILABLE", "Learning content is unavailable");
    }

    private LearningRequestException contentFailure() {
        return new LearningRequestException(502, "CONTENT_FAILURE", "Learning content request failed");
    }

    private record PracticeSearch(UUID knowledgePointId, List<UUID> excludePackageIds,
                                  int minQuestions, int limit, UUID preferredLessonId) {}
    private record AvailabilityRequest(List<UUID> knowledgePointIds, List<UUID> excludePackageIds,
                                       int minQuestions) {}
    private record Availability(Map<UUID, Integer> counts) {}
}
