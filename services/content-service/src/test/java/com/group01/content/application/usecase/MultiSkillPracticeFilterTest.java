package com.group01.content.application.usecase;

import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.LessonPracticeSetResult;
import com.group01.content.domain.exception.LessonNotFoundException;
import com.group01.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MultiSkillPracticeFilterTest {
    private final LearningContentReader reader = mock(LearningContentReader.class);
    private final GetLessonPracticeSetsUseCase useCase = new GetLessonPracticeSetsUseCase(reader);
    private final UUID lesson = UUID.randomUUID();

    @Test
    void selectedSkillIsPassedToTheReader() {
        when(reader.publishedLessonExists(lesson)).thenReturn(true);
        var result = List.of(new LessonPracticeSetResult(lesson, UUID.randomUUID(), UUID.randomUUID(),
                "READING_SET", "Reading", 2, List.of(), null, List.of(Skill.READING)));
        when(reader.lessonPracticeSets(lesson, Optional.of(Skill.READING))).thenReturn(result);

        assertThat(useCase.execute(lesson, Optional.of(Skill.READING))).isEqualTo(result);
        verify(reader).lessonPracticeSets(lesson, Optional.of(Skill.READING));
        verify(reader, never()).lessonPracticeSets(lesson);
    }

    @Test
    void absentSkillPreservesTheUnfilteredReaderContract() {
        when(reader.publishedLessonExists(lesson)).thenReturn(true);
        var result = List.of(new LessonPracticeSetResult(lesson, UUID.randomUUID(), UUID.randomUUID(),
                "MIXED_SET", "Mixed", 2, List.of(), null, List.of(Skill.LISTENING, Skill.READING)));
        when(reader.lessonPracticeSets(lesson)).thenReturn(result);

        assertThat(useCase.execute(lesson, Optional.empty())).isEqualTo(result);
        verify(reader).lessonPracticeSets(lesson);
        verify(reader, never()).lessonPracticeSets(eq(lesson), any());
    }

    @Test
    void missingLessonIsRejectedBeforeReadingFilteredSets() {
        assertThatThrownBy(() -> useCase.execute(lesson, Optional.of(Skill.READING)))
                .isInstanceOf(LessonNotFoundException.class);
        verify(reader).publishedLessonExists(lesson);
        verifyNoMoreInteractions(reader);
    }
}
