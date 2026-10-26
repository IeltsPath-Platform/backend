package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.exception.LessonExerciseLockedException;
import com.ieltspath.content.domain.vo.LessonStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LessonTest {
    private Lesson lesson(LessonStatus status) {
        return new Lesson(UUID.randomUUID(), UUID.randomUUID(), "LESSON", "Lesson", null, null,
                null, 0, status, Instant.now(), List.of(), List.of());
    }

    @Test void exerciseCanBeEditedOnlyWhileDraft() {
        assertThatCode(() -> lesson(LessonStatus.DRAFT).ensureExerciseEditable()).doesNotThrowAnyException();
        assertThatThrownBy(() -> lesson(LessonStatus.PUBLISHED).ensureExerciseEditable())
                .isInstanceOf(LessonExerciseLockedException.class);
        assertThatThrownBy(() -> lesson(LessonStatus.ARCHIVED).ensureExerciseEditable())
                .isInstanceOf(LessonExerciseLockedException.class);
    }

    @Test void publishAndArchiveChangeStatus() {
        Lesson lesson = lesson(LessonStatus.DRAFT);
        lesson.publish();
        org.assertj.core.api.Assertions.assertThat(lesson.getStatus()).isEqualTo(LessonStatus.PUBLISHED);
        lesson.archive();
        org.assertj.core.api.Assertions.assertThat(lesson.getStatus()).isEqualTo(LessonStatus.ARCHIVED);
    }
}
