package com.group01.library.application.usecase;

import com.group01.library.application.command.CreateLearningVideoCommand;
import com.group01.library.application.exception.TopicServiceUnavailableException;
import com.group01.library.application.port.TopicLookup;
import com.group01.library.domain.aggregate.LearningVideo;
import com.group01.library.domain.repository.LearningVideoRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateLearningVideoUseCaseTest {
    private final LearningVideoRepository repository = mock(LearningVideoRepository.class);
    private final TopicLookup topicLookup = mock(TopicLookup.class);
    private final CreateLearningVideoUseCase useCase = new CreateLearningVideoUseCase(repository, topicLookup);

    @Test
    void validatesTopicBeforeSaving() {
        UUID topicId = UUID.randomUUID();
        when(topicLookup.exists(topicId)).thenReturn(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0, LearningVideo.class));
        var result = useCase.execute(command(topicId));
        assertEquals(topicId, result.topicId());
        verify(repository).save(any());
    }

    @Test
    void rejectsMissingTopicWithoutSaving() {
        UUID topicId = UUID.randomUUID();
        when(topicLookup.exists(topicId)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(command(topicId)));
        verify(repository, never()).save(any());
    }

    @Test
    void propagatesTopicServiceFailureWithoutSaving() {
        UUID topicId = UUID.randomUUID();
        when(topicLookup.exists(topicId)).thenThrow(new TopicServiceUnavailableException(new RuntimeException()));
        assertThrows(TopicServiceUnavailableException.class, () -> useCase.execute(command(topicId)));
        verify(repository, never()).save(any());
    }

    private CreateLearningVideoCommand command(UUID topicId) {
        return new CreateLearningVideoCommand("abc123", "https://www.youtube.com/watch?v=abc123",
                "Lesson", null, null, 120, topicId, null, null, null);
    }
}
