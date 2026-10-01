package com.group01.content.domain.repository;

import com.group01.content.domain.aggregate.LearningVideo;
import com.group01.content.domain.vo.PublicationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningVideoRepository {
    LearningVideo save(LearningVideo video);
    Optional<LearningVideo> findById(UUID id);

    Optional<LearningVideo> findBySegmentId(UUID segmentId);
    Optional<LearningVideo> findByYoutubeVideoId(String youtubeVideoId);

    List<LearningVideo> findAll(Boolean featureRequired, PublicationStatus status);
    boolean existsByYoutubeVideoId(String youtubeVideoId);
}
