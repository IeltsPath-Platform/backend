package com.group01.learning.infrastructure.persistence;

import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.exception.LearningRequestException;
import com.group01.learning.application.port.LearningContentClient;
import com.group01.learning.application.port.ExerciseSubmissionLog;
import com.group01.learning.application.port.LearnerLock;
import com.group01.learning.application.result.MasteryResult;
import com.group01.learning.application.result.SubmissionResult;
import com.group01.learning.application.usecase.GetMasteryUseCase;
import com.group01.learning.application.usecase.GetLessonUseCase;
import com.group01.learning.application.usecase.SubmitLessonExerciseUseCase;
import com.group01.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.group01.learning.domain.aggregate.LearnerCurriculum;
import com.group01.learning.domain.exception.LearningGateException;
import com.group01.learning.domain.repository.KnowledgePointCatalogRepository;
import com.group01.learning.domain.repository.LearnerCurriculumRepository;
import com.group01.learning.domain.vo.KnowledgePointCatalogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "management.health.rabbit.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
@Testcontainers(disabledWithoutDocker = true)
class LessonSubmissionIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("learning_db")
            .withPassword(UUID.randomUUID().toString());

    private static final byte[] INTERNAL_KEY = new byte[32];
    static {
        new SecureRandom().nextBytes(INTERNAL_KEY);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(INTERNAL_KEY));
    }

    private static final UUID USER = UUID.randomUUID();
    private static final UUID OTHER_USER = UUID.randomUUID();
    private static final UUID TOPIC = UUID.randomUUID();
    private static final UUID OTHER_TOPIC = UUID.randomUUID();
    private static final UUID LESSON = UUID.randomUUID();
    private static final UUID BLOCK = UUID.randomUUID();
    private static final UUID QUESTION = UUID.randomUUID();
    private static final UUID KP = UUID.randomUUID();
    private static final UUID OTHER_KP = UUID.randomUUID();

    @Autowired JdbcTemplate jdbc;
    @Autowired GetLessonUseCase getLessonUseCase;
    @Autowired SubmitLessonExerciseUseCase submitLessonExerciseUseCase;
    @Autowired RefreshLearningTopicsUseCase topics;
    @Autowired GetMasteryUseCase mastery;
    @Autowired ExerciseSubmissionLog submissions;
    @Autowired LearnerLock lock;
    @Autowired LearnerCurriculumRepository curricula;
    @Autowired KnowledgePointCatalogRepository catalogs;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean LearningContentClient content;

    @BeforeEach
    void resetDataAndCurriculum() {
        jdbc.execute("""
                TRUNCATE review_sets, review_items, lesson_exercise_submissions, kp_evidence,
                lesson_progress, topic_progress, knowledge_point_catalog, topic_test_assignments,
                assessment_result_versions RESTART IDENTITY
                """);
        when(content.getTopicSequence()).thenReturn(List.of(
                new LearningContentClient.Topic(TOPIC, "FIRST", "First topic", 900,
                        List.of(knowledgePoint(KP, true))),
                new LearningContentClient.Topic(OTHER_TOPIC, "SECOND", "Second topic", 910,
                        List.of(knowledgePoint(OTHER_KP, false)))));
        when(content.getTopicLessons(TOPIC)).thenReturn(List.of(
                new LearningContentClient.LessonSummary(LESSON, TOPIC, "L1", "First lesson", null,
                        1, List.of(KP), List.of(BLOCK))));
        var question = new LearningContentClient.Question(QUESTION, 1, "Choose the topic sentence",
                List.of(new LearningContentClient.Option("A", "Main idea", 1),
                        new LearningContentClient.Option("B", "Supporting detail", 2)),
                Map.of("type", "CHOICE", "correct", "A"), "A gives the main idea", List.of(KP));
        var block = new LearningContentClient.Block(BLOCK, "EXERCISE", 1, null, null, null,
                List.of(question));
        when(content.getLesson(LESSON)).thenReturn(new LearningContentClient.Lesson(
                LESSON, TOPIC, "L1", "First lesson", null, 1, List.of(KP), List.of(block)));
    }

    @Test
    void hintsOpenOnWrongAnswersPersistAcrossAllAttemptsAndDisappearAfterPassing() {
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        var questions = List.of(fillQuestion(QUESTION, 1, "Find the noun near the contrast."),
                fillQuestion(second, 2, "Look for the phrase after however."),
                fillQuestion(third, 3, "Read the final sentence."));
        hintLesson(List.of(new LearningContentClient.Block(BLOCK, "EXERCISE", 1, null, null, null, questions)));
        topics.execute(USER);
        assertTrue(getLessonUseCase.execute(USER, LESSON).blocks().getFirst().questions().stream().allMatch(q -> q.hint() == null));

        UUID firstRequest = UUID.randomUUID();
        var first = submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, hintCommand(firstRequest, questions, "wrong", "word", "wrong"));
        assertFalse(first.blockPassed());
        assertEquals(questions.getFirst().hint(), first.results().getFirst().hint());
        assertNull(first.results().get(1).hint());
        assertNull(first.results().getFirst().correctAnswer());
        assertNull(first.results().getFirst().explanation());
        assertEquals(Map.of(BLOCK, Set.of(QUESTION, third)), submissions.wrongQuestions(USER, LESSON));
        var firstRead = getLessonUseCase.execute(USER, LESSON).blocks().getFirst().questions();
        assertEquals(questions.getFirst().hint(), firstRead.getFirst().hint());
        assertNull(firstRead.get(1).hint());
        var firstMastery = mastery.execute(USER);

        var secondAttempt = submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK,
                hintCommand(UUID.randomUUID(), questions, "word", "wrong", "wrong"));
        assertFalse(secondAttempt.blockPassed());
        assertTrue(secondAttempt.results().getFirst().correct());
        assertEquals(questions.getFirst().hint(), secondAttempt.results().getFirst().hint());
        assertEquals(questions.get(1).hint(), secondAttempt.results().get(1).hint());
        assertEquals(Map.of(BLOCK, Set.of(QUESTION, second, third)), submissions.wrongQuestions(USER, LESSON));
        assertEquals(questions.stream().map(LearningContentClient.Question::hint).toList(),
                getLessonUseCase.execute(USER, LESSON).blocks().getFirst().questions().stream().map(q -> q.hint()).toList());
        assertEquals(3, count("kp_evidence"));
        assertEquals(firstMastery, mastery.execute(USER));

        var thirdAttempt = submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK,
                hintCommand(UUID.randomUUID(), questions, "wrong", "word", "wrong"));
        assertFalse(thirdAttempt.blockPassed());
        assertTrue(thirdAttempt.results().get(1).correct());
        assertEquals(questions.get(1).hint(), thirdAttempt.results().get(1).hint());

        var passed = submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK,
                hintCommand(UUID.randomUUID(), questions, "word", "word", "word"));
        assertTrue(passed.blockPassed());
        assertTrue(passed.results().stream().allMatch(q -> q.hint() == null));
        assertTrue(passed.results().stream().allMatch(q -> "word".equals(q.correctAnswer()) && q.explanation() != null));
        jdbc.update("UPDATE review_items SET status = 'SKIPPED'");
        var after = getLessonUseCase.execute(USER, LESSON).blocks().getFirst();
        assertTrue(after.questions().stream().allMatch(q -> q.hint() == null));
        assertEquals(3, after.solutions().size());
        assertEquals(3, count("kp_evidence"));
        assertEquals(firstMastery, mastery.execute(USER));

        var wrongAfterPass = submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK,
                hintCommand(UUID.randomUUID(), questions, "wrong", "wrong", "wrong"));
        assertFalse(wrongAfterPass.blockPassed());
        assertTrue(wrongAfterPass.results().stream().allMatch(q -> q.hint() == null));
        assertTrue(getLessonUseCase.execute(USER, LESSON).blocks().getFirst().questions().stream().allMatch(q -> q.hint() == null));

        var edited = List.of(fillQuestion(QUESTION, 1, "Edited hint."), questions.get(1), questions.get(2));
        hintLesson(List.of(new LearningContentClient.Block(BLOCK, "EXERCISE", 1, null, null, null, edited)));
        clearInvocations(content);
        assertEquals(first, submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK,
                hintCommand(firstRequest, questions, "word", "word", "word")));
        verifyNoInteractions(content);
    }

    @Test
    void hintEligibilityAndMissingContentAreAppliedToSubmissionAndLessonRead() {
        var three = new LearningContentClient.Question(UUID.randomUUID(), 1, "Choose one", List.of(
                new LearningContentClient.Option("A", "First", 1), new LearningContentClient.Option("B", "Second", 2),
                new LearningContentClient.Option("C", "Third", 3)), Map.of("type", "CHOICE", "correct", "A"),
                "Explanation", List.of(KP), null, "Compare the scope of the options.");
        var two = new LearningContentClient.Question(UUID.randomUUID(), 2, "Choose one", three.options().subList(0, 2),
                three.answerSpec(), three.explanation(), List.of(KP), null, "This must stay hidden.");
        var tfng = new LearningContentClient.Question(UUID.randomUUID(), 3, "Is the statement supported?", List.of(),
                Map.of("type", "CHOICE", "correct", "TRUE"), "Explanation", List.of(KP), null, "Check what is stated.");
        var noHint = fillQuestion(UUID.randomUUID(), 4, null);
        var questions = List.of(three, two, tfng, noHint);
        hintLesson(List.of(new LearningContentClient.Block(BLOCK, "EXERCISE", 1, null, null, null, questions)));
        topics.execute(USER);
        clearInvocations(content);
        var failed = submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK,
                hintCommand(UUID.randomUUID(), questions, "B", "B", "FALSE", "wrong"));
        assertEquals(three.hint(), failed.results().get(0).hint());
        assertNull(failed.results().get(1).hint());
        assertEquals(tfng.hint(), failed.results().get(2).hint());
        assertNull(failed.results().get(3).hint());
        verify(content, times(1)).getLesson(LESSON);
        clearInvocations(content);
        var read = getLessonUseCase.execute(USER, LESSON).blocks().getFirst().questions();
        assertEquals(three.hint(), read.get(0).hint());
        assertNull(read.get(1).hint());
        assertEquals(tfng.hint(), read.get(2).hint());
        assertNull(read.get(3).hint());
        verify(content, times(1)).getLesson(LESSON);
    }

    @Test
    void wrongQuestionHistoryIsScopedByUserLessonAndBlock() {
        UUID otherBlock = UUID.randomUUID();
        var question = fillQuestion(QUESTION, 1, "Find the relevant phrase.");
        hintLesson(List.of(new LearningContentClient.Block(BLOCK, "EXERCISE", 1, null, null, null, List.of(question)),
                new LearningContentClient.Block(otherBlock, "EXERCISE", 2, null, null, null, List.of(question))));
        topics.execute(USER);
        topics.execute(OTHER_USER);
        submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, hintCommand(UUID.randomUUID(), List.of(question), "wrong"));
        assertEquals(Map.of(BLOCK, Set.of(QUESTION)), submissions.wrongQuestions(USER, LESSON));
        assertTrue(submissions.wrongQuestions(OTHER_USER, LESSON).isEmpty());
        assertTrue(submissions.wrongQuestions(USER, UUID.randomUUID()).isEmpty());
        var own = getLessonUseCase.execute(USER, LESSON);
        assertEquals(question.hint(), own.blocks().getFirst().questions().getFirst().hint());
        assertNull(own.blocks().get(1).questions().getFirst().hint());
        assertTrue(getLessonUseCase.execute(OTHER_USER, LESSON).blocks().stream()
                .flatMap(block -> block.questions().stream()).allMatch(q -> q.hint() == null));
    }

    @Test
    void hintsDoNotChangeFirstAttemptEvidenceOrMastery() {
        var withHint = fillQuestion(QUESTION, 1, "Find the relevant phrase.");
        hintLesson(List.of(new LearningContentClient.Block(BLOCK, "EXERCISE", 1, null, null, null, List.of(withHint))));
        topics.execute(USER);
        topics.execute(OTHER_USER);
        submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, hintCommand(UUID.randomUUID(), List.of(withHint), "wrong"));
        var withoutHint = fillQuestion(QUESTION, 1, null);
        hintLesson(List.of(new LearningContentClient.Block(BLOCK, "EXERCISE", 1, null, null, null, List.of(withoutHint))));
        submitLessonExerciseUseCase.execute(OTHER_USER, LESSON, BLOCK, hintCommand(UUID.randomUUID(), List.of(withoutHint), "wrong"));
        assertEquals(mastery.execute(USER), mastery.execute(OTHER_USER));
        var before = mastery.execute(USER);
        submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, hintCommand(UUID.randomUUID(), List.of(withHint), "word"));
        assertEquals(before, mastery.execute(USER));
        assertEquals(2, count("kp_evidence"));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE correct = false", Integer.class));
    }

    private LearningContentClient.Question fillQuestion(UUID id, int order, String hint) {
        return new LearningContentClient.Question(id, order, "Complete one word", null,
                Map.of("type", "FILL", "accepted", List.of("word")), "Explanation", List.of(KP), null, hint);
    }

    private void hintLesson(List<LearningContentClient.Block> blocks) {
        when(content.getTopicLessons(TOPIC)).thenReturn(List.of(new LearningContentClient.LessonSummary(
                LESSON, TOPIC, "L1", "First lesson", null, 1, List.of(KP), blocks.stream()
                .map(LearningContentClient.Block::blockId).toList())));
        when(content.getLesson(LESSON)).thenReturn(new LearningContentClient.Lesson(
                LESSON, TOPIC, "L1", "First lesson", null, 1, List.of(KP), blocks));
    }

    private SubmitExerciseCommand hintCommand(UUID request, List<LearningContentClient.Question> questions,
                                               String... answers) {
        List<SubmitExerciseCommand.Answer> results = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            results.add(new SubmitExerciseCommand.Answer(questions.get(i).questionVersionId(), answers[i]));
        }
        return new SubmitExerciseCommand(request, results);
    }

    @Test
    void failedFirstAttemptThenCorrectRetryCompletesLessonAndReplaysAfterReviewGate() {
        topics.execute(USER);
        UUID firstRequest = UUID.randomUUID();
        SubmissionResult failed = submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command(firstRequest, "B"));
        assertFalse(failed.blockPassed());
        assertFalse(failed.lessonCompleted());
        assertFalse(failed.results().getFirst().correct());
        assertNull(failed.results().getFirst().correctAnswer());
        assertNull(failed.results().getFirst().explanation());
        assertEquals(Boolean.FALSE, jdbc.queryForObject(
                "SELECT correct FROM kp_evidence WHERE user_id = ? AND kp_id = ?", Boolean.class, USER, KP));
        assertEquals(failed, submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command(firstRequest, "A")));

        UUID retryRequest = UUID.randomUUID();
        SubmissionResult passed = submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command(retryRequest, "A"));
        assertTrue(passed.blockPassed());
        assertTrue(passed.lessonCompleted());
        assertEquals("A", passed.results().getFirst().correctAnswer());
        assertEquals("A gives the main idea", passed.results().getFirst().explanation());
        assertEquals(1, count("kp_evidence"));
        assertEquals(2, count("lesson_exercise_submissions"));
        assertEquals("lesson_exercise", jdbc.queryForObject("SELECT source FROM kp_evidence", String.class));
        assertNotNull(jdbc.queryForObject("SELECT completed_at FROM lesson_progress WHERE user_id = ?",
                Timestamp.class, USER));
        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM review_items", String.class));
        assertEquals(KP, jdbc.queryForObject("SELECT knowledge_point_id FROM review_items", UUID.class));
        assertEquals(LESSON, jdbc.queryForObject("SELECT lesson_id FROM review_items", UUID.class));

        clearInvocations(content);
        assertEquals(failed, submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command(firstRequest, "A")));
        assertEquals(passed, submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command(retryRequest, "B")));
        verifyNoInteractions(content);
        assertEquals(1, count("kp_evidence"));
        assertEquals(2, count("lesson_exercise_submissions"));
        assertEquals(1, count("review_items"));

        LearningGateException gate = assertThrows(LearningGateException.class,
                () -> submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command(UUID.randomUUID(), "A")));
        assertEquals("REVIEW_REQUIRED", gate.getCode());
        assertEquals(KP, gate.getReviews().getFirst().knowledgePointId());
        MasteryResult result = mastery.execute(USER).stream()
                .filter(item -> item.knowledgePointId().equals(KP)).findFirst().orElseThrow();
        assertEquals(0.0, result.mastery());
        assertEquals(1, result.evidenceCount());
    }

    enum ChangedScope { USER, LESSON, BLOCK }

    @ParameterizedTest
    @EnumSource(ChangedScope.class)
    void requestIdCannotBeReusedInAnotherScope(ChangedScope scope) {
        topics.execute(USER);
        UUID request = UUID.randomUUID();
        submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command(request, "B"));
        UUID user = scope == ChangedScope.USER ? OTHER_USER : USER;
        UUID lesson = scope == ChangedScope.LESSON ? UUID.randomUUID() : LESSON;
        UUID block = scope == ChangedScope.BLOCK ? UUID.randomUUID() : BLOCK;
        clearInvocations(content);

        LearningRequestException conflict = assertThrows(LearningRequestException.class,
                () -> submitLessonExerciseUseCase.execute(user, lesson, block, command(request, "A")));
        assertEquals(409, conflict.getStatus());
        assertEquals("REQUEST_CONFLICT", conflict.getCode());
        assertEquals(1, count("lesson_exercise_submissions"));
        assertEquals(1, count("kp_evidence"));
        verifyNoInteractions(content);
    }

    static Stream<List<SubmitExerciseCommand.Answer>> incompleteAnswers() {
        return Stream.of(List.of(),
                List.of(new SubmitExerciseCommand.Answer(QUESTION, "A"),
                        new SubmitExerciseCommand.Answer(QUESTION, "A")),
                List.of(new SubmitExerciseCommand.Answer(UUID.randomUUID(), "A")));
    }

    @ParameterizedTest
    @MethodSource("incompleteAnswers")
    void missingDuplicateOrForeignAnswersDoNotWriteProgress(List<SubmitExerciseCommand.Answer> answers) {
        topics.execute(USER);
        var command = new SubmitExerciseCommand(UUID.randomUUID(), answers);
        LearningRequestException invalid = assertThrows(LearningRequestException.class,
                () -> submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command));
        assertEquals(422, invalid.getStatus());
        assertEquals("INVALID_ANSWERS", invalid.getCode());
        assertEquals(0, count("lesson_exercise_submissions"));
        assertEquals(0, count("kp_evidence"));
        assertEquals(0, count("lesson_progress"));
    }

    @Test
    void submissionRefreshesLessonMetadataWithoutAPriorLessonRead() {
        topics.execute(USER);
        jdbc.update("""
                INSERT INTO lesson_progress (user_id, lesson_id, topic_id, lesson_sort_order, knowledge_point_ids)
                VALUES (?, ?, ?, 99, ARRAY[CAST(? AS uuid)])
                """, USER, LESSON, OTHER_TOPIC, OTHER_KP.toString());

        SubmissionResult response = submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command(UUID.randomUUID(), "A"));
        assertTrue(response.lessonCompleted());
        assertEquals(TOPIC, jdbc.queryForObject("SELECT topic_id FROM lesson_progress", UUID.class));
        assertEquals(1, jdbc.queryForObject("SELECT lesson_sort_order FROM lesson_progress", Integer.class));
        assertEquals(KP, jdbc.queryForObject("SELECT knowledge_point_ids[1] FROM lesson_progress", UUID.class));
        assertEquals(1, jdbc.queryForObject("SELECT cardinality(knowledge_point_ids) FROM lesson_progress", Integer.class));
        assertEquals(0, count("review_items"));
        verify(content, times(1)).getLesson(LESSON);
    }

    @Test
    void masteryUsesLatestFiveOrdinalsCountsAllEvidenceAndIsolatesUsers() {
        topics.execute(USER);
        appendEvidence(USER, KP, List.of(true, true, false, false, true, false, true));
        appendEvidence(OTHER_USER, KP, List.of(true));

        Map<UUID, MasteryResult> byKp = mastery.execute(USER).stream().collect(
                java.util.stream.Collectors.toMap(MasteryResult::knowledgePointId, item -> item));
        assertEquals(2, byKp.size());
        assertEquals(TOPIC, byKp.get(KP).topicId());
        assertEquals(0.4625, byKp.get(KP).mastery(), 1e-12);
        assertEquals(7, byKp.get(KP).evidenceCount());
        assertEquals(0.0, byKp.get(OTHER_KP).mastery());
        assertEquals(0, byKp.get(OTHER_KP).evidenceCount());
        MasteryResult other = mastery.execute(OTHER_USER).stream()
                .filter(item -> item.knowledgePointId().equals(KP)).findFirst().orElseThrow();
        assertEquals(0.5, other.mastery());
        assertEquals(1, other.evidenceCount());
    }

    @Test
    void concurrentDuplicateRequestsProduceOneSubmissionAndOneEvidenceItem() throws Exception {
        topics.execute(USER);
        var command = command(UUID.randomUUID(), "A");
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> {
                start.await();
                return submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command);
            });
            var second = executor.submit(() -> {
                start.await();
                return submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command);
            });
            start.countDown();
            assertEquals(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, count("lesson_exercise_submissions"));
        assertEquals(1, count("kp_evidence"));
        assertEquals(1, jdbc.queryForObject("SELECT cardinality(passed_block_ids) FROM lesson_progress", Integer.class));
        assertNotNull(jdbc.queryForObject("SELECT completed_at FROM lesson_progress", Timestamp.class));
    }

    @Test
    void concurrentUsersCanRefreshSharedCatalogInOppositeContentOrders() throws Exception {
        List<KnowledgePointCatalogEntry> catalog = Stream.generate(() ->
                        new KnowledgePointCatalogEntry(UUID.randomUUID(), TOPIC, true))
                .limit(32).toList();
        List<KnowledgePointCatalogEntry> reversed = new ArrayList<>(catalog);
        Collections.reverse(reversed);
        var ready = new CountDownLatch(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> refreshCatalog(USER, catalog, ready));
            var second = executor.submit(() -> refreshCatalog(OTHER_USER, reversed, ready));
            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        assertEquals(32, count("knowledge_point_catalog"));
        assertEquals(2, count("topic_progress"));
        assertEquals(32, mastery.execute(USER).size());
        assertEquals(32, mastery.execute(OTHER_USER).size());
    }

    @Test
    void reviewInsertFailureRollsBackPassingRetryAndCompletion() {
        topics.execute(USER);
        submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command(UUID.randomUUID(), "B"));
        UUID retry = UUID.randomUUID();
        jdbc.execute("ALTER TABLE review_items ADD CONSTRAINT reject_pending_review CHECK (status <> 'PENDING')");
        try {
            assertThrows(DataIntegrityViolationException.class,
                    () -> submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command(retry, "A")));
            assertEquals(1, count("lesson_exercise_submissions"));
            assertEquals(1, count("kp_evidence"));
            assertEquals(0, count("review_items"));
            assertEquals(0, jdbc.queryForObject("SELECT cardinality(passed_block_ids) FROM lesson_progress", Integer.class));
            assertNull(jdbc.queryForObject("SELECT completed_at FROM lesson_progress", Timestamp.class));
        } finally {
            jdbc.execute("ALTER TABLE review_items DROP CONSTRAINT reject_pending_review");
        }
        assertTrue(submitLessonExerciseUseCase.execute(USER, LESSON, BLOCK, command(retry, "A")).lessonCompleted());
        assertEquals(1, count("review_items"));
        assertEquals(2, count("lesson_exercise_submissions"));
        assertEquals(1, count("kp_evidence"));
    }

    private LearningContentClient.KnowledgePoint knowledgePoint(UUID id, boolean practice) {
        return new LearningContentClient.KnowledgePoint(id, "KP", "Knowledge point", "PROCEDURE",
                "READING", null, practice);
    }

    private SubmitExerciseCommand command(UUID request, String answer) {
        return new SubmitExerciseCommand(request, List.of(new SubmitExerciseCommand.Answer(QUESTION, answer)));
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private void appendEvidence(UUID user, UUID kp, List<Boolean> correctness) {
        List<Object[]> rows = new ArrayList<>();
        Instant timestamp = Instant.parse("2026-10-01T12:00:00Z");
        for (int index = 0; index < correctness.size(); index++) {
            rows.add(new Object[]{UUID.randomUUID(), user, kp, correctness.get(index), UUID.randomUUID(),
                    Timestamp.from(timestamp.minusSeconds(index))});
        }
        jdbc.batchUpdate("""
                INSERT INTO kp_evidence (id, user_id, kp_id, correct, source, source_reference_id, created_at)
                VALUES (?, ?, ?, ?, 'assessment', ?, ?)
                """, rows);
    }

    private void refreshCatalog(UUID user, List<KnowledgePointCatalogEntry> catalog, CountDownLatch ready) {
        new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            lock.lock(user);
            ready.countDown();
            try {
                if (!ready.await(20, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent refresh did not start");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Concurrent refresh interrupted", exception);
            }
            LearnerCurriculum curriculum = curricula.find(user);
            curriculum.reorder(List.of(TOPIC));
            curricula.save(curriculum);
            catalogs.upsert(catalog);
        });
    }
}
