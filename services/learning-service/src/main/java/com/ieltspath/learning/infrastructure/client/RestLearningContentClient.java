package com.ieltspath.learning.infrastructure.client;

import com.ieltspath.commonsecurity.header.SecurityHeaders;
import com.ieltspath.commonresilience.CallKind;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.commonresilience.ResilientCall;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
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
    private final ResilientCall resilientCall;
    /** The token for a call made outside any request (an event consumer); empty when there is none to offer. */
    private final java.util.function.Supplier<java.util.Optional<String>> workerToken;

    @Autowired
    public RestLearningContentClient(RestClient.Builder builder,
                                     @Value("${learning.content.base-url:http://localhost:8082}") String contentBaseUrl,
                                     ResilientCall resilientCall, LearningWorkerTokenProvider workerTokens) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        restClient = builder.requestFactory(requestFactory).baseUrl(contentBaseUrl).build();
        this.resilientCall = resilientCall;
        this.workerToken = () -> java.util.Optional.of(workerTokens.token());
    }

    RestLearningContentClient(RestClient restClient) {
        this.restClient = restClient;
        this.workerToken = java.util.Optional::empty;
        RetryConfig retryConfig = RetryConfig.custom().maxAttempts(2).waitDuration(Duration.ofMillis(1))
                .retryOnException(ResilientCall::isTransientFailure).build();
        this.resilientCall = new ResilientCall(CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.of(retryConfig), BulkheadRegistry.ofDefaults());
    }

    RestLearningContentClient(RestClient restClient, ResilientCall resilientCall) {
        this.restClient = restClient;
        this.resilientCall = resilientCall;
        this.workerToken = java.util.Optional::empty;
    }

    RestLearningContentClient(RestClient restClient, java.util.function.Supplier<java.util.Optional<String>> workerToken) {
        this(restClient, new ResilientCall(CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.of(RetryConfig.custom().maxAttempts(2).waitDuration(Duration.ofMillis(1))
                        .retryOnException(ResilientCall::isTransientFailure).build()), BulkheadRegistry.ofDefaults()), workerToken);
    }

    private RestLearningContentClient(RestClient restClient, ResilientCall resilientCall,
                                      java.util.function.Supplier<java.util.Optional<String>> workerToken) {
        this.restClient = restClient;
        this.resilientCall = resilientCall;
        this.workerToken = workerToken;
    }

    @Override
    public List<Topic> getTopicSequence() {
        return get("/topic-sequence", new ParameterizedTypeReference<>() { });
    }

    @Override
    public List<Topic> getTopicSequence(boolean includeErrorTraps) {
        if (!includeErrorTraps) return getTopicSequence();
        return get("/topic-sequence?includeErrorTraps={includeErrorTraps}",
                new ParameterizedTypeReference<>() { }, includeErrorTraps);
    }

    @Override
    public ProgressMetadata progressMetadata(List<UUID> courseIds) {
        return response(CallKind.READ, () -> restClient.post().uri(INTERNAL_PATH + "/progress-metadata")
                .headers(this::forwardRequestHeaders).body(Map.of("courseIds", courseIds))
                .retrieve().body(ProgressMetadata.class));
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
    public LessonSummary getLessonAccessSummary(UUID lessonId) {
        return get("/lessons/{id}/access", new ParameterizedTypeReference<>() { }, lessonId);
    }

    @Override
    public Lesson getLesson(UUID lessonId, boolean signedAudio) {
        return get("/lessons/{id}?signedAudio={signedAudio}", new ParameterizedTypeReference<>() { }, lessonId,
                signedAudio);
    }

    @Override
    public List<TestPackage> getTopicTestPackages(UUID topicId) {
        return get("/topics/{id}/test-packages", new ParameterizedTypeReference<>() { }, topicId);
    }

    @Override
    public java.util.Set<UUID> getFreeCourseIds() {
        List<UUID> ids = get("/courses/free", new ParameterizedTypeReference<>() { });
        return ids == null ? java.util.Set.of() : java.util.Set.copyOf(ids);
    }

    @Override
    public List<TestPackage> getCourseTestPackages(UUID courseId) {
        return get("/courses/{id}/test-packages", new ParameterizedTypeReference<>() { }, courseId);
    }

    @Override
    public LessonResources getLessonResources(UUID lessonId) {
        return get("/lessons/{id}/resources", new ParameterizedTypeReference<>() { }, lessonId);
    }

    @Override
    public List<PracticeTestMilestone> getPracticeTestMilestones(UUID courseId) {
        return get("/courses/{id}/practice-test-milestones", new ParameterizedTypeReference<>() { }, courseId);
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
        Availability response = response(CallKind.READ, () -> restClient.post()
                .uri(INTERNAL_PATH + "/practice-sets/availability")
                .headers(this::forwardRequestHeaders)
                .body(new AvailabilityRequest(knowledgePointIds, excludePackageIds, minQuestions))
                .retrieve().body(Availability.class));
        return response.counts();
    }

    @Override
    public PracticeTestKeys practiceTestKeys(List<UUID> packageVersionIds, List<UUID> questionVersionIds) {
        if (packageVersionIds.isEmpty() && questionVersionIds.isEmpty()) return new PracticeTestKeys(Map.of(), Map.of());
        KeysResponse response = response(CallKind.READ, () -> restClient.post()
                .uri(INTERNAL_PATH + "/practice-test-keys")
                .headers(this::forwardRequestHeaders)
                .body(new KeysRequest(packageVersionIds, questionVersionIds))
                .retrieve().body(KeysResponse.class));
        Map<UUID, UUID> packageByVersion = new java.util.HashMap<>();
        if (response.versions() != null) response.versions().forEach(item -> packageByVersion.put(item.packageVersionId(), item.packageId()));
        Map<UUID, PracticeTestQuestionKey> byQuestion = new java.util.HashMap<>();
        if (response.questions() != null) {
            response.questions().forEach(item -> byQuestion.put(item.questionVersionId(),
                    new PracticeTestQuestionKey(item.packageId(), item.partNo())));
        }
        return new PracticeTestKeys(packageByVersion, byQuestion);
    }

    @Override
    public List<PracticeSet> searchPracticeSets(UUID knowledgePointId, List<UUID> excludePackageIds,
                                               int minQuestions, int limit) {
        return searchPracticeSets(knowledgePointId, excludePackageIds, minQuestions, limit, null);
    }

    @Override
    public List<PracticeSet> searchPracticeSets(UUID knowledgePointId, List<UUID> excludePackageIds,
                                               int minQuestions, int limit, UUID preferredLessonId) {
        return searchPracticeSets(knowledgePointId, excludePackageIds, minQuestions, limit, preferredLessonId, null);
    }

    @Override
    public List<PracticeSet> searchPracticeSets(UUID knowledgePointId, List<UUID> excludePackageIds,
                                               int minQuestions, int limit, UUID preferredLessonId,
                                               String trapErrorCode) {
        return response(CallKind.READ, () -> restClient.post()
                .uri(INTERNAL_PATH + "/practice-sets/search")
                .headers(this::forwardRequestHeaders)
                .body(new PracticeSearch(knowledgePointId, excludePackageIds, minQuestions, limit, preferredLessonId,
                        trapErrorCode))
                .retrieve()
                .body(new ParameterizedTypeReference<List<PracticeSet>>() { }));
    }

    @Override
    public PackageVersion getPackageVersion(UUID versionId) {
        return get("/package-versions/{id}", new ParameterizedTypeReference<>() { }, versionId);
    }

    @Override
    public Map<UUID, List<UUID>> prerequisites(List<UUID> knowledgePointIds) {
        return response(CallKind.READ, () -> restClient.post()
                .uri(INTERNAL_PATH + "/knowledge-points/prerequisites")
                .headers(this::forwardRequestHeaders)
                .body(new PrerequisitesRequest(knowledgePointIds))
                .retrieve()
                .body(new ParameterizedTypeReference<Map<UUID, List<UUID>>>() { }));
    }

    @Override
    public Map<UUID, String> questionDifficulties(java.util.Collection<UUID> questionVersionIds) {
        if (questionVersionIds.isEmpty()) return Map.of();
        Difficulties response = response(CallKind.READ, () -> restClient.post()
                .uri(INTERNAL_PATH + "/questions/difficulties")
                .headers(this::forwardRequestHeaders)
                .body(new DifficultyRequest(List.copyOf(questionVersionIds)))
                .retrieve().body(Difficulties.class));
        return response.difficulties() == null ? Map.of() : response.difficulties();
    }

    @Override
    public WorkItemSubjects workItemSubjects(java.util.Collection<UUID> knowledgePointIds,
                                             java.util.Collection<UUID> questionVersionIds) {
        if (knowledgePointIds.isEmpty() && questionVersionIds.isEmpty()) return new WorkItemSubjects(Map.of(), Map.of());
        return response(CallKind.READ, () -> restClient.post()
                .uri(INTERNAL_PATH + "/work-item-subjects")
                .headers(this::forwardRequestHeaders)
                .body(new WorkItemSubjectsRequest(List.copyOf(knowledgePointIds), List.copyOf(questionVersionIds)))
                .retrieve().body(WorkItemSubjects.class));
    }

    @Override
    public SuggestionCandidates suggestionCandidates(List<UUID> knowledgePointIds, java.math.BigDecimal maxBand) {
        return response(CallKind.READ, () -> restClient.post()
                .uri(INTERNAL_PATH + "/suggestions/candidates")
                .headers(this::forwardRequestHeaders)
                .body(new SuggestionRequest(knowledgePointIds, maxBand))
                .retrieve().body(SuggestionCandidates.class));
    }

    @Override
    public Rubric getActiveRubric(String task) {
        return get("/rubrics/active?task={task}", new ParameterizedTypeReference<>() { }, task);
    }

    @Override
    public Rubric getRubricVersion(String code, int version) {
        return get("/rubrics/{code}/{version}", new ParameterizedTypeReference<>() { }, code, version);
    }

    @Override
    public List<ErrorType> activeErrorTypes() {
        return get("/error-types", new ParameterizedTypeReference<>() { });
    }

    @Override
    public List<ErrorType> activeErrorTypes(String skill) {
        return get("/error-types?skill={skill}", new ParameterizedTypeReference<>() { }, skill);
    }

    private <T> T get(String path, ParameterizedTypeReference<T> type, Object... uriVariables) {
        return response(CallKind.READ, () -> restClient.get()
                .uri(INTERNAL_PATH + path, uriVariables)
                .headers(this::forwardRequestHeaders)
                .retrieve()
                .body(type));
    }

    private <T> T response(CallKind kind, Supplier<T> request) {
        try {
            T body = resilientCall.call("content", kind, request);
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
        } catch (DownstreamUnavailableException exception) {
            throw contentUnavailable();
        } catch (RestClientException exception) {
            throw contentFailure();
        }
    }

    private void forwardRequestHeaders(HttpHeaders headers) {
        // A request must carry the learner's verified JWT. Only work with no request at all, such as applying an event, uses
        // the worker token; an unauthenticated request never does.
        headers.setBearerAuth(GatewayRequestHeaders.bearerToken()
                .or(() -> GatewayRequestHeaders.outsideRequest() ? workerToken.get() : java.util.Optional.empty())
                .orElseThrow(() -> new LearningRequestException(401, "UNAUTHORIZED", "Authenticated gateway JWT required")));
        headers.set(SecurityHeaders.CORRELATION_ID, GatewayRequestHeaders.correlationId());
    }

    private LearningRequestException contentUnavailable() {
        return new LearningRequestException(503, "CONTENT_UNAVAILABLE", "Learning content is unavailable");
    }

    private LearningRequestException contentFailure() {
        return new LearningRequestException(502, "CONTENT_FAILURE", "Learning content request failed");
    }

    private record PracticeSearch(UUID knowledgePointId, List<UUID> excludePackageIds,
                                  int minQuestions, int limit, UUID preferredLessonId, String trapErrorCode) {}
    private record AvailabilityRequest(List<UUID> knowledgePointIds, List<UUID> excludePackageIds,
                                       int minQuestions) {}
    private record DifficultyRequest(List<UUID> questionVersionIds) {}
    private record WorkItemSubjectsRequest(List<UUID> knowledgePointIds, List<UUID> questionVersionIds) {}
    private record Difficulties(Map<UUID, String> difficulties) {}
    private record SuggestionRequest(List<UUID> knowledgePointIds, java.math.BigDecimal maxBand) {}
    private record PrerequisitesRequest(List<UUID> ids) {}
    private record Availability(Map<UUID, Integer> counts) {}
    private record KeysRequest(List<UUID> packageVersionIds, List<UUID> questionVersionIds) {}
    private record KeysResponse(List<VersionKey> versions, List<QuestionKey> questions) {}
    private record VersionKey(UUID packageVersionId, UUID packageId) {}
    private record QuestionKey(UUID questionVersionId, UUID packageId, Integer partNo) {}
}
