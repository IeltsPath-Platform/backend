package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.domain.repository.VideoLearningProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteVideoProgressUseCase {
    private final VideoLearningProgressRepository repository;

    @Transactional
    public void execute(UUID userId, UUID progressId) {
        repository.delete(ApplicationSupport.required(repository.findByIdAndUserId(progressId, userId)));
    }
}
