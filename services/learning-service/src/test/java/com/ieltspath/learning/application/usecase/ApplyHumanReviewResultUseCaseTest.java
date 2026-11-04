package com.ieltspath.learning.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.command.HumanReviewResult;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.ErrorTrapCatalog;
import com.ieltspath.learning.application.service.ErrorTrapReviews;
import com.ieltspath.learning.application.service.LessonAccess;
import com.ieltspath.learning.application.service.LessonEvidenceReference;
import com.ieltspath.learning.application.service.MasteryProfileUpdater;
import com.ieltspath.learning.application.service.WritingCriterionReviews;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.HumanReviewOutcomeRepository;
import com.ieltspath.learning.domain.repository.HumanReviewRequestRepository;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.EssayPrompt;
import com.ieltspath.learning.domain.vo.FailedWritingCriterion;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplyHumanReviewResultUseCaseTest {
    private static final UUID LEARNER = id(1), ESSAY = id(2), REVIEW = id(3), LESSON = id(4), BLOCK = id(5);
    private static final UUID TR = id(10), CC = id(11), TRAP_LINKER = id(12), PARENT_LINKER = id(13), QV = id(14);

    private LearnerLock lock;
    private WritingSubmissionRepository essays;
    private HumanReviewRequestRepository requests;
    private PracticeAttemptRepository attempts;
    private LearningContentClient content;
    private ErrorTrapCatalog traps;
    private LessonAccess access;
    private KnowledgeEvidenceRepository evidence;
    private MasteryProfileUpdater profiles;
    private ErrorTrapReviews errorTrapReviews;
    private WritingCriterionReviews criterionReviews;
    private ReviewItemRepository reviews;
    private HumanReviewOutcomeRepository outcomes;
    private ObjectMapper json;
    private ApplyHumanReviewResultUseCase useCase;

    @BeforeEach
    void setUp() {
        lock = mock(LearnerLock.class);
        essays = mock(WritingSubmissionRepository.class);
        requests = mock(HumanReviewRequestRepository.class);
        attempts = mock(PracticeAttemptRepository.class);
        content = mock(LearningContentClient.class);
        traps = mock(ErrorTrapCatalog.class);
        access = mock(LessonAccess.class);
        evidence = mock(KnowledgeEvidenceRepository.class);
        profiles = mock(MasteryProfileUpdater.class);
        errorTrapReviews = mock(ErrorTrapReviews.class);
        criterionReviews = mock(WritingCriterionReviews.class);
        reviews = mock(ReviewItemRepository.class);
        outcomes = mock(HumanReviewOutcomeRepository.class);
        json = new ObjectMapper();
        useCase = new ApplyHumanReviewResultUseCase(lock, essays, requests, attempts, content, traps, access, evidence,
                profiles, errorTrapReviews, criterionReviews, reviews, outcomes, json, 2);

        WritingSubmission essay = WritingSubmission.start(ESSAY, LEARNER, LESSON, BLOCK, UUID.randomUUID(),
                "A learner essay of sixty words or so.", 60, prompt(), 3, Instant.parse("2026-11-05T00:00:00Z"));
        when(essays.findOwned(ESSAY, LEARNER)).thenReturn(Optional.of(essay));
        when(requests.findReviewId(ESSAY)).thenReturn(Optional.of(REVIEW));
        when(content.getActiveRubric("TASK_2")).thenReturn(new LearningContentClient.Rubric("IELTS_WRITING_TASK_2",
                "TASK_2", 1, null, List.of(
                new LearningContentClient.RubricCriterion("TR", "Task response", "", 1, List.of(), TR),
                new LearningContentClient.RubricCriterion("CC", "Coherence", "", 2, List.of(), CC)), List.of()));
        when(content.activeErrorTypes()).thenReturn(List.of(
                new LearningContentClient.ErrorType("LINKER_MISUSE", "Linker", "COHERENCE", 1, "Use the right linker")));
        when(traps.load()).thenReturn(new ErrorTrapCatalog.ErrorTraps(Map.of("LINKER_MISUSE", TRAP_LINKER),
                Map.of(TRAP_LINKER, PARENT_LINKER), Map.of()));
        when(access.knownKnowledgePoints(eq(LEARNER), anyCollection())).thenAnswer(invocation ->
                new HashSet<UUID>(invocation.<Collection<UUID>>getArgument(1)));
        when(outcomes.studyPointsOf(ESSAY)).thenReturn(Optional.empty());
        when(reviews.findPending(LEARNER)).thenReturn(List.of());
        when(content.getTopicSequence()).thenReturn(List.of());
    }

    @Test
    void theExaminersBandsReplaceTheAiAndTheMarkedErrorIsCountedOnce() throws Exception {
        when(evidence.removeWritingEvidence(eq(LEARNER), anyCollection())).thenReturn(Set.of(CC));

        useCase.execute(result(List.of(new HumanReviewResult.Criterion("coherence", new BigDecimal("5.0")),
                new HumanReviewResult.Criterion("taskResponse", new BigDecimal("6.5"))),
                List.of(new HumanReviewResult.ErrorCount("LINKER_MISUSE", 2))));

        ArgumentCaptor<Collection<UUID>> removed = captorOf(Collection.class);
        verify(evidence).removeWritingEvidence(eq(LEARNER), removed.capture());
        assertTrue(removed.getValue().contains(LessonEvidenceReference.forWriting(ESSAY, CC)),
                "the AI's criterion evidence for this essay is the one replaced");

        ArgumentCaptor<List<KnowledgeEvidence>> appended = captorOf(List.class);
        verify(evidence).append(eq(LEARNER), appended.capture());
        assertTrue(appended.getValue().contains(KnowledgeEvidence.of(CC, false, EvidenceSource.HUMAN_REVIEW,
                LessonEvidenceReference.forHumanReview(REVIEW, CC))));
        assertTrue(appended.getValue().contains(KnowledgeEvidence.of(TR, true, EvidenceSource.HUMAN_REVIEW,
                LessonEvidenceReference.forHumanReview(REVIEW, TR))));
        assertTrue(appended.getValue().contains(KnowledgeEvidence.of(TRAP_LINKER, false, EvidenceSource.ERROR_TRAP,
                LessonEvidenceReference.forHumanReview(REVIEW, TRAP_LINKER))), "one trap wrong answer per marked code");
        assertEquals(3, appended.getValue().size());
        verify(profiles).rebuild(eq(LEARNER), any(Collection.class));

        ArgumentCaptor<Collection<ErrorTrapReviews.MarkedErrorTrap>> marked = captorOf(Collection.class);
        verify(errorTrapReviews).createForMarkedCodes(eq(LEARNER), eq(LESSON), marked.capture(), eq(2));
        assertEquals(List.of(new ErrorTrapReviews.MarkedErrorTrap("LINKER_MISUSE", TRAP_LINKER, 2)), List.copyOf(marked.getValue()));
        verify(criterionReviews).create(eq(LEARNER), eq(LESSON),
                eq(List.of(new FailedWritingCriterion(CC, "DEMO_WRITING_CRITERION_CC", new BigDecimal("5.0")))), eq(2));

        ArgumentCaptor<String> stored = captorOf(String.class);
        verify(outcomes).insertIfAbsent(eq(REVIEW), eq(LEARNER), eq(ESSAY), stored.capture());
        JsonNode points = json.readTree(stored.getValue());
        assertEquals("ERROR_CODE", points.get(0).get("kind").asText());
        assertEquals(2, points.get(0).get("count").asInt());
        assertEquals("CRITERION", points.get(1).get("kind").asText());
        assertEquals("CC", points.get(1).get("criterionCode").asText());
        assertEquals(5.0, points.get(1).get("band").asDouble());
        assertFalse(stored.getValue().contains("A learner essay"), "study points carry no essay text");
    }

    @Test
    void theErrorReviewsTakeTheSharedLimitSoCriterionReviewsGetTheRest() {
        when(evidence.removeWritingEvidence(eq(LEARNER), anyCollection())).thenReturn(Set.of());
        when(errorTrapReviews.createForMarkedCodes(eq(LEARNER), eq(LESSON), anyCollection(), eq(2))).thenReturn(1);

        useCase.execute(result(List.of(new HumanReviewResult.Criterion("coherence", new BigDecimal("5.0"))),
                List.of(new HumanReviewResult.ErrorCount("LINKER_MISUSE", 1))));

        verify(criterionReviews).create(eq(LEARNER), eq(LESSON), any(Collection.class), eq(1));
    }

    @Test
    void aRepeatedEventChangesNothing() {
        when(outcomes.studyPointsOf(ESSAY)).thenReturn(Optional.of("[]"));

        useCase.execute(result(List.of(new HumanReviewResult.Criterion("coherence", new BigDecimal("5.0"))),
                List.of(new HumanReviewResult.ErrorCount("LINKER_MISUSE", 1))));

        verify(evidence, never()).removeWritingEvidence(any(), anyCollection());
        verify(evidence, never()).append(any(), anyList());
        verify(errorTrapReviews, never()).createForMarkedCodes(any(), any(), anyCollection(), org.mockito.ArgumentMatchers.anyInt());
        verify(outcomes, never()).insertIfAbsent(any(), any(), any(), any());
    }

    @Test
    void anUnapprovedCodeIsNeitherCountedNorShown() throws Exception {
        when(content.activeErrorTypes()).thenReturn(List.of());

        useCase.execute(result(List.of(new HumanReviewResult.Criterion("coherence", new BigDecimal("6.5"))),
                List.of(new HumanReviewResult.ErrorCount("LINKER_MISUSE", 3))));

        ArgumentCaptor<List<KnowledgeEvidence>> appended = captorOf(List.class);
        verify(evidence).append(eq(LEARNER), appended.capture());
        assertTrue(appended.getValue().stream().noneMatch(item -> item.equals(
                KnowledgeEvidence.of(TRAP_LINKER, false, EvidenceSource.ERROR_TRAP,
                        LessonEvidenceReference.forHumanReview(REVIEW, TRAP_LINKER)))));
        ArgumentCaptor<String> stored = captorOf(String.class);
        verify(outcomes).insertIfAbsent(eq(REVIEW), eq(LEARNER), eq(ESSAY), stored.capture());
        assertFalse(stored.getValue().contains("ERROR_CODE"));
    }

    @Test
    void aContentOutageWritesNothing() {
        when(content.getActiveRubric("TASK_2")).thenThrow(new IllegalStateException("content is down"));

        assertThrows(IllegalStateException.class, () -> useCase.execute(result(
                List.of(new HumanReviewResult.Criterion("coherence", new BigDecimal("5.0"))),
                List.of(new HumanReviewResult.ErrorCount("LINKER_MISUSE", 1)))));

        verify(evidence, never()).removeWritingEvidence(any(), anyCollection());
        verify(evidence, never()).append(any(), anyList());
        verify(outcomes, never()).insertIfAbsent(any(), any(), any(), any());
    }

    @Test
    void anEssayThatIsNotTheLearnersIsRefused() {
        when(essays.findOwned(ESSAY, LEARNER)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(result(List.of(), List.of())));

        verify(evidence, never()).append(any(), anyList());
    }

    @Test
    void aReviewThatWasNotAskedForTheEssayIsRefused() {
        when(requests.findReviewId(ESSAY)).thenReturn(Optional.of(id(99)));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(result(List.of(), List.of())));

        verify(evidence, never()).append(any(), anyList());
    }

    @Test
    void theStudyPointsNameTheGrammarPointAndTheReviewThatWasMadeForIt() throws Exception {
        when(content.getTopicSequence()).thenReturn(List.of(new LearningContentClient.Topic(id(50), "T", "Topic", 1,
                List.of(new LearningContentClient.KnowledgePoint(PARENT_LINKER, "DEMO_WRITING_LINKING", "Linking words",
                        "CONCEPT", "WRITING", "", true, null, null, null, null)))));
        // The optional review made for the marked error is pending by the time the study points are read.
        when(reviews.findPending(LEARNER)).thenReturn(List.of(new PendingReview(id(77), LESSON, PARENT_LINKER,
                null, false, "ERROR_PATTERN")));

        useCase.execute(result(List.of(), List.of(new HumanReviewResult.ErrorCount("LINKER_MISUSE", 1))));

        ArgumentCaptor<String> stored = captorOf(String.class);
        verify(outcomes).insertIfAbsent(eq(REVIEW), eq(LEARNER), eq(ESSAY), stored.capture());
        JsonNode point = json.readTree(stored.getValue()).get(0);
        assertEquals(PARENT_LINKER.toString(), point.get("knowledgePointId").asText());
        assertEquals("DEMO_WRITING_LINKING", point.get("code").asText());
        assertEquals("Linking words", point.get("name").asText());
        assertEquals(id(77).toString(), point.get("reviewItemId").asText());
        assertEquals(PARENT_LINKER.toString(), point.get("practiceKnowledgePointId").asText());
    }

    private static HumanReviewResult result(List<HumanReviewResult.Criterion> criteria,
                                            List<HumanReviewResult.ErrorCount> errors) {
        return new HumanReviewResult(id(90), LEARNER, REVIEW, ESSAY, criteria, errors);
    }

    private static EssayPrompt prompt() {
        return new EssayPrompt(QV, "Discuss both views.", "TASK_2", 250, new BigDecimal("6.0"), null, null, List.of(),
                List.of(CC));
    }

    private static UUID id(int suffix) {
        return UUID.fromString("50000000-0000-4000-8000-" + String.format("%012d", suffix));
    }

    /** A captor typed by its use site; Mockito only offers raw captors for generic parameters. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ArgumentCaptor<T> captorOf(Class<?> type) {
        return (ArgumentCaptor) ArgumentCaptor.forClass(type);
    }
}
