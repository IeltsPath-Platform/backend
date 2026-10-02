package com.group01.learning.domain.service;

import com.group01.learning.domain.entity.TopicProgress;
import com.group01.learning.domain.vo.TopicStatus;
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
}
