package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.command.CreateLearningVideoCommand;
import com.ieltspath.library.domain.aggregate.LearningVideo;
import com.ieltspath.library.domain.exception.DuplicateCodeException;
import com.ieltspath.library.domain.repository.LearningVideoRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateLearningVideoUseCaseTest {
    private final LearningVideoRepository repository = mock(LearningVideoRepository.class);
    private final CreateLearningVideoUseCase useCase = new CreateLearningVideoUseCase(repository);

    @Test
    void keepsTheTopicIdAsAReferenceWithoutAskingAnotherService() {
        UUID topicId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0, LearningVideo.class));
        var result = useCase.execute(command(topicId));
        assertEquals(topicId, result.topicId());
        verify(repository).save(any());
    }

    @Test
    void savesAVideoWithoutATopic() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0, LearningVideo.class));
        assertNull(useCase.execute(command(null)).topicId());
    }

    @Test
    void rejectsADuplicateYoutubeVideoWithoutSaving() {
        when(repository.existsByYoutubeVideoId("abc123")).thenReturn(true);
        assertThrows(DuplicateCodeException.class, () -> useCase.execute(command(UUID.randomUUID())));
        verify(repository, never()).save(any());
    }

    private CreateLearningVideoCommand command(UUID topicId) {
        return new CreateLearningVideoCommand("abc123", "https://www.youtube.com/watch?v=abc123",
                "Lesson", null, null, 120, topicId, null, null, null);
    }
}
