package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.CreateLearningActivityCommand;
import com.ieltspath.learning.domain.aggregate.LearningActivity;
import com.ieltspath.learning.domain.exception.InvalidDataException;
import com.ieltspath.learning.domain.exception.ResourceNotFoundException;
import com.ieltspath.learning.domain.repository.LearningActivityRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static final Instant AT = Instant.parse("2026-09-23T04:00:00Z");

    private CreateLearningActivityCommand command(String type, UUID sourceId, Instant at, Integer seconds) {
        return new CreateLearningActivityCommand(UUID.randomUUID(), type, "VIDEO", sourceId, at, seconds);
    }

    @Test
    void anActivityNeedsASourceAndATime() {
        assertEquals("activity không hợp lệ", assertThrows(InvalidDataException.class,
                () -> useCase.execute(command("WATCH", null, AT, 30))).getMessage());
        assertEquals("activity không hợp lệ", assertThrows(InvalidDataException.class,
                () -> useCase.execute(command("WATCH", UUID.randomUUID(), null, 30))).getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void aDurationMayBeZeroOrMissingButNotNegative() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals("durationSeconds không hợp lệ", assertThrows(InvalidDataException.class,
                () -> useCase.execute(command("WATCH", UUID.randomUUID(), AT, -1))).getMessage());

        assertEquals(0, useCase.execute(command("WATCH", UUID.randomUUID(), AT, 0)).durationSeconds());
        assertNull(useCase.execute(command("WATCH", UUID.randomUUID(), AT, null)).durationSeconds());
        verify(repository, times(2)).save(any());
    }

    @Test
    void anActivityTypeHasAtMost50Characters() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals("a".repeat(50), useCase.execute(command("a".repeat(50), UUID.randomUUID(), AT, 30)).activityType());

        assertEquals("activityType không hợp lệ", assertThrows(InvalidDataException.class,
                () -> useCase.execute(command("a".repeat(51), UUID.randomUUID(), AT, 30))).getMessage());
        assertEquals("activityType không hợp lệ", assertThrows(InvalidDataException.class,
                () -> useCase.execute(command(" ", UUID.randomUUID(), AT, 30))).getMessage());
        verify(repository, times(1)).save(any());
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

    @Test
    void theUsersOwnActivityIsDeleted() {
        UUID userId = UUID.randomUUID();
        var activity = LearningActivity.create(userId, "WATCH", "VIDEO", UUID.randomUUID(),
                Instant.parse("2026-09-23T04:00:00Z"), 30);
        when(repository.findByIdAndUserId(activity.getId(), userId)).thenReturn(Optional.of(activity));

        useCase.execute(userId, activity.getId());

        verify(repository).delete(activity);
    }
}
