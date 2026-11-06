package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePointMapping;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.port.LearningContentClient.Section;
import com.ieltspath.learning.application.port.SpeechAnalysisClient;
import com.ieltspath.learning.application.service.SpeakingGrader;
import com.ieltspath.learning.application.service.SpeakingPromptAssembler;
import com.ieltspath.learning.application.service.SpeakingResultAssembler;
import com.ieltspath.learning.application.usecase.CompleteLessonUseCase;
import com.ieltspath.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.ieltspath.learning.application.usecase.CompleteSpeakingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.CreateLessonSpeakingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.CreateSpeakingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.SettleSpeakingSubmissionUseCase;
import com.ieltspath.learning.application.usecase.StartPracticeAttemptUseCase;
import com.ieltspath.learning.application.usecase.SubmitPracticeAttemptUseCase;
import com.ieltspath.learning.application.usecase.UploadSpeakingRecordingUseCase;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import com.ieltspath.learning.domain.vo.SpeechSegment;
import com.ieltspath.learning.domain.vo.SpeakingSubmissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * The whole path of a recorded answer in a lesson's practice set, on a real database: open the attempt, record, grade,
 * charge, submit the attempt, and the grade reaches mastery. Only the outside services are replaced (the speech service,
 * the language model, object storage, points and content); every use case and table is the real one. A free Speaking test
 * answered with the same grade must not reach mastery.
 */
@SpringBootTest(properties = {"spring.config.import=", "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false", "management.health.rabbit.enabled=false",
        "learning.messaging.consumer-enabled=false", "app.auth.internal-jwt-issuer=urn:code-base:api-gateway",
        "learning.speaking.enabled=true", "learning.speaking.job-delay-ms=3600000"})
