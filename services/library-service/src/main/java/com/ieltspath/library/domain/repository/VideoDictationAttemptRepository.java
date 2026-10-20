package com.ieltspath.library.domain.repository;

import com.ieltspath.library.domain.aggregate.VideoDictationAttempt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface VideoDictationAttemptRepository {
    VideoDictationAttempt save(VideoDictationAttempt attempt);

    /** Highest attempt number the learner already has for the segment, or 0 when there is none. */
    int maxAttemptNo(UUID userId, UUID segmentId);

    /** All attempts of the learner on one video, oldest first. */
    List<VideoDictationAttempt> findByUserIdAndVideoId(UUID userId, UUID videoId);

    /** For each given video, how many distinct segments the learner has passed; videos without a pass are absent. */
    Map<UUID, Integer> countPassedSegmentsByVideo(UUID userId, Collection<UUID> videoIds);

    /** For each given video, when the learner last typed an answer on it; videos they never practised are absent. */
    Map<UUID, Instant> lastAttemptAtByVideo(UUID userId, Collection<UUID> videoIds);
}
