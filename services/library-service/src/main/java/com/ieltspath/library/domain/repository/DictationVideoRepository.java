package com.ieltspath.library.domain.repository;

import com.ieltspath.library.domain.aggregate.LearningVideo;
import com.ieltspath.library.domain.vo.DictationVideoSummary;
import com.ieltspath.library.domain.vo.VideoLevel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read side of video dictation: only published videos, segments without lexical entries. It is separate from
 * {@link LearningVideoRepository} because that port loads segments and lexical entries in one graph.
 */
public interface DictationVideoRepository {
    /** Published videos, optionally one level, ordered by level then title. */
    List<DictationVideoSummary> findPublished(VideoLevel level);

    /** A published video with its segments ordered by sequence; lexical entries are left empty. */
    Optional<LearningVideo> findPublishedById(UUID id);

    /** Id of the published video the segment belongs to; empty when the segment is unknown or its video unpublished. */
    Optional<UUID> findPublishedVideoIdBySegmentId(UUID segmentId);
}
