package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.command.PublishLearningVideoCommand;
import com.ieltspath.library.application.result.LearningVideoResult;
import com.ieltspath.library.domain.aggregate.LearningVideo;
import com.ieltspath.library.domain.exception.VideoNotFoundException;
import com.ieltspath.library.domain.repository.LearningVideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublishLearningVideoUseCase {

    private final LearningVideoRepository learningVideoRepository;

    public PublishLearningVideoUseCase(LearningVideoRepository learningVideoRepository) {
        this.learningVideoRepository = learningVideoRepository;
    }

    public LearningVideoResult execute(PublishLearningVideoCommand command) {
        LearningVideo video = learningVideoRepository.findById(command.videoId())
                .orElseThrow(() -> new VideoNotFoundException(command.videoId()));

        video.publish();
        LearningVideo saved = learningVideoRepository.save(video);

        return new LearningVideoResult(
                saved.getId(),
                saved.getYoutubeVideoId(),
                saved.getYoutubeUrl(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getThumbnailUrl(),
                saved.getDurationSeconds(),
                saved.getTopicId(),
                saved.getLevel(),
                saved.getRequiredFeatureKey(),
                saved.getStatus(),
                saved.getCreatedBy(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}
