package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.domain.aggregate.LearningVideo;
import com.ieltspath.library.domain.entity.VideoSegment;
import com.ieltspath.library.domain.repository.DictationVideoRepository;
import com.ieltspath.library.domain.vo.DictationVideoSummary;
import com.ieltspath.library.domain.vo.PublicationStatus;
import com.ieltspath.library.domain.vo.VideoLevel;
import com.ieltspath.library.infrastructure.persistence.entity.LearningVideoJpaEntity;
import com.ieltspath.library.infrastructure.persistence.entity.VideoSegmentJpaEntity;
import com.ieltspath.library.infrastructure.persistence.repository.DictationSegmentJpaRepository;
import com.ieltspath.library.infrastructure.persistence.repository.DictationVideoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DictationVideoRepositoryAdapter implements DictationVideoRepository {
    private final DictationVideoJpaRepository videos;
    private final DictationSegmentJpaRepository segments;

    @Override
    public List<DictationVideoSummary> findPublished(VideoLevel level) {
        List<LearningVideoJpaEntity> found = level == null
                ? videos.findByStatusOrderByLevelAscTitleAsc(PublicationStatus.PUBLISHED)
                : videos.findByStatusAndLevelOrderByTitleAsc(PublicationStatus.PUBLISHED, level);
        if (found.isEmpty()) {
            return List.of();
        }
        Map<UUID, Integer> counts = new HashMap<>();
        segments.countByVideoIds(found.stream().map(LearningVideoJpaEntity::getId).toList())
                .forEach(count -> counts.put(count.getVideoId(), (int) count.getTotal()));
        return found.stream()
                .map(video -> new DictationVideoSummary(video.getId(), video.getYoutubeVideoId(), video.getTitle(),
                        video.getThumbnailUrl(), video.getDurationSeconds(), video.getLevel(),
                        counts.getOrDefault(video.getId(), 0)))
                .toList();
    }

    @Override
    public Optional<LearningVideo> findPublishedById(UUID id) {
        return videos.findByIdAndStatus(id, PublicationStatus.PUBLISHED).map(video -> new LearningVideo(
                video.getId(), video.getYoutubeVideoId(), video.getYoutubeUrl(), video.getTitle(),
                video.getDescription(), video.getThumbnailUrl(), video.getDurationSeconds(), video.getTopicId(),
                video.getLevel(), video.getRequiredFeatureKey(), video.getStatus(), video.getCreatedBy(),
                video.getCreatedAt(), video.getUpdatedAt(),
                segments.findByVideoIdOrderBySequenceNoAsc(video.getId()).stream().map(this::toSegment).toList()));
    }

    @Override
    public Optional<UUID> findPublishedVideoIdBySegmentId(UUID segmentId) {
        return segments.findVideoIdById(segmentId)
                .flatMap(videoId -> videos.findByIdAndStatus(videoId, PublicationStatus.PUBLISHED))
                .map(LearningVideoJpaEntity::getId);
    }

    private VideoSegment toSegment(VideoSegmentJpaEntity segment) {
        return new VideoSegment(segment.getId(), segment.getVideoId(), segment.getSequenceNo(), segment.getStartMs(),
                segment.getEndMs(), segment.getTranscript(), segment.getTranslationVi(), segment.getCreatedAt(),
                segment.getUpdatedAt(), List.of());
    }
}
