package com.group01.library.application.usecase;

import com.group01.library.application.command.CreateLearningVideoCommand;
import com.group01.library.application.result.LearningVideoResult;
import com.group01.library.application.port.TopicLookup;
import com.group01.library.domain.aggregate.LearningVideo;
import com.group01.library.domain.exception.DuplicateCodeException;
import com.group01.library.domain.repository.LearningVideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateLearningVideoUseCase {

    private final LearningVideoRepository learningVideoRepository;
    private final TopicLookup topicLookup;

    public CreateLearningVideoUseCase(LearningVideoRepository learningVideoRepository, TopicLookup topicLookup) {
        this.learningVideoRepository = learningVideoRepository;
        this.topicLookup = topicLookup;
    }

    public LearningVideoResult execute(CreateLearningVideoCommand command) {
        if (command.topicId() != null && !topicLookup.exists(command.topicId())) {
            throw new IllegalArgumentException("Topic not found: " + command.topicId());
        }
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
