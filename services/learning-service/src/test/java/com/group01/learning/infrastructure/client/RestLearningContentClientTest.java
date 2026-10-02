package com.group01.learning.infrastructure.client;

import com.group01.commonsecurity.header.SecurityHeaders;
import com.group01.learning.application.exception.LearningRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RestLearningContentClientTest {
    private static final String BASE_URL = "http://content.test";
    private static final String INTERNAL_PATH = BASE_URL + "/internal/learning-content";
    private static final UUID TOPIC_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID KP_ID = UUID.fromString("20000000-0000-4000-8000-000000000003");
    private static final UUID LESSON_ID = UUID.fromString("20000000-0000-4000-8000-000000000101");
    private static final UUID PACKAGE_ID = UUID.fromString("20000000-0000-4000-8000-000000000301");
    private static final UUID VERSION_ID = UUID.fromString("20000000-0000-4000-8000-000000000401");
    private MockRestServiceServer server;
    private RestLearningContentClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestLearningContentClient(builder.build());
        var jwt = Jwt.withTokenValue("test-internal-token").header("alg", "HS256")
                .subject(UUID.randomUUID().toString()).issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
        server.verify();
    }

    @Test
    void forwardsVerifiedJwtAndCorrelationAndMapsTopicSequence() {
        MockHttpServletRequest request = requestWithCorrelation("learning-request-123");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer unverified-request-header");
        request.addHeader("X-User-Id", UUID.randomUUID().toString());
        server.expect(requestTo(INTERNAL_PATH + "/topic-sequence"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-internal-token"))
                .andExpect(header(SecurityHeaders.CORRELATION_ID, "learning-request-123"))
                .andExpect(headerDoesNotExist("X-User-Id"))
                .andRespond(withSuccess("""
                        [{"topicId":"10000000-0000-4000-8000-000000000001",
                          "code":"DEMO_READING","name":"Demo IELTS Reading","sortOrder":900,
                          "knowledgePoints":[{"id":"20000000-0000-4000-8000-000000000003",
                            "code":"DR_TOPIC_SENTENCE","name":"Topic sentence","learningType":"PROCEDURE",
                            "skill":"READING","description":null,"hasPracticeSet":true}]}]
                        """, MediaType.APPLICATION_JSON));

        var topics = client.getTopicSequence();

        assertEquals(1, topics.size());
        var topic = topics.getFirst();
        assertEquals(TOPIC_ID, topic.topicId());
        assertEquals("DEMO_READING", topic.code());
        assertEquals(900, topic.sortOrder());
        var knowledgePoint = topic.knowledgePoints().getFirst();
        assertEquals(KP_ID, knowledgePoint.id());
        assertEquals("PROCEDURE", knowledgePoint.learningType());
        assertEquals("READING", knowledgePoint.skill());
        assertNull(knowledgePoint.description());
        assertTrue(knowledgePoint.hasPracticeSet());
    }

    @Test
    void mapsLessonSummaryIdentifiers() {
        server.expect(requestTo(INTERNAL_PATH + "/topics/" + TOPIC_ID + "/lessons"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"lessonId":"20000000-0000-4000-8000-000000000101",
                          "topicId":"10000000-0000-4000-8000-000000000001","code":"L1",
                          "title":"Topic sentences","summary":null,"sortOrder":1,
                          "knowledgePointIds":["20000000-0000-4000-8000-000000000003"],
                          "exerciseBlockIds":["20000000-0000-4000-8000-000000000202"]}]
                        """, MediaType.APPLICATION_JSON));

        var lesson = client.getTopicLessons(TOPIC_ID).getFirst();

        assertEquals(LESSON_ID, lesson.lessonId());
        assertEquals(TOPIC_ID, lesson.topicId());
        assertEquals("L1", lesson.code());
        assertEquals(List.of(KP_ID), lesson.knowledgePointIds());
        assertEquals(List.of(UUID.fromString("20000000-0000-4000-8000-000000000202")),
                lesson.exerciseBlockIds());
    }

    @Test
    void mapsLessonBlocksAndChoiceAndFillAnswerSpecs() {
        server.expect(requestTo(INTERNAL_PATH + "/lessons/" + LESSON_ID))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"lessonId":"20000000-0000-4000-8000-000000000101",
                         "topicId":"10000000-0000-4000-8000-000000000001","code":"L1",
                         "title":"Topic sentences","summary":null,"sortOrder":1,
                         "knowledgePointIds":["20000000-0000-4000-8000-000000000003"],
                         "blocks":[
                           {"blockId":"20000000-0000-4000-8000-000000000201","blockType":"TEXT",
                            "sortOrder":1,"textContent":"Lesson prose"},
                           {"blockId":"20000000-0000-4000-8000-000000000204","blockType":"ASSET",
                            "sortOrder":2,"asset":{"id":"20000000-0000-4000-8000-000000000801",
                              "assetType":"PASSAGE","textContent":"A reading passage.",
                              "mediaReference":null,"durationSeconds":null}},
                           {"blockId":"20000000-0000-4000-8000-000000000207","blockType":"VOCABULARY",
                            "sortOrder":3,"vocabularySenseIds":["20000000-0000-4000-8000-000000000901"]},
                           {"blockId":"20000000-0000-4000-8000-000000000202","blockType":"EXERCISE",
                            "sortOrder":4,"questions":[
                              {"questionVersionId":"20000000-0000-4000-8000-000000000013",
                               "sortOrder":1,"stem":"Which sentence is the topic sentence?",
                               "options":[{"optionKey":"A","content":"Main idea","sortOrder":1}],
                               "answerSpec":{"type":"CHOICE","correct":"A"},
                               "explanation":"A states the main idea.","hint":"Compare the scope of the options.",
                               "knowledgePointIds":["20000000-0000-4000-8000-000000000003"]},
                              {"questionVersionId":"20000000-0000-4000-8000-000000000012",
                               "sortOrder":2,"stem":"People who doubt are called ____.","options":null,
                               "answerSpec":{"type":"FILL","accepted":["critics"]},
                               "explanation":"The passage says critics.","hint":"Find the noun near the contrast.",
                               "knowledgePointIds":["20000000-0000-4000-8000-000000000003"]}]}]}
                        """, MediaType.APPLICATION_JSON));

        var lesson = client.getLesson(LESSON_ID);

        assertEquals(LESSON_ID, lesson.lessonId());
        assertEquals(4, lesson.blocks().size());
        assertEquals("Lesson prose", lesson.blocks().get(0).textContent());
        var passage = lesson.blocks().get(1).asset();
        assertEquals(UUID.fromString("20000000-0000-4000-8000-000000000801"), passage.id());
        assertEquals("PASSAGE", passage.assetType());
        assertEquals("A reading passage.", passage.textContent());
        assertNull(passage.mediaReference());
        assertNull(passage.durationSeconds());
        assertEquals(List.of(UUID.fromString("20000000-0000-4000-8000-000000000901")),
                lesson.blocks().get(2).vocabularySenseIds());
        var questions = lesson.blocks().get(3).questions();
        assertEquals(2, questions.size());
        assertEquals("A", questions.getFirst().options().getFirst().optionKey());
        assertEquals("Main idea", questions.getFirst().options().getFirst().content());
        assertEquals("A", questions.getFirst().answerSpec().get("correct"));
        assertEquals("A states the main idea.", questions.getFirst().explanation());
        assertEquals("Compare the scope of the options.", questions.getFirst().hint());
        assertEquals("Find the noun near the contrast.", questions.get(1).hint());
        assertEquals(List.of(KP_ID), questions.getFirst().knowledgePointIds());
        assertNull(questions.get(1).options());
        assertEquals("FILL", questions.get(1).answerSpec().get("type"));
        assertEquals(List.of("critics"), questions.get(1).answerSpec().get("accepted"));
    }

    @Test
    void mapsTopicTestPackages() {
        server.expect(requestTo(INTERNAL_PATH + "/topics/" + TOPIC_ID + "/test-packages"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"packageId":"20000000-0000-4000-8000-000000000301",
                          "packageVersionId":"20000000-0000-4000-8000-000000000401","code":"X1"}]
                        """, MediaType.APPLICATION_JSON));

        var testPackage = client.getTopicTestPackages(TOPIC_ID).getFirst();

        assertEquals(PACKAGE_ID, testPackage.packageId());
        assertEquals(VERSION_ID, testPackage.packageVersionId());
        assertEquals("X1", testPackage.code());
    }

    @Test
    void sendsPracticeSearchAndMapsEligiblePackages() {
        requestWithCorrelation("practice-search-123");
        server.expect(requestTo(INTERNAL_PATH + "/practice-sets/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-internal-token"))
                .andExpect(header(SecurityHeaders.CORRELATION_ID, "practice-search-123"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"knowledgePointId":"20000000-0000-4000-8000-000000000003",
                         "excludePackageIds":["20000000-0000-4000-8000-000000000301"],
                         "minQuestions":3,"limit":2}
                        """))
                .andRespond(withSuccess("""
                        [{"packageId":"20000000-0000-4000-8000-000000000501",
                          "packageVersionId":"20000000-0000-4000-8000-000000000601",
                          "code":"PS-KP1-A","questionCount":4,"matchedQuestionCount":3}]
                        """, MediaType.APPLICATION_JSON));

        var practiceSet = client.searchPracticeSets(KP_ID, List.of(PACKAGE_ID), 3, 2).getFirst();

        assertEquals(UUID.fromString("20000000-0000-4000-8000-000000000501"), practiceSet.packageId());
        assertEquals(UUID.fromString("20000000-0000-4000-8000-000000000601"), practiceSet.packageVersionId());
        assertEquals("PS-KP1-A", practiceSet.code());
        assertEquals(4, practiceSet.questionCount());
        assertEquals(3, practiceSet.matchedQuestionCount());
    }

    @Test
    void mapsPackageSectionsScoresAndKnowledgePointMappings() {
        server.expect(requestTo(INTERNAL_PATH + "/package-versions/" + VERSION_ID))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"packageVersionId":"20000000-0000-4000-8000-000000000401",
                         "packageId":"20000000-0000-4000-8000-000000000301","packageType":"TOPIC_TEST",
                         "topicId":"10000000-0000-4000-8000-000000000001","rules":{},
                         "sections":[{"sectionId":"20000000-0000-4000-8000-000000000701",
                           "title":"Street trees","skill":"READING","instructions":null,"sortOrder":1,
                           "passage":"City trees improve life.",
                           "items":[{"questionVersionId":"20000000-0000-4000-8000-000000000002",
                             "sortOrder":1,"stem":"What is the passage mainly about?",
                             "options":[{"optionKey":"B","content":"City life","sortOrder":2}],
                             "answerSpec":{"type":"CHOICE","correct":"B"},
                             "explanation":"The passage describes benefits.","hint":"Package hints stay ignored.","maxScore":1.5,
                             "knowledgePointMappings":[{
                               "knowledgePointId":"20000000-0000-4000-8000-000000000003","weight":0.75}]}]}]}
                        """, MediaType.APPLICATION_JSON));

        var version = client.getPackageVersion(VERSION_ID);

        assertEquals(VERSION_ID, version.packageVersionId());
        assertEquals(PACKAGE_ID, version.packageId());
        assertEquals("TOPIC_TEST", version.packageType());
        assertEquals(TOPIC_ID, version.topicId());
        assertTrue(version.rules().isEmpty());
        var section = version.sections().getFirst();
        assertEquals(UUID.fromString("20000000-0000-4000-8000-000000000701"), section.sectionId());
        assertEquals("READING", section.skill());
        assertEquals("City trees improve life.", section.passage());
        assertNull(section.instructions());
        var item = section.items().getFirst();
        assertEquals("B", item.answerSpec().get("correct"));
        assertEquals(new BigDecimal("1.5"), item.maxScore());
        assertEquals(KP_ID, item.knowledgePointMappings().getFirst().knowledgePointId());
        assertEquals(new BigDecimal("0.75"), item.knowledgePointMappings().getFirst().weight());
        assertFalse(java.util.Arrays.stream(item.getClass().getRecordComponents())
                .anyMatch(component -> component.getName().equals("hint")));
    }

    @ParameterizedTest
    @MethodSource("invalidCorrelationIds")
    void generatesCorrelationIdForMissingOrInvalidRequestHeader(String correlationId) {
        requestWithCorrelation(correlationId);
        server.expect(requestTo(INTERNAL_PATH + "/topic-sequence"))
                .andExpect(outbound -> assertDoesNotThrow(() -> UUID.fromString(
                        outbound.getHeaders().getFirst(SecurityHeaders.CORRELATION_ID))))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.getTopicSequence().isEmpty());
    }

    private static Stream<String> invalidCorrelationIds() {
        return Stream.of(null, "", "   ", "x".repeat(129), "unsafe\ncorrelation");
    }

    @Test
    void generatesCorrelationIdWithoutServletRequestContext() {
        server.expect(requestTo(INTERNAL_PATH + "/topic-sequence"))
                .andExpect(outbound -> assertDoesNotThrow(() -> UUID.fromString(
                        outbound.getHeaders().getFirst(SecurityHeaders.CORRELATION_ID))))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.getTopicSequence().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"404,404,NOT_FOUND", "503,503,CONTENT_UNAVAILABLE", "400,502,CONTENT_FAILURE",
            "401,502,CONTENT_FAILURE", "403,502,CONTENT_FAILURE", "500,502,CONTENT_FAILURE"})
    void translatesHttpErrorsWithoutExposingContentResponse(int contentStatus, int status, String code) {
        server.expect(requestTo(INTERNAL_PATH + "/lessons/" + LESSON_ID))
                .andRespond(withStatus(HttpStatus.valueOf(contentStatus))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"private-content-response\"}"));

        var exception = assertThrows(LearningRequestException.class, () -> client.getLesson(LESSON_ID));

        assertEquals(status, exception.getStatus());
        assertEquals(code, exception.getCode());
        assertFalse(exception.getMessage().contains("private-content-response"));
        assertNull(exception.getCause());
    }

    @Test
    void translatesTransportFailureToContentUnavailable() {
        server.expect(requestTo(INTERNAL_PATH + "/topic-sequence"))
                .andRespond(withException(new IOException("private-transport-detail")));

        var exception = assertThrows(LearningRequestException.class, client::getTopicSequence);

        assertEquals(503, exception.getStatus());
        assertEquals("CONTENT_UNAVAILABLE", exception.getCode());
        assertFalse(exception.getMessage().contains("private-transport-detail"));
        assertNull(exception.getCause());
    }

    @Test
    void translatesMalformedResponseToContentFailure() {
        server.expect(requestTo(INTERNAL_PATH + "/lessons/" + LESSON_ID))
                .andRespond(withSuccess("{private-invalid-json", MediaType.APPLICATION_JSON));

        var exception = assertThrows(LearningRequestException.class, () -> client.getLesson(LESSON_ID));

        assertEquals(502, exception.getStatus());
        assertEquals("CONTENT_FAILURE", exception.getCode());
        assertFalse(exception.getMessage().contains("private-invalid-json"));
        assertNull(exception.getCause());
    }

    @Test
    void translatesMissingResponseBodyToContentFailure() {
        server.expect(requestTo(INTERNAL_PATH + "/lessons/" + LESSON_ID))
                .andRespond(withNoContent());

        var exception = assertThrows(LearningRequestException.class, () -> client.getLesson(LESSON_ID));

        assertEquals(502, exception.getStatus());
        assertEquals("CONTENT_FAILURE", exception.getCode());
    }

    @Test
    void rejectsRawRequestBearerWithoutVerifiedJwt() {
        SecurityContextHolder.clearContext();
        requestWithCorrelation("raw-request").addHeader(HttpHeaders.AUTHORIZATION, "Bearer raw-request-token");

        var exception = assertThrows(LearningRequestException.class, client::getTopicSequence);

        assertEquals(401, exception.getStatus());
        assertEquals("UNAUTHORIZED", exception.getCode());
    }

    @Test
    void rejectsUnauthenticatedJwt() {
        SecurityContextHolder.getContext().getAuthentication().setAuthenticated(false);

        var exception = assertThrows(LearningRequestException.class, client::getTopicSequence);

        assertEquals(401, exception.getStatus());
        assertEquals("UNAUTHORIZED", exception.getCode());
    }

    private MockHttpServletRequest requestWithCorrelation(String correlationId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (correlationId != null) request.addHeader(SecurityHeaders.CORRELATION_ID, correlationId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }
}
