package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.command.UpsertStreakCommand;
import com.group01.learningsupport.domain.repository.LearningActivityRepository;
import com.group01.learningsupport.domain.repository.StreakRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpsertStreakUseCaseTest {
    private final StreakRepository streakRepository = mock(StreakRepository.class);
    private final LearningActivityRepository activityRepository = mock(LearningActivityRepository.class);
    private final UpsertStreakUseCase useCase = new UpsertStreakUseCase(streakRepository);

    @Test
    void storesSubmittedStreakWithoutReadingActivities() {
        UUID userId = UUID.randomUUID();
        when(streakRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = useCase.execute(new UpsertStreakCommand(userId, 3, 5, LocalDate.parse("2026-09-22"), "Asia/Ho_Chi_Minh"));

        assertEquals(3, saved.currentDays());
        assertEquals(5, saved.longestDays());
        verify(streakRepository).save(any());
        verifyNoInteractions(activityRepository);
    }
}
