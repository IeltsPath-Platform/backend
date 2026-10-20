package com.ieltspath.library.infrastructure.persistence.repository;

import com.ieltspath.library.infrastructure.persistence.entity.VideoDictationAttemptJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface VideoDictationAttemptJpaRepository extends JpaRepository<VideoDictationAttemptJpaEntity, UUID> {
    @Query("select coalesce(max(a.attemptNo), 0) from VideoDictationAttemptJpaEntity a "
            + "where a.userId = :userId and a.segmentId = :segmentId")
    int maxAttemptNo(@Param("userId") UUID userId, @Param("segmentId") UUID segmentId);

    List<VideoDictationAttemptJpaEntity> findByUserIdAndVideoIdOrderByCreatedAtAscAttemptNoAsc(UUID userId, UUID videoId);

    @Query("select a.videoId as videoId, count(distinct a.segmentId) as passed from VideoDictationAttemptJpaEntity a "
            + "where a.userId = :userId and a.correct = true and a.videoId in :videoIds group by a.videoId")
    List<PassedCount> countPassedByVideo(@Param("userId") UUID userId, @Param("videoIds") Collection<UUID> videoIds);

    @Query("select a.videoId as videoId, max(a.createdAt) as lastAt from VideoDictationAttemptJpaEntity a "
            + "where a.userId = :userId and a.videoId in :videoIds group by a.videoId")
    List<LastAttempt> lastAttemptAtByVideo(@Param("userId") UUID userId, @Param("videoIds") Collection<UUID> videoIds);

    interface PassedCount {
        UUID getVideoId();

        long getPassed();
    }

    interface LastAttempt {
        UUID getVideoId();

        Instant getLastAt();
    }
}
