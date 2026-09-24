package com.group01.content.application.usecase;

import com.group01.content.application.result.LearningVideoResult;
import com.group01.content.domain.aggregate.LearningVideo;
import com.group01.content.domain.repository.LearningVideoRepository;
import com.group01.content.domain.vo.PublicationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListLearningVideosUseCase {

    private final LearningVideoRepository learningVideoRepository;

    public ListLearningVideosUseCase(LearningVideoRepository learningVideoRepository) {
        this.learningVideoRepository = learningVideoRepository;
    }

    public List<LearningVideoResult> execute(Boolean featureRequired, PublicationStatus status) {
        List<LearningVideo> videos = learningVideoRepository.findAll(featureRequired, status);
        return videos.stream()
                .map(v -> new LearningVideoResult(
                        v.getId(),
                        v.getYoutubeVideoId(),
                        v.getYoutubeUrl(),
                        v.getTitle(),
                        v.getDescription(),
                        v.getThumbnailUrl(),
                        v.getDurationSeconds(),
                        v.getTopicId(),
                        v.getLevel(),
                        v.getRequiredFeatureKey(),
                        v.getStatus(),
                        v.getCreatedBy(),
                        v.getCreatedAt(),
                        v.getUpdatedAt()
                ))
                .toList();
    }
}