@Testcontainers(disabledWithoutDocker = true)
class SpeakingLessonMasteryIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
    private static final byte[] KEY = new byte[32];
    static { new SecureRandom().nextBytes(KEY); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    @MockitoBean LearningContentClient content;
    @MockitoBean AccessClient access;
    @MockitoBean ObjectStorage storage;
    @MockitoBean SpeechAnalysisClient speech;
    @MockitoBean SpeakingGrader grader;

    @Autowired JdbcTemplate jdbc;
    @Autowired CompleteLessonUseCase completeLesson;
    @Autowired StartPracticeAttemptUseCase startAttempt;
    @Autowired CreateLessonSpeakingSubmissionUseCase createForAttempt;
    @Autowired CreateSpeakingSubmissionUseCase createFree;
    @Autowired UploadSpeakingRecordingUseCase upload;
    @Autowired CompleteSpeakingSubmissionUseCase complete;
    @Autowired SettleSpeakingSubmissionUseCase settle;
    @Autowired SubmitPracticeAttemptUseCase submitAttempt;
    @Autowired com.ieltspath.learning.infrastructure.scheduler.SpeakingGradingJob job;
    @Autowired RefreshLearningTopicsUseCase topics;

    private static final UUID USER = UUID.randomUUID();
    private static final UUID TOPIC = UUID.randomUUID();
    private static final UUID LESSON = UUID.randomUUID();
    private static final UUID PACKAGE = UUID.randomUUID();
    private static final UUID VERSION = UUID.randomUUID();
    private static final UUID SECTION = UUID.randomUUID();
    private static final UUID QUESTION = UUID.randomUUID();
    private static final UUID KP = UUID.randomUUID();
    private static final UUID FC = UUID.randomUUID(), LR = UUID.randomUUID(), GRA = UUID.randomUUID(),
            PR = UUID.randomUUID(), TRAP = UUID.randomUUID();

    /** One lesson of the Speaking track with its practice set of a single recorded question. */
    private record Spoken(UUID lesson, UUID pkg, UUID version, UUID section, UUID question, int position) { }

    private static final Spoken FIRST = new Spoken(LESSON, PACKAGE, VERSION, SECTION, QUESTION, 1);
    private static final Spoken SECOND = new Spoken(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID(), 2);
    private static final Spoken THIRD = new Spoken(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID(), 3);
    private static final List<Spoken> LESSONS = List.of(FIRST, SECOND, THIRD);

    @BeforeEach
    void seed() {
        jdbc.execute("""
                TRUNCATE speaking_recordings, speaking_analyses, speaking_grading_contexts, speaking_submissions,
                kp_evidence, lesson_progress, topic_progress, knowledge_point_catalog, kp_mastery_profiles,
                question_attempt_events, review_items, llm_daily_usage, practice_attempts RESTART IDENTITY CASCADE
                """);
        reset(content, access, storage, speech, grader);
        // The criterion points and the trap point belong to the topic but to no lesson, as in the course.
        when(content.getTopicSequence()).thenReturn(List.of(new LearningContentClient.Topic(TOPIC, "SPEAKING",
                "Speaking", 900, null, List.of(kp(KP), kp(FC), kp(LR), kp(GRA), kp(PR),
                new LearningContentClient.KnowledgePoint(TRAP, "SPK_TRAP", "Trap", "PROCEDURE", "SPEAKING", "", false,
                        null, null, KP, "SPK_NO_REASON")), LearningSkill.SPEAKING, true)));
        when(content.getTopicLessons(TOPIC)).thenReturn(LESSONS.stream().map(set ->
                new LearningContentClient.LessonSummary(set.lesson(), TOPIC, "S55-" + set.position(),
                        "Part " + set.position(), null, set.position(), List.of(KP), List.of())).toList());
        LearningContentClientTestSupport.stubLessonAccessSummaries(content);
        for (Spoken set : LESSONS) {
            Item item = new Item(set.question(), 1, "Describe what you usually do at weekends.", null,
                    Map.of("type", "SPEAKING", "durationSeconds", 60, "passBand", 5.5), "Answer, then a reason.", "Hint",
                    BigDecimal.ONE, List.of(new KnowledgePointMapping(KP, BigDecimal.ONE)));
            when(content.getLesson(set.lesson())).thenReturn(new LearningContentClient.Lesson(set.lesson(), TOPIC,
                    "S55-" + set.position(), "Part " + set.position(), null, set.position(), List.of(KP), List.of(),
                    LearningSkill.SPEAKING));
            when(content.lessonPracticeSets(set.lesson())).thenReturn(List.of(new LearningContentClient.LessonPracticeSet(
                    set.pkg(), set.version(), "SPK-REC-" + set.position(), "Nói Part " + set.position(), 1, List.of(KP),
                    null, Set.of(LearningSkill.SPEAKING))));
            when(content.getPackageVersion(set.version())).thenReturn(new PackageVersion(set.version(), set.pkg(),
                    "PRACTICE_SET", TOPIC, Map.of(),
                    List.of(new Section(set.section(), "Speak", "SPEAKING", null, 1, null, List.of(item)))));
        }
        // Every criterion point has an unseen review set, and the trap point's parent has a set with trap questions.
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenAnswer(invocation ->
                invocation.<List<UUID>>getArgument(0).stream().collect(java.util.stream.Collectors.toMap(p -> p, p -> 3)));
        when(content.searchPracticeSets(eq(KP), anyList(), eq(3), eq(1), any(), eq("SPK_NO_REASON"))).thenReturn(List.of(
                new LearningContentClient.PracticeSet(UUID.randomUUID(), UUID.randomUUID(), "TRAP_SET", 3, 3, null, null, 2)));
        when(content.getActiveRubric("SPEAKING")).thenReturn(new LearningContentClient.Rubric("SPEAKING_V1", "SPEAKING", 1,
                "", List.of(), List.of()));
        topics.execute(USER);
        when(access.balance()).thenReturn(100L);
        when(access.debit(any(), anyInt(), anyString(), any(), anyString(), anyString())).thenAnswer(invocation -> UUID.randomUUID());
        when(speech.available()).thenReturn(true);
        when(grader.available()).thenReturn(true);
        when(speech.analyze(any(), any())).thenReturn(new SpeechAnalysis(List.of(new SpeechSegment("I like walking.",
                80, 80, 80.0, 80, 80, 0.9, List.of(new com.ieltspath.learning.domain.vo.SpokenWord("I", 0, 300, 80, null))))));
        when(storage.download(anyString(), anyString())).thenReturn(wav());
        when(grader.prepare(any(), any())).thenReturn(new SpeakingPromptAssembler.Prepared(
                new LearningContentClient.Rubric("SPEAKING_V1", "SPEAKING", 1, "", List.of(), List.of()),
                "a".repeat(64), "system", "user"));
    }

    /** The grader's answer: a band, and whether the speech could be trusted as evidence. */
    private void gradeWith(BigDecimal band, boolean usable) {
        // As the real grader does for one answer: criterion bands, and no overall band.
        var criteria = List.of(Map.of("code", "FC", "band", band), Map.of("code", "LR", "band", band),
                Map.of("code", "GRA", "band", band), Map.of("code", "PR", "band", band));
        when(grader.grade(any(), any())).thenReturn(new SpeakingResultAssembler.Assembled(
                Map.of("display", Map.of("criteria", criteria), "signals", Map.of("usableAsEvidence", usable)), null));
    }

    private static final Map<String, UUID> POINTS = Map.of("FC", FC, "LR", LR, "GRA", GRA, "PR", PR);

    /** The grader's answer with each criterion's band and point and the errors it marked. */
    private void gradeWithSignals(Map<String, String> bands, List<String> errors, boolean usable) {
        List<Map<String, Object>> display = new java.util.ArrayList<>();
        List<Map<String, Object>> signalRows = new java.util.ArrayList<>();
        bands.forEach((code, band) -> {
            display.add(Map.of("code", code, "band", new BigDecimal(band)));
            signalRows.add(Map.of("code", code, "band", new BigDecimal(band), "kpId", POINTS.get(code).toString()));
        });
        when(grader.grade(any(), any())).thenReturn(new SpeakingResultAssembler.Assembled(
                Map.of("display", Map.of("criteria", display), "signals", Map.of("usableAsEvidence", usable,
                        "criteria", signalRows, "errors", errors.stream()
                        .map(code -> Map.<String, Object>of("code", code, "partNo", 1, "excerpt", "x")).toList())), null));
    }

    private static Map<String, String> bands(String fc, String lr, String gra, String pr) {
        return Map.of("FC", fc, "LR", lr, "GRA", gra, "PR", pr);
    }

    /** Opens the set's attempt after finishing the lessons before it, records one answer and has it graded. */
    private UUID recordedAttempt(Spoken set) {
        for (Spoken lesson : LESSONS) {
            completeLesson.execute(USER, lesson.lesson());
            if (lesson == set) break;
        }
        UUID attempt = startAttempt.execute(USER, set.lesson(), set.pkg()).attemptId();
        UUID submission = createForAttempt.execute(USER, attempt, UUID.randomUUID(), true).id();
        upload.execute(USER, submission, set.question(), "audio/wav", wav(), 10_000);
        complete.execute(USER, submission);
        job.runOne();
        settle.execute(USER, submission);
        return attempt;
    }

    private com.ieltspath.learning.domain.vo.PracticeSubmission submit(UUID attempt) {
        return submitAttempt.execute(USER, attempt, new SubmitExerciseCommand(UUID.randomUUID(), List.of()));
    }

    /** One graded recorded answer submitted for the set. */
    private com.ieltspath.learning.domain.vo.PracticeSubmission speak(Spoken set, Map<String, String> bands,
                                                                       List<String> errors) {
        gradeWithSignals(bands, errors, true);
        return submit(recordedAttempt(set));
    }

    private long reviews(String condition, Object... arguments) {
        Object[] all = new Object[arguments.length + 1];
        all[0] = USER;
        System.arraycopy(arguments, 0, all, 1, arguments.length);
        return jdbc.queryForObject("SELECT count(*) FROM review_items WHERE user_id = ? AND " + condition, Long.class, all);
    }

    private Boolean evidenceOf(UUID point, String source) {
        return jdbc.queryForObject("SELECT correct FROM kp_evidence WHERE user_id = ? AND kp_id = ? AND source = ?",
                Boolean.class, USER, point, source);
    }

    /** A 16 kHz, 16-bit, mono WAV of a tenth of a second: what the browser sends. */
    private static byte[] wav() {
        int dataBytes = 3200;
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes()).putInt(36 + dataBytes).put("WAVE".getBytes())
                .put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1).putInt(16000)
                .putInt(32000).putShort((short) 2).putShort((short) 16)
                .put("data".getBytes()).putInt(dataBytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(header.array());
        out.writeBytes(new byte[dataBytes]);
        return out.toByteArray();
    }

    /** Opens, records, grades and charges one recorded answer of the attempt; returns the attempt's id. */
    private UUID gradedAnswerInAttempt(BigDecimal band, boolean usable) {
        gradeWith(band, usable);
        completeLesson.execute(USER, LESSON);
        UUID attempt = startAttempt.execute(USER, LESSON, PACKAGE).attemptId();
        UUID submission = createForAttempt.execute(USER, attempt, UUID.randomUUID(), true).id();
        return recordGradeAndSettle(submission, attempt);
    }

    private UUID recordGradeAndSettle(UUID submission, UUID attempt) {
        upload.execute(USER, submission, QUESTION, "audio/wav", wav(), 10_000);
        complete.execute(USER, submission);
        job.runOne();
        settle.execute(USER, submission);
        return attempt;
    }

    private long evidenceRows(String source) {
        return jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ? AND source = ?", Long.class, USER, source);
    }

    @Test
    void aGradedRecordedAnswerReachesMasteryOnlyWhenTheAttemptIsSubmitted() {
        UUID attempt = gradedAnswerInAttempt(new BigDecimal("6.5"), true);
        assertThat(evidenceRows("lesson_speaking")).as("nothing before the attempt is submitted").isZero();

        var submitted = submitAttempt.execute(USER, attempt, new SubmitExerciseCommand(UUID.randomUUID(), List.of()));

        assertThat(submitted.passed()).isTrue();
        assertThat(submitted.countedAsEvidence()).isTrue();
        assertThat(jdbc.queryForObject("SELECT correct FROM kp_evidence WHERE user_id = ? AND source = 'lesson_speaking' AND kp_id = ?",
                Boolean.class, USER, KP)).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM kp_mastery_profiles WHERE user_id = ? AND knowledge_point_id = ?",
                Long.class, USER, KP)).isPositive();
    }

    @Test
    void aBandUnderThePassBandIsWeakEvidenceOfTheSamePoint() {
        UUID attempt = gradedAnswerInAttempt(new BigDecimal("4.5"), true);

        var submitted = submitAttempt.execute(USER, attempt, new SubmitExerciseCommand(UUID.randomUUID(), List.of()));

        assertThat(submitted.passed()).isFalse();
        assertThat(jdbc.queryForObject("SELECT correct FROM kp_evidence WHERE user_id = ? AND source = 'lesson_speaking'",
                Boolean.class, USER)).isFalse();
    }

    @Test
    void speechThatCannotBeTrustedWritesNoMastery() {
        UUID attempt = gradedAnswerInAttempt(new BigDecimal("6.5"), false);

        submitAttempt.execute(USER, attempt, new SubmitExerciseCommand(UUID.randomUUID(), List.of()));

        assertThat(evidenceRows("lesson_speaking")).isZero();
    }

    @Test
    void theAttemptCannotBeSubmittedWhileTheRecordingIsStillBeingGraded() {
        gradeWith(new BigDecimal("6.5"), true);
        completeLesson.execute(USER, LESSON);
        UUID attempt = startAttempt.execute(USER, LESSON, PACKAGE).attemptId();
        UUID submission = createForAttempt.execute(USER, attempt, UUID.randomUUID(), true).id();
        upload.execute(USER, submission, QUESTION, "audio/wav", wav(), 10_000);
        complete.execute(USER, submission);

        assertThatThrownBy(() -> submitAttempt.execute(USER, attempt, new SubmitExerciseCommand(UUID.randomUUID(), List.of())))
                .isInstanceOfSatisfying(LearningRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("SPEAKING_NOT_GRADED"));
        assertThat(evidenceRows("lesson_speaking")).isZero();
    }

    @Test
    void aFreeSpeakingPracticeOfTheSameTestNeverReachesMastery() {
        gradeWith(new BigDecimal("6.5"), true);
        when(speech.analyze(any(), any())).thenReturn(new SpeechAnalysis(List.of(new SpeechSegment("Yes.", 80, 80, 80.0,
                80, 80, 0.9, List.of(new com.ieltspath.learning.domain.vo.SpokenWord("Yes", 0, 300, 80, null))))));
        UUID submission = createFree.execute(USER, UUID.randomUUID(), VERSION, List.of(), true).id();
        recordGradeAndSettle(submission, null);

        assertThat(jdbc.queryForObject("SELECT status FROM speaking_submissions WHERE id = ?", String.class, submission))
                .isEqualTo(SpeakingSubmissionStatus.GRADED.name());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM speaking_submissions WHERE id = ? AND lesson_id IS NULL",
                Long.class, submission)).isEqualTo(1L);
        assertThat(evidenceRows("lesson_speaking")).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ?", Long.class, USER)).isZero();
    }

    @Test
    void eachCriterionIsEvidenceForItsOwnPointAndOnlyTheWeakOneLeavesAnOptionalSpeakingReview() {
        speak(FIRST, bands("4.5", "6.5", "6.5", "6.5"), List.of());

        assertThat(evidenceOf(FC, "lesson_speaking")).isFalse();
        assertThat(evidenceOf(LR, "lesson_speaking")).isTrue();
        assertThat(evidenceOf(GRA, "lesson_speaking")).isTrue();
        assertThat(evidenceOf(PR, "lesson_speaking")).isTrue();
        assertThat(evidenceOf(KP, "lesson_speaking")).as("mean 6.0 reaches the pass band").isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM kp_mastery_profiles WHERE user_id = ? AND knowledge_point_id = ?",
                Long.class, USER, FC)).isEqualTo(1L);
        assertThat(reviews("knowledge_point_id = ? AND trigger_kind = 'SPEAKING_CRITERION' AND skill = 'SPEAKING' "
                + "AND status = 'PENDING' AND blocking = false AND lesson_id = ?", FC, LESSON)).isEqualTo(1L);
        assertThat(reviews("true")).as("no review for a criterion that passed").isEqualTo(1L);
    }

    @Test
    void anAnswerThatPassesEveryCriterionLeavesNoReview() {
        speak(FIRST, bands("6.5", "6.0", "5.5", "7.0"), List.of());

        assertThat(evidenceRows("lesson_speaking")).isEqualTo(5L);
        assertThat(reviews("true")).isZero();
    }

    @Test
    void theTwoWeakestCriteriaGetReviewsAndTheOthersWait() {
        speak(FIRST, bands("5.0", "4.0", "4.5", "6.5"), List.of());

        assertThat(jdbc.queryForList("SELECT knowledge_point_id FROM review_items WHERE user_id = ? AND "
                + "trigger_kind = 'SPEAKING_CRITERION'", UUID.class, USER)).containsExactlyInAnyOrder(LR, GRA);
    }

    @Test
    void aCriterionWithoutAnUnseenReviewSetGetsNoReviewButItsEvidenceStays() {
        when(content.practiceSetAvailability(anyList(), anyList(), eq(3))).thenReturn(Map.of(FC, 0));

        speak(FIRST, bands("4.5", "6.5", "6.5", "6.5"), List.of());

        assertThat(evidenceOf(FC, "lesson_speaking")).isFalse();
        assertThat(reviews("true")).isZero();
    }

    @Test
    void speechTheGraderCouldNotTrustMakesNeitherCriterionEvidenceNorTrapsNorReviews() {
        gradeWithSignals(bands("3.0", "3.0", "3.0", "3.0"), List.of("SPK_NO_REASON"), false);

        submit(recordedAttempt(FIRST));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ?", Long.class, USER)).isZero();
        assertThat(reviews("true")).isZero();
    }

    @Test
    void aCriterionWeakAgainWhileItsReviewWaitsStaysOptionalAndNeverBlocksTheNextLesson() {
        speak(FIRST, bands("4.5", "6.5", "6.5", "6.5"), List.of());

        var second = speak(SECOND, bands("5.0", "6.5", "6.5", "6.5"), List.of());

        assertThat(reviews("knowledge_point_id = ?", FC)).as("the same review, not a second one").isEqualTo(1L);
        assertThat(reviews("knowledge_point_id = ? AND blocking = false AND status = 'PENDING' "
                + "AND trigger_kind = 'SPEAKING_CRITERION'", FC)).as("AI grades suggest, they never block").isEqualTo(1L);
        assertThat(reviews("blocking = true")).isZero();
        assertThat(second.reviewsCreated()).as("the review already waits, so none is created").isEmpty();
        assertThatNoException().isThrownBy(() -> completeLesson.execute(USER, THIRD.lesson()));
    }

    @Test
    void aWritingReviewOfThePointIsNeverEscalatedByAWeakSpeakingAnswer() {
        jdbc.update("INSERT INTO review_items (id, user_id, knowledge_point_id, lesson_id, status, skill, trigger_kind, "
                + "blocking, source_submission_id) VALUES (?, ?, ?, ?, 'PENDING', 'WRITING', 'WRITING_CRITERION', FALSE, ?)",
                UUID.randomUUID(), USER, FC, LESSON, UUID.randomUUID());

        speak(FIRST, bands("4.5", "6.5", "6.5", "6.5"), List.of());

        assertThat(reviews("knowledge_point_id = ? AND blocking = true", FC)).as("a weak speaking answer never blocks").isZero();
        assertThat(reviews("knowledge_point_id = ? AND trigger_kind = 'WRITING_CRITERION' AND blocking = false "
                + "AND status = 'PENDING'", FC)).as("the writing review is left as it was").isEqualTo(1L);
    }

    @Test
    void aDifferentCriterionWeakTheSecondTimeGetsItsOwnOptionalReviewAndTheFirstStaysOptional() {
        speak(FIRST, bands("4.5", "6.5", "6.5", "6.5"), List.of());

        speak(SECOND, bands("6.5", "6.5", "4.5", "6.5"), List.of());

        assertThat(reviews("knowledge_point_id = ? AND blocking = false", FC)).isEqualTo(1L);
        assertThat(reviews("knowledge_point_id = ? AND blocking = false AND trigger_kind = 'SPEAKING_CRITERION'", GRA))
                .isEqualTo(1L);
        assertThat(reviews("blocking = true")).isZero();
    }

    @Test
    void aReviewDoneAndThenAWeakGradeAgainStartsAFreshOptionalReview() {
        speak(FIRST, bands("4.5", "6.5", "6.5", "6.5"), List.of());
        jdbc.update("UPDATE review_items SET status = 'DONE', done_at = now() WHERE user_id = ? AND knowledge_point_id = ?",
                USER, FC);

        speak(SECOND, bands("4.5", "6.5", "6.5", "6.5"), List.of());

        assertThat(reviews("knowledge_point_id = ? AND status = 'DONE'", FC)).isEqualTo(1L);
        assertThat(reviews("knowledge_point_id = ? AND status = 'PENDING' AND blocking = false", FC)).isEqualTo(1L);
        assertThat(reviews("blocking = true")).isZero();
    }

    @Test
    void aSecondAttemptOfASetAlreadyCountedAddsNoEvidenceAndNoReview() {
        speak(FIRST, bands("6.5", "6.5", "6.5", "6.5"), List.of());
        long before = jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ?", Long.class, USER);

        gradeWithSignals(bands("3.0", "3.0", "3.0", "3.0"), List.of("SPK_NO_REASON"), true);
        UUID repeat = recordedAttempt(FIRST);
        var result = submit(repeat);

        assertThat(result.countedAsEvidence()).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ?", Long.class, USER)).isEqualTo(before);
        assertThat(reviews("true")).isZero();
    }

    @Test
    void aMarkedErrorIsEvidenceAgainstItsTrapPointAndOnlyKnownCodesCount() {
        speak(FIRST, bands("6.5", "6.5", "6.5", "6.5"), List.of("SPK_NO_REASON", "SPK_NO_REASON", "SPK_NOT_IN_CATALOG"));

        assertThat(evidenceOf(TRAP, "error_trap")).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM kp_evidence WHERE user_id = ? AND source = 'error_trap'",
                Long.class, USER)).as("once per trap point, nothing for an unknown code").isEqualTo(1L);
    }

    @Test
    void aTrapPointThatIsWeakMakesAnOptionalReviewOfItsParentPoint() {
        speak(FIRST, bands("6.5", "6.5", "6.5", "6.5"), List.of("SPK_NO_REASON"));

        assertThat(reviews("knowledge_point_id = ? AND trigger_kind = 'ERROR_PATTERN' AND blocking = false "
                + "AND status = 'PENDING'", KP)).isEqualTo(1L);
        assertThat(reviews("true")).isEqualTo(1L);
    }

    @Test
    void aMarkedErrorWhoseParentPointHasNoTrapQuestionsGetsNoReviewButItsEvidenceStays() {
        when(content.searchPracticeSets(eq(KP), anyList(), eq(3), eq(1), any(), eq("SPK_NO_REASON")))
                .thenReturn(List.of());

        speak(FIRST, bands("6.5", "6.5", "6.5", "6.5"), List.of("SPK_NO_REASON"));

        assertThat(evidenceOf(TRAP, "error_trap")).isFalse();
        assertThat(reviews("true")).isZero();
    }

    @Test
    void anAnswerWithoutMarkedErrorsWritesNoTrapEvidence() {
        speak(FIRST, bands("6.5", "6.5", "6.5", "6.5"), List.of());

        assertThat(evidenceRows("error_trap")).isZero();
    }

    private static LearningContentClient.KnowledgePoint kp(UUID id) {
        return new LearningContentClient.KnowledgePoint(id, "KP", "Knowledge point", "PROCEDURE", "SPEAKING", null, false);
    }
}
