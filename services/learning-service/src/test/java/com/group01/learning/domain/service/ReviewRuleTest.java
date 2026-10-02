package com.group01.learning.domain.service;

import com.group01.learning.domain.exception.LearningGateException;
import com.group01.learning.domain.service.ReviewRule.CompletedLesson;
import com.group01.learning.domain.service.ReviewRule.ReviewCandidate;
import com.group01.learning.domain.vo.PendingReview;
import com.group01.learning.domain.entity.TopicProgress;
import com.group01.learning.domain.vo.TopicStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewRuleTest {
    private final ReviewRule rule = new ReviewRule(0.6);
    private final UUID kp1 = new UUID(0, 1);
    private final UUID kp2 = new UUID(0, 2);
    private final UUID kp3 = new UUID(0, 3);
    private final UUID kp4 = new UUID(0, 4);
    private final UUID l1 = new UUID(0, 101);
    private final UUID l2 = new UUID(0, 102);
    private final UUID l3 = new UUID(0, 103);
    private final UUID l4 = new UUID(0, 104);

    @Test
    void requiresLowMasteryWrongAnswerCompletedLessonAndPracticeSetTogether() {
        Map<UUID, List<CompletedLesson>> completed = Map.of(kp1, List.of(new CompletedLesson(l2, 1, 2)));
        Map<UUID, Double> lowMastery = Map.of(kp1, 0.1);

        assertThat(rule.reevaluate(Set.of(kp1), Set.of(kp1), lowMastery, completed, Set.of(kp1), Set.of()))
                .containsExactly(new ReviewCandidate(kp1, l2));
        assertThat(rule.reevaluate(Set.of(kp1), Set.of(), lowMastery, completed, Set.of(kp1), Set.of())).isEmpty();
        assertThat(rule.reevaluate(Set.of(kp1), Set.of(kp1), lowMastery, Map.of(), Set.of(kp1), Set.of())).isEmpty();
        assertThat(rule.reevaluate(Set.of(kp1), Set.of(kp1), lowMastery, completed, Set.of(), Set.of())).isEmpty();
        assertThat(rule.reevaluate(Set.of(kp1), Set.of(kp1), Map.of(kp1, 0.6), completed, Set.of(kp1), Set.of()))
                .isEmpty();
        assertThat(rule.reevaluate(Set.of(), Set.of(kp1), lowMastery, completed, Set.of(kp1), Set.of())).isEmpty();
    }

    @Test
    void skipsKnowledgePointsWithAlreadyPendingReviews() {
        assertThat(rule.reevaluate(Set.of(kp1), Set.of(kp1), Map.of(kp1, 0.0),
                Map.of(kp1, List.of(new CompletedLesson(l2, 1, 2))), Set.of(kp1), Set.of(kp1))).isEmpty();
    }

    @Test
    void choosesEarlierCompletedL2BeforeL4TeachingSameKnowledgePoint() {
        assertThat(rule.reevaluate(Set.of(kp1), Set.of(kp1), Map.of(kp1, 0.0),
                Map.of(kp1, List.of(new CompletedLesson(l4, 1, 4), new CompletedLesson(l2, 1, 2))),
                Set.of(kp1), Set.of())).containsExactly(new ReviewCandidate(kp1, l2));
    }

    @Test
    void lessonSelectionUsesTopicSequenceThenLessonOrderThenStableIdWithNullSequenceLast() {
        Map<UUID, List<CompletedLesson>> lessons = Map.of(kp1, List.of(
                new CompletedLesson(l1, null, 1), new CompletedLesson(l2, 2, 1),
                new CompletedLesson(l4, 1, 3), new CompletedLesson(l3, 1, 3)));

        assertThat(rule.reevaluate(Set.of(kp1), Set.of(kp1), Map.of(kp1, 0.0), lessons, Set.of(kp1), Set.of()))
                .containsExactly(new ReviewCandidate(kp1, l3));
        assertThat(rule.reevaluate(Set.of(kp1), Set.of(kp1), Map.of(kp1, 0.0),
                Map.of(kp1, List.of(new CompletedLesson(l1, null, 1))), Set.of(kp1), Set.of()))
                .containsExactly(new ReviewCandidate(kp1, l1));
    }

    @Test
    void matchesLanLessonReviewAndLaterAssessmentScenarioAtPointSix() {
        MasteryCalculator calculator = new MasteryCalculator();
        Set<UUID> practiceKps = Set.of(kp1, kp2, kp3, kp4);
        double kp3Mastery = calculator.compute(List.of(true, true, false, true));
        assertThat(kp3Mastery).isCloseTo(0.729, within(0.0005));
        Map<UUID, List<CompletedLesson>> afterL1 = Map.of(kp3, List.of(new CompletedLesson(l1, 1, 1)));
        assertThat(rule.reevaluate(Set.of(kp3), Set.of(kp3), Map.of(kp3, kp3Mastery),
                afterL1, practiceKps, Set.of())).isEmpty();
        assertThat(new ReviewRule(0.9).reevaluate(Set.of(kp3), Set.of(kp3), Map.of(kp3, kp3Mastery),
                afterL1, practiceKps, Set.of())).containsExactly(new ReviewCandidate(kp3, l1));

        Map<UUID, List<CompletedLesson>> afterL2 = Map.of(kp1, List.of(new CompletedLesson(l2, 1, 2)));
        assertThat(rule.reevaluate(Set.of(kp1), Set.of(kp1), Map.of(kp1, calculator.compute(List.of(false))),
                afterL2, practiceKps, Set.of())).containsExactly(new ReviewCandidate(kp1, l2));
        PendingReview pending = new PendingReview(new UUID(0, 201), l2, kp1);
        LessonAccessGate gate = new LessonAccessGate();
        assertThat(assertThrows(LearningGateException.class,
                () -> gate.authorize(List.of(pending), null, TopicStatus.IN_PROGRESS, true)).getCode())
                .isEqualTo("REVIEW_REQUIRED");

        double kp1AfterReview = calculator.compute(List.of(false, true, true, true, true));
        assertThat(kp1AfterReview).isCloseTo(0.875, within(0.0005));
        assertThat(rule.reevaluate(Set.of(kp1), Set.of(kp1), Map.of(kp1, kp1AfterReview),
                afterL2, practiceKps, Set.of())).isEmpty();
        assertThatCode(() -> gate.authorize(List.of(), null, TopicStatus.IN_PROGRESS, true)).doesNotThrowAnyException();

        Map<UUID, List<CompletedLesson>> afterL3AndL4 = Map.of(
                kp2, List.of(new CompletedLesson(l3, 1, 3)), kp4, List.of(new CompletedLesson(l4, 1, 4)));
        assertThat(rule.reevaluate(Set.of(kp2, kp4), Set.of(), Map.of(kp2, 0.5, kp4, 0.5),
                afterL3AndL4, practiceKps, Set.of())).isEmpty();

        double kp2AfterTest = calculator.compute(List.of(true, false));
        assertThat(kp2AfterTest).isCloseTo(0.487, within(0.0005));
        assertThat(rule.reevaluate(Set.of(kp1, kp2, kp3, kp4), Set.of(kp2), Map.of(kp2, kp2AfterTest),
                afterL3AndL4, practiceKps, Set.of())).containsExactly(new ReviewCandidate(kp2, l3));
        UUID reading = new UUID(0, 301);
        UUID tfng = new UUID(0, 302);
        assertThat(new TopicStatusDeriver().derive(List.of(
                new TopicProgress(reading, 1, Instant.parse("2026-10-01T00:00:00Z")),
                new TopicProgress(tfng, 2, null))))
                .containsEntry(reading, TopicStatus.PASSED).containsEntry(tfng, TopicStatus.IN_PROGRESS);
    }

    @Test
    void validatesThresholdAndKeepsThreeFailedSetEscapeConstant() {
        for (double invalid : new double[]{0.0, -0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> new ReviewRule(invalid)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatCode(() -> new ReviewRule(1.0)).doesNotThrowAnyException();
        assertThat(ReviewRule.MAX_FAILED_REVIEW_SETS).isEqualTo(3);
    }
}
