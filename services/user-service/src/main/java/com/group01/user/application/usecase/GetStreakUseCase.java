package com.group01.user.application.usecase;

import com.group01.user.application.ApplicationSupport;
import com.group01.user.application.result.StreakResult;
import com.group01.user.domain.repository.StreakRepository;
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
