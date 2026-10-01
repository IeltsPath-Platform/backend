package com.group01.user.application.usecase;

import com.group01.user.application.command.UpsertStreakCommand;
import com.group01.user.application.result.StreakResult;
import com.group01.user.domain.aggregate.Streak;
import com.group01.user.domain.repository.StreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpsertStreakUseCase {
    private final StreakRepository streakRepository;

    @Transactional
    public StreakResult execute(UpsertStreakCommand command) {
        return StreakResult.from(streakRepository.save(Streak.of(
                command.userId(),
                command.currentDays(),
                command.longestDays(),
                command.lastQualifiedDate(),
                command.timezone()
        )));
    }
}
