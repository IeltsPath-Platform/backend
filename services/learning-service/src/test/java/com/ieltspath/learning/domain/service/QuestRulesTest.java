package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.service.QuestRules.Candidate;
import com.ieltspath.learning.domain.service.QuestRules.Params;
import com.ieltspath.learning.domain.service.QuestRules.Spec;
import com.ieltspath.learning.domain.service.QuestRules.Type;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QuestRulesTest {
    private static final Params PARAMS = new Params(5, 20, 5, 3, 30, 10, 30, 10);
    private final UUID weak = UUID.randomUUID();
    private final UUID weaker = UUID.randomUUID();
    private final UUID blocked = UUID.randomUUID();

    @Test
    void aDayHasThreeQuestsWithTheWeaknessOneWhenThereIsATarget() {
        List<Spec> set = QuestRules.dailySet(PARAMS, true);

        assertThat(set).extracting(Spec::type).containsExactly(Type.PRACTICE, Type.WEAKNESS, Type.STREAK);
        assertThat(set.get(0)).isEqualTo(new Spec(Type.PRACTICE, 5, 20, 5));
        assertThat(set.get(1)).isEqualTo(new Spec(Type.WEAKNESS, 3, 30, 0));
        assertThat(set.get(2)).isEqualTo(new Spec(Type.STREAK, 1, 10, 0));
    }

    @Test
    void withoutATargetALongerPracticeQuestTakesTheirPlace() {
        List<Spec> set = QuestRules.dailySet(PARAMS, false);

        assertThat(set).extracting(Spec::type).containsExactly(Type.PRACTICE, Type.PRACTICE_PLUS, Type.STREAK);
        assertThat(set.get(1)).isEqualTo(new Spec(Type.PRACTICE_PLUS, 10, 30, 0));
    }

    @Test
    void theLowestMasteryPointIsPicked() {
        assertThat(QuestRules.pickWeakness(List.of(new Candidate(weak, 0.5, false, true),
                new Candidate(weaker, 0.2, false, true)))).contains(weaker);
    }

    @Test
    void aPointWithABlockingReviewComesBeforeAWeakerOne() {
        assertThat(QuestRules.pickWeakness(List.of(new Candidate(weaker, 0.1, false, true),
                new Candidate(blocked, 0.6, true, true)))).contains(blocked);
    }

    @Test
    void aPointThatCannotBePracticedIsSkippedAndNoneLeftMeansNoTarget() {
        assertThat(QuestRules.pickWeakness(List.of(new Candidate(weak, 0.1, false, false),
                new Candidate(weaker, 0.4, false, true)))).contains(weaker);
        assertThat(QuestRules.pickWeakness(List.of(new Candidate(weak, 0.1, false, false)))).isEmpty();
        assertThat(QuestRules.pickWeakness(List.of())).isEmpty();
    }

    @Test
    void onlyJudgedFirstAnswersFromPracticeSourcesCount() {
        assertThat(QuestRules.isPracticeAnswer(QuestionEventSource.LESSON_EXERCISE, true, true)).isTrue();
        assertThat(QuestRules.isPracticeAnswer(QuestionEventSource.PRACTICE_SET, false, true)).as("a wrong answer").isTrue();
        assertThat(QuestRules.isPracticeAnswer(QuestionEventSource.FREE_PRACTICE, true, true)).isTrue();
        assertThat(QuestRules.isPracticeAnswer(QuestionEventSource.REVIEW_SET, true, true)).isTrue();
        assertThat(QuestRules.isPracticeAnswer(QuestionEventSource.ASSESSMENT, true, true)).isFalse();
        assertThat(QuestRules.isPracticeAnswer(QuestionEventSource.DIAGNOSTIC_PROBE, true, true)).isFalse();
        assertThat(QuestRules.isPracticeAnswer(QuestionEventSource.LESSON_WRITING, false, true)).as("a graded essay").isTrue();
        assertThat(QuestRules.isPracticeAnswer(QuestionEventSource.LESSON_WRITING, true, false)).as("a rewrite").isFalse();
        assertThat(QuestRules.isPracticeAnswer(QuestionEventSource.LESSON_EXERCISE, null, true)).as("not judged").isFalse();
        assertThat(QuestRules.isPracticeAnswer(QuestionEventSource.LESSON_EXERCISE, true, false)).as("a repeat").isFalse();
    }

    @Test
    void theWeaknessQuestCountsOnlyTheTargetPointFromFreePracticeOrReview() {
        List<UUID> points = List.of(weak, weaker);

        assertThat(QuestRules.isWeaknessAnswer(QuestionEventSource.FREE_PRACTICE, true, true, points, weak)).isTrue();
        assertThat(QuestRules.isWeaknessAnswer(QuestionEventSource.REVIEW_SET, false, true, points, weaker)).isTrue();
        assertThat(QuestRules.isWeaknessAnswer(QuestionEventSource.LESSON_EXERCISE, true, true, points, weak))
                .as("a lesson answer is not weakness practice").isFalse();
        assertThat(QuestRules.isWeaknessAnswer(QuestionEventSource.FREE_PRACTICE, true, true, points, blocked))
                .as("another point").isFalse();
        assertThat(QuestRules.isWeaknessAnswer(QuestionEventSource.FREE_PRACTICE, true, true, points, null)).isFalse();
    }
}
