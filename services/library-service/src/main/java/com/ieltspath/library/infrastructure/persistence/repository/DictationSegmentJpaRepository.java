package com.ieltspath.library.infrastructure.persistence.repository;

import com.ieltspath.library.infrastructure.persistence.entity.VideoSegmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DictationSegmentJpaRepository extends JpaRepository<VideoSegmentJpaEntity, UUID> {
    List<VideoSegmentJpaEntity> findByVideoIdOrderBySequenceNoAsc(UUID videoId);

    @Query("select s.videoId from VideoSegmentJpaEntity s where s.id = :segmentId")
    Optional<UUID> findVideoIdById(@Param("segmentId") UUID segmentId);

    @Query("select s.videoId as videoId, count(s) as total from VideoSegmentJpaEntity s "
            + "where s.videoId in :videoIds group by s.videoId")
    List<SegmentCount> countByVideoIds(@Param("videoIds") Collection<UUID> videoIds);

    interface SegmentCount {
        UUID getVideoId();

        long getTotal();
    }
}
