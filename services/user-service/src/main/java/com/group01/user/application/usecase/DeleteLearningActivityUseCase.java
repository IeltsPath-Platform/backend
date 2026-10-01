package com.group01.user.application.usecase;

import com.group01.user.application.ApplicationSupport;
import com.group01.user.domain.repository.LearningActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteLearningActivityUseCase {
    private final LearningActivityRepository repository;

    @Transactional
    public void execute(UUID userId, UUID activityId) {
        repository.delete(ApplicationSupport.required(repository.findByIdAndUserId(activityId, userId)));
    }
}
