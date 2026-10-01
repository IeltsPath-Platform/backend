package com.group01.content.application.usecase;

import com.group01.content.application.command.CreateLearningVideoCommand;
import com.group01.content.application.result.LearningVideoResult;
import com.group01.content.domain.aggregate.LearningVideo;
import com.group01.content.domain.exception.DuplicateCodeException;
import com.group01.content.domain.repository.LearningVideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateLearningVideoUseCase {

    private final LearningVideoRepository learningVideoRepository;

    public CreateLearningVideoUseCase(LearningVideoRepository learningVideoRepository) {
        this.learningVideoRepository = learningVideoRepository;
    }

    public LearningVideoResult execute(CreateLearningVideoCommand command) {
        if (learningVideoRepository.existsByYoutubeVideoId(command.youtubeVideoId())) {
            throw new DuplicateCodeException("LearningVideo", command.youtubeVideoId());
        }

        LearningVideo video = LearningVideo.create(
                command.youtubeVideoId(),
                command.youtubeUrl(),
                command.title(),
                command.description(),
                command.thumbnailUrl(),
                command.durationSeconds(),
                command.topicId(),
                command.level(),
                command.requiredFeatureKey(),
                command.createdBy()
        );

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
