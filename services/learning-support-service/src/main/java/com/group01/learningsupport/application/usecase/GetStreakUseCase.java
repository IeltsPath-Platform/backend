package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.domain.aggregate.Streak;
import com.group01.learningsupport.domain.repository.StreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetStreakUseCase {
    private final StreakRepository repository;

    @Transactional(readOnly = true)
    public Streak execute(UUID userId) {
        return ApplicationSupport.required(repository.findByUserId(userId));
    }
}
