package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.command.CreateLearningActivityCommand;
import com.group01.learningsupport.domain.exception.ResourceNotFoundException;
import com.group01.learningsupport.domain.repository.LearningActivityRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateLearningActivityUseCaseTest {
    private final LearningActivityRepository repository = mock(LearningActivityRepository.class);
    private final CreateLearningActivityUseCase useCase = new CreateLearningActivityUseCase(repository);

    @Test
    void createLeavesVerifiedAtEmpty() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = useCase.execute(new CreateLearningActivityCommand(
                UUID.randomUUID(),
                "WATCH",
                "VIDEO",
                UUID.randomUUID(),
                Instant.parse("2026-09-23T04:00:00Z"),
                30
        ));

        assertNull(saved.verifiedAt());
    }
}

class DeleteLearningActivityUseCaseTest {
    private final LearningActivityRepository repository = mock(LearningActivityRepository.class);
    private final DeleteLearningActivityUseCase useCase = new DeleteLearningActivityUseCase(repository);

    @Test
    void missingOwnerRowIsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        when(repository.findByIdAndUserId(activityId, userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(userId, activityId));
        verify(repository, never()).delete(any());
    }
}
