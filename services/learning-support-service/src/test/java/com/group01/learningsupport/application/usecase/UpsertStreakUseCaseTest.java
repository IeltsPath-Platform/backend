package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.command.UpsertStreakCommand;
import com.group01.learningsupport.domain.aggregate.Streak;
import com.group01.learningsupport.domain.repository.LearningActivityRepository;
import com.group01.learningsupport.domain.repository.StreakRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UpsertStreakUseCaseTest {
    private final StreakRepository streakRepository = mock(StreakRepository.class);
    private final LearningActivityRepository activityRepository = mock(LearningActivityRepository.class);
    private final UpsertStreakUseCase useCase = new UpsertStreakUseCase(streakRepository);

    @Test
    void storesSubmittedStreakWithoutReadingActivities() {
        UUID userId = UUID.randomUUID();
        when(streakRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Streak saved = useCase.execute(new UpsertStreakCommand(userId, 3, 5, LocalDate.parse("2026-09-22"), "Asia/Ho_Chi_Minh"));

        assertEquals(3, saved.getCurrentDays());
        assertEquals(5, saved.getLongestDays());
        verify(streakRepository).save(any());
        verifyNoInteractions(activityRepository);
    }
}
