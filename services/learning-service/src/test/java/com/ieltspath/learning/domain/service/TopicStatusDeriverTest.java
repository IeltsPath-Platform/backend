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
    void tiesUseStableTopicIds() {
        assertThat(deriver.derive(List.of(new TopicProgress(second, 1, null),
                new TopicProgress(first, 1, null))))
                .containsEntry(first, TopicStatus.IN_PROGRESS).containsEntry(second, TopicStatus.LOCKED);
    }

    @Test
    void opensAnIndependentTopicForEachSkillIncludingUnspecified() {
        UUID fourth = new UUID(0, 4);
        UUID fifth = new UUID(0, 5);
        UUID readingCourse = new UUID(1, 1);
        UUID higherCourse = new UUID(1, 2);
        UUID unassignedCourse = new UUID(1, 3);
        var result = deriver.derive(List.of(
                new TopicProgress(first, readingCourse, 1, null, LearningSkill.READING, true),
                new TopicProgress(second, readingCourse, 2, null, LearningSkill.LISTENING, true),
                new TopicProgress(third, unassignedCourse, 3, null, null, true),
                new TopicProgress(fourth, higherCourse, 4, null, LearningSkill.READING, true),
                new TopicProgress(fifth, higherCourse, 5, null, LearningSkill.LISTENING, true)));

        assertThat(result).containsEntry(first, TopicStatus.IN_PROGRESS)
                .containsEntry(second, TopicStatus.LOCKED)
                .containsEntry(third, TopicStatus.IN_PROGRESS)
                .containsEntry(fourth, TopicStatus.IN_PROGRESS)
                .containsEntry(fifth, TopicStatus.LOCKED);
    }
}
