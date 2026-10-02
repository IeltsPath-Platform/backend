package com.group01.learning.domain.aggregate;

import com.group01.learning.domain.vo.TopicStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Lesson progress, the learner's curriculum and final-test assignments. */
class ProgressAggregatesTest {
    private static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");

    @Test
    void lessonBlocksPassAndTheLessonCompletesOnce() {
        LessonProgress progress = LessonProgress.start(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                List.of());
        UUID block = UUID.randomUUID();

        assertThat(progress.passBlock(block)).isTrue();
        assertThat(progress.passBlock(block)).isFalse();
        assertThat(progress.hasPassed(block)).isTrue();

        assertThat(progress.complete(NOW)).isTrue();
        assertThat(progress.complete(NOW.plusSeconds(60))).isFalse();
        assertThat(progress.completedAt()).isEqualTo(NOW);
    }

    @Test
    void curriculumFollowsContentOrderAndPassesOneWay() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID removed = UUID.randomUUID();
        LearnerCurriculum curriculum = LearnerCurriculum.restore(UUID.randomUUID(), List.of());

        curriculum.reorder(List.of(removed, first, second));
        assertThat(curriculum.status(removed)).isEqualTo(TopicStatus.IN_PROGRESS);
        assertThat(curriculum.pass(removed, NOW)).isTrue();
        assertThat(curriculum.pass(removed, NOW.plusSeconds(1))).isFalse();

        curriculum.reorder(List.of(first, second));
        assertThat(curriculum.status(first)).isEqualTo(TopicStatus.IN_PROGRESS);
        assertThat(curriculum.status(second)).isEqualTo(TopicStatus.LOCKED);
        assertThat(curriculum.topics()).filteredOn(topic -> topic.topicId().equals(removed)).singleElement()
                .satisfies(topic -> {
                    assertThat(topic.sequenceOrder()).isNull();
                    assertThat(topic.passedAt()).isEqualTo(NOW);
                });
    }

    @Test
    void assignmentIsConsumedOnceAndPassesAtSeventyPercent() {
        TopicTestAssignment passing = TopicTestAssignment.assign(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertThat(passing.consume(UUID.randomUUID(), new BigDecimal("70.0000"))).isTrue();
        assertThat(passing.isConsumed()).isTrue();
        assertThatThrownBy(() -> passing.consume(UUID.randomUUID(), BigDecimal.TEN))
                .isInstanceOf(IllegalStateException.class);

        TopicTestAssignment failing = TopicTestAssignment.assign(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertThat(failing.consume(UUID.randomUUID(), new BigDecimal("66.6667"))).isFalse();
    }
}
