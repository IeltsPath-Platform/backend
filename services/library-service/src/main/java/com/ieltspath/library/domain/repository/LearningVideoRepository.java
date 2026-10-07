package com.ieltspath.library.domain.repository;

import com.ieltspath.library.domain.aggregate.LearningVideo;
import com.ieltspath.library.domain.vo.PublicationStatus;

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
