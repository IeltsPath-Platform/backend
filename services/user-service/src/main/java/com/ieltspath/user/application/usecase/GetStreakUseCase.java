package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.ApplicationSupport;
import com.ieltspath.user.application.result.StreakResult;
import com.ieltspath.user.domain.repository.StreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetStreakUseCase {
    private final StreakRepository repository;

    @Transactional(readOnly = true)
    public StreakResult execute(UUID userId) {
        return StreakResult.from(ApplicationSupport.required(repository.findByUserId(userId)));
    }
}
