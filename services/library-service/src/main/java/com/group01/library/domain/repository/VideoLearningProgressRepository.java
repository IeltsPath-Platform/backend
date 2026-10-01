package com.group01.library.domain.repository;

import com.group01.library.domain.aggregate.VideoLearningProgress;
import com.group01.library.domain.vo.OwnedPage;

import java.util.Optional;
import java.util.UUID;

public interface VideoLearningProgressRepository {
    Optional<VideoLearningProgress> findByUserIdAndVideoId(UUID userId, UUID videoId);

    Optional<VideoLearningProgress> findByIdAndUserId(UUID id, UUID userId);

    OwnedPage<VideoLearningProgress> findByUserId(UUID userId, int page, int size);

    VideoLearningProgress save(VideoLearningProgress progress);

    void delete(VideoLearningProgress progress);
}
