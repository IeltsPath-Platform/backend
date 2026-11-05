package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.vo.TopicStatus;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TopicStatusDeriverTest {
    private final TopicStatusDeriver deriver = new TopicStatusDeriver();
    private final UUID first = new UUID(0, 1);
    private final UUID second = new UUID(0, 2);
    private final UUID third = new UUID(0, 3);
    private final Instant passedAt = Instant.parse("2026-10-01T00:00:00Z");

    @Test
    void opensOnlyFirstUnpassedTopicInCurrentSequence() {
        Map<UUID, TopicStatus> result = deriver.derive(List.of(
                new TopicProgress(third, 3, null), new TopicProgress(first, 1, passedAt),
                new TopicProgress(second, 2, null)));

        assertThat(result).containsEntry(first, TopicStatus.PASSED)
                .containsEntry(second, TopicStatus.IN_PROGRESS).containsEntry(third, TopicStatus.LOCKED);
        assertThat(result.keySet()).containsExactly(first, second, third);
    }

    @Test
    void newlyInsertedTopicCanOpenBeforeAlreadyPassedTopic() {
        Map<UUID, TopicStatus> result = deriver.derive(List.of(
                new TopicProgress(first, 2, passedAt), new TopicProgress(second, 3, null),
                new TopicProgress(third, 1, null)));

        assertThat(result).containsEntry(third, TopicStatus.IN_PROGRESS)
                .containsEntry(first, TopicStatus.PASSED).containsEntry(second, TopicStatus.LOCKED);
    }

    @Test
    void removedTopicDoesNotBlockRemainingSequenceEvenIfItWasNotPassed() {
        Map<UUID, TopicStatus> result = deriver.derive(List.of(
                new TopicProgress(first, null, null), new TopicProgress(second, 2, null),
                new TopicProgress(third, null, passedAt)));

        assertThat(result).containsOnlyKeys(second).containsEntry(second, TopicStatus.IN_PROGRESS);
    }

    @Test
    void passedTopicsStayPassedAndEmptySequenceHasNoStatuses() {
        assertThat(deriver.derive(List.of(new TopicProgress(first, 1, passedAt),
                new TopicProgress(second, 2, passedAt))))
                .containsEntry(first, TopicStatus.PASSED).containsEntry(second, TopicStatus.PASSED);
        assertThat(deriver.derive(List.of())).isEmpty();
    }

    @Test
    void aTopicTestedBelowThePassMarkIsCompletedAndStillOpensTheNextTopic() {
        var tested = new TopicProgress(first, 1, null);
        tested.recordFirstTest(passedAt, 42.0);

        var result = deriver.derive(List.of(tested, new TopicProgress(second, 2, null),
                new TopicProgress(third, 3, null)));

        assertThat(result).containsEntry(first, TopicStatus.COMPLETED)
                .containsEntry(second, TopicStatus.IN_PROGRESS).containsEntry(third, TopicStatus.LOCKED);
    }

    @Test
    void passingAfterTheFirstTestMakesTheTopicPassedButKeepsTheFirstScore() {
        var topic = new TopicProgress(first, 1, null);
        assertThat(topic.recordFirstTest(passedAt, 42.0)).isTrue();
        assertThat(topic.recordFirstTest(passedAt.plusSeconds(60), 90.0)).isFalse();
        topic.pass(passedAt.plusSeconds(60));

        assertThat(deriver.derive(List.of(topic))).containsEntry(first, TopicStatus.PASSED);
        assertThat(topic.firstTestPercent()).isEqualTo(42.0);
        assertThat(topic.done()).isTrue();
    }

    @Test
    void tiesUseStableTopicIds() {
        assertThat(deriver.derive(List.of(new TopicProgress(second, 1, null),
                new TopicProgress(first, 1, null))))
                .containsEntry(first, TopicStatus.IN_PROGRESS).containsEntry(second, TopicStatus.LOCKED);
    }

    @Test
    void opensTheFirstUnfinishedTopicIndependentlyForEachCourseSkill() {
        UUID fourth = new UUID(0, 4);
        UUID fifth = new UUID(0, 5);
        UUID sixth = new UUID(0, 6);
        UUID seventh = new UUID(0, 7);
        UUID course = new UUID(1, 1);
        var result = deriver.derive(List.of(
                new TopicProgress(first, course, 1, null, LearningSkill.READING, true),
                new TopicProgress(second, course, 2, null, LearningSkill.LISTENING, true),
                new TopicProgress(third, course, 3, null, LearningSkill.WRITING, true),
                new TopicProgress(fourth, course, 4, null, LearningSkill.SPEAKING, true),
                new TopicProgress(fifth, course, 5, null, LearningSkill.READING, true),
                new TopicProgress(sixth, course, 6, null, LearningSkill.LISTENING, true),
                new TopicProgress(seventh, course, 7, passedAt, LearningSkill.WRITING, true)));

        assertThat(result).containsEntry(first, TopicStatus.IN_PROGRESS)
                .containsEntry(second, TopicStatus.IN_PROGRESS)
                .containsEntry(third, TopicStatus.IN_PROGRESS)
                .containsEntry(fourth, TopicStatus.IN_PROGRESS)
                .containsEntry(fifth, TopicStatus.LOCKED)
                .containsEntry(sixth, TopicStatus.LOCKED)
                .containsEntry(seventh, TopicStatus.PASSED);
    }

    @Test
    void topicsWithoutACourseContinueToShareOneSequence() {
        var result = deriver.derive(List.of(
                new TopicProgress(first, 1, null, LearningSkill.READING, true),
                new TopicProgress(second, 2, null, LearningSkill.LISTENING, true)));

        assertThat(result).containsEntry(first, TopicStatus.IN_PROGRESS)
                .containsEntry(second, TopicStatus.LOCKED);
    }
}
