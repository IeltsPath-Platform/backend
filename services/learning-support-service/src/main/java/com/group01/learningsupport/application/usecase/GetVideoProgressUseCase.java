package com.group01.learningsupport.application.usecase;

import com.group01.learningsupport.application.ApplicationSupport;
import com.group01.learningsupport.domain.aggregate.VideoLearningProgress;
import com.group01.learningsupport.domain.repository.VideoLearningProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetVideoProgressUseCase {
    private final VideoLearningProgressRepository repository;

    @Transactional(readOnly = true)
    public VideoLearningProgress execute(UUID userId, UUID progressId) {
        return ApplicationSupport.required(repository.findByIdAndUserId(progressId, userId));
    }
}
