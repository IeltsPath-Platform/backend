package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Course;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePoint;
import com.ieltspath.learning.application.port.LearningContentClient.Topic;
import com.ieltspath.learning.application.service.PaywallGate;
import com.ieltspath.learning.application.usecase.GetLearnerTreeUseCase.BlockingReview;
import com.ieltspath.learning.application.usecase.GetLearnerTreeUseCase.Node;
import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.BlockingReviewSummary;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The blocking review of a point stays on the tree after it is finished, so the map keeps the review. */
class GetLearnerTreeBlockingReviewTest {
    private final UUID user = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID reading = UUID.randomUUID();
    private final UUID skimming = UUID.randomUUID();
    private final UUID scanning = UUID.randomUUID();
    private final UUID untouched = UUID.randomUUID();

    private final LearningContentClient content = mock(LearningContentClient.class);
    private final ReviewItemRepository reviews = mock(ReviewItemRepository.class);
    private GetLearnerTreeUseCase tree;

    @BeforeEach
    void setUp() {
        LearnerCurriculumRepository curricula = mock(LearnerCurriculumRepository.class, RETURNS_DEEP_STUBS);
        TopicProgress taking = mock(TopicProgress.class);
        when(taking.topicId()).thenReturn(reading);
        when(curricula.find(user).topics()).thenReturn(List.of(taking));
        PaywallGate paywall = mock(PaywallGate.class);
        PaywallGate.Request request = mock(PaywallGate.Request.class);
        when(paywall.forLearner(user)).thenReturn(request);
        when(request.knowledgePointOpen(any(), any(), anyBoolean())).thenReturn(true);
        when(content.practiceSetAvailability(anyList(), anyList(), anyInt())).thenReturn(Map.of());
        when(content.prerequisites(anyList())).thenReturn(Map.of());
        Course course = new Course(courseId, "IELTS_5_5", "IELTS 5.5", BigDecimal.valueOf(5.5), false, true, null, null);
        when(content.getTopicSequence(true)).thenReturn(List.of(new Topic(reading, "Reading", "Reading", 900, null,
                List.of(point(skimming, "R_SKIM"), point(scanning, "R_SCAN"), point(untouched, "R_GAP")),
                LearningSkill.READING, false, course, null, true, 0)));
        tree = new GetLearnerTreeUseCase(curricula, mock(MasteryProfileRepository.class), reviews,
                mock(PracticeAttemptRepository.class), content, paywall, mock(LessonProgressRepository.class), 2);
    }

    private KnowledgePoint point(UUID id, String code) {
        return new KnowledgePoint(id, code, code, "PROCEDURE", "READING", null, true, null, null);
    }

    private Node node(UUID id) {
        return tree.execute(user, courseId).knowledgePoints().stream()
                .filter(row -> row.knowledgePointId().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void carriesAPendingReviewOfAPointWithTheLessonThatRaisedIt() {
        UUID review = UUID.randomUUID();
        UUID lesson = UUID.randomUUID();
        Instant created = Instant.parse("2026-11-10T08:00:00Z");
        when(reviews.findBlockingByPoint(eq(user), anyCollection())).thenReturn(Map.of(skimming,
                new BlockingReviewSummary(review, skimming, lesson, ReviewStatus.PENDING, created, null, 0, null, null)));

        BlockingReview shown = node(skimming).blockingReview();

        assertThat(shown).isEqualTo(new BlockingReview(review, "PENDING", lesson, created, null, 0, null, null));
        assertThat(node(untouched).blockingReview()).isNull();
    }

    @Test
    void keepsAFinishedReviewWithWhenItWasDoneAndHowTheLastSetWentSoTheMapStillShowsIt() {
        UUID review = UUID.randomUUID();
        Instant created = Instant.parse("2026-11-10T08:00:00Z");
        Instant done = Instant.parse("2026-11-11T09:30:00Z");
        when(reviews.findBlockingByPoint(eq(user), anyCollection())).thenReturn(Map.of(scanning,
                new BlockingReviewSummary(review, scanning, UUID.randomUUID(), ReviewStatus.DONE, created, done, 2, 8, 10)));

        BlockingReview shown = node(scanning).blockingReview();

        assertThat(shown.status()).isEqualTo("DONE");
        assertThat(shown.doneAt()).isEqualTo(done);
        assertThat(shown.setsAnswered()).isEqualTo(2);
        assertThat(shown.lastCorrect()).isEqualTo(8);
        assertThat(shown.lastTotal()).isEqualTo(10);
    }

    @Test
    void stillAnswersWithoutReviewsWhenTheLearnerHasNone() {
        when(reviews.findBlockingByPoint(eq(user), anyCollection())).thenReturn(Map.of());

        assertThat(node(skimming).blockingReview()).isNull();
    }
}
