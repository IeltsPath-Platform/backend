package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.ApplicationSupport;
import com.ieltspath.library.application.result.VideoLearningProgressResult;
import com.ieltspath.library.domain.repository.VideoLearningProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetVideoProgressUseCase {
    private final VideoLearningProgressRepository repository;

    @Transactional(readOnly = true)
    public VideoLearningProgressResult execute(UUID userId, UUID progressId) {
        return VideoLearningProgressResult.from(
                ApplicationSupport.required(repository.findByIdAndUserId(progressId, userId))
        );
    }
}
