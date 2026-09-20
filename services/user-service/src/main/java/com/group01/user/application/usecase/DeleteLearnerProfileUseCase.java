package com.group01.user.application.usecase;

import com.group01.user.domain.repository.LearnerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteLearnerProfileUseCase {
    private final LearnerProfileRepository learnerProfileRepository;

    @Transactional
    public void execute(UUID userId) {
        learnerProfileRepository.deleteByUserId(userId);
    }
}

