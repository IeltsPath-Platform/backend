package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.exception.VocabularyServiceUnavailableException;
import com.ieltspath.content.application.port.VocabularySenseLookup;
import com.ieltspath.content.application.result.LessonAuthoringResult;
import com.ieltspath.content.domain.exception.LessonBlockInvalidException;
import com.ieltspath.content.domain.exception.LessonExerciseLockedException;
import com.ieltspath.content.domain.exception.LessonNotPublishableException;
import com.ieltspath.content.domain.aggregate.Lesson;
import com.ieltspath.content.domain.aggregate.LessonBlock;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LessonAuthoringUseCaseTest {
    private final LessonAuthoringRepository repository = mock(LessonAuthoringRepository.class);
    private final VocabularySenseLookup vocabulary = mock(VocabularySenseLookup.class);
    private final LessonAuthoringUseCase useCase = new LessonAuthoringUseCase(repository, vocabulary);
    private final UUID lessonId = UUID.randomUUID();
    private final UUID blockId = UUID.randomUUID();

    @BeforeEach void stubLesson() { when(repository.lockAndFindById(lessonId)).thenReturn(Optional.of(lesson(LessonStatus.PUBLISHED, exerciseBlock()))); }

    @Test void locksExerciseQuestionsOnPublishedLesson() {
        assertThatThrownBy(() -> useCase.updateBlock(lessonId, blockId, BlockType.EXERCISE, null, null, null, List.of(), List.of(UUID.randomUUID()), List.of()))
                .isInstanceOf(LessonExerciseLockedException.class);
        verify(repository, never()).save(any(Lesson.class));
    }

    @Test void locksAddingExerciseBlockToPublishedLesson() {
        assertThatThrownBy(() -> useCase.addBlock(lessonId, BlockType.EXERCISE, null, null, null,
                List.of(), List.of(UUID.randomUUID()), List.of()))
                .isInstanceOf(LessonExerciseLockedException.class);
        verify(repository, never()).save(any(Lesson.class));
    }

    @Test void rejectsBlockThatDoesNotBelongToLesson() {
        assertThatThrownBy(() -> useCase.updateBlock(lessonId, UUID.randomUUID(), BlockType.TEXT, null, "text", null,
                List.of(), List.of(), List.of()))
                .isInstanceOf(LessonBlockInvalidException.class);
    }

    @Test void rejectsPublicationWhenRepositoryReportsViolations() {
        when(repository.publicationViolations(lessonId)).thenReturn(List.of("LESSON_HAS_NO_ACTIVE_KP"));
        assertThatThrownBy(() -> useCase.publish(lessonId)).isInstanceOf(LessonNotPublishableException.class);
        verify(repository, never()).save(any(Lesson.class));
    }

    @Test void mapsLibraryFailureToServiceUnavailableForPublish() {
        UUID senseId = UUID.randomUUID();
        Lesson lesson = lesson(LessonStatus.DRAFT);
        lesson.addBlock(LessonBlock.create(lessonId, BlockType.VOCABULARY, 0, null, null,
                List.of(senseId), List.of(), List.of()));
        when(repository.lockAndFindById(lessonId)).thenReturn(Optional.of(lesson));
        when(repository.publicationViolations(lessonId)).thenReturn(List.of());
        when(vocabulary.findActiveSenseIds(List.of(senseId))).thenThrow(new VocabularyServiceUnavailableException(new RuntimeException()));
        assertThatThrownBy(() -> useCase.publish(lessonId)).isInstanceOf(VocabularyServiceUnavailableException.class);
        verify(repository, never()).save(any(Lesson.class));
    }

    private Lesson lesson(LessonStatus status, LessonBlock... blocks) {
        return new Lesson(lessonId, UUID.randomUUID(), "L-1", "Lesson", null, null, 10, 0, status,
                Instant.now(), List.of(blocks), List.of(UUID.randomUUID()));
    }
    private LessonBlock exerciseBlock() {
        return new LessonBlock(blockId, lessonId, BlockType.EXERCISE, 0, null, null, List.of(),
                List.of(UUID.randomUUID()), List.of());
    }
}
