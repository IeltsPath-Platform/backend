package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.LessonContentResult;
import com.ieltspath.content.domain.exception.LessonNotFoundException;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetLessonContentUseCaseTest {
    private final LearningContentReader reader = mock(LearningContentReader.class);
    private final GetLessonContentUseCase useCase = new GetLessonContentUseCase(reader);
    private final UUID lessonId = UUID.randomUUID();

    @Test
    void returnsThePublishedLessonWithItsSummary() {
        var lesson = new LessonContentResult(lessonId, UUID.randomUUID(), "L1", "Title", null, "## Outcomes", 1,
                List.of(UUID.randomUUID()), null, List.of(Skill.READING));
        when(reader.publishedLesson(lessonId)).thenReturn(Optional.of(lesson));

        assertThat(useCase.execute(lessonId)).isSameAs(lesson);
        assertThat(lesson.skill()).isEqualTo(Skill.READING);
    }

    @Test
    void aLessonTeachingSeveralSkillsHasNoSingleSkill() {
        var lesson = new LessonContentResult(lessonId, UUID.randomUUID(), "T1", "Trees", null, null, 1, List.of(), null,
                List.of(Skill.WRITING, Skill.READING, Skill.ALL));

        assertThat(lesson.skills()).containsExactly(Skill.READING, Skill.WRITING);
        assertThat(lesson.skill()).isNull();
    }

    @Test
    void anUnknownOrUnpublishedLessonIsNotFound() {
        when(reader.publishedLesson(lessonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(lessonId)).isInstanceOf(LessonNotFoundException.class);
    }
}
