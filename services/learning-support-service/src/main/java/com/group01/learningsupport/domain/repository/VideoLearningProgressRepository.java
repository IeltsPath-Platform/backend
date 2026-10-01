package com.group01.learningsupport.domain.repository;

import com.group01.learningsupport.domain.aggregate.VideoLearningProgress;
import com.group01.learningsupport.domain.vo.OwnedPage;

import java.util.Optional;
import java.util.UUID;

public interface VideoLearningProgressRepository {
    Optional<VideoLearningProgress> findByUserIdAndVideoId(UUID userId, UUID videoId);

    Optional<VideoLearningProgress> findByIdAndUserId(UUID id, UUID userId);

    OwnedPage<VideoLearningProgress> findByUserId(UUID userId, int page, int size);

    VideoLearningProgress save(VideoLearningProgress progress);

    void delete(VideoLearningProgress progress);
}
