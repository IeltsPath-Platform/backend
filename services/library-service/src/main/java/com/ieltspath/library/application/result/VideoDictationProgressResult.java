package com.ieltspath.library.application.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Where a learner stands in one video, derived from the stored dictation attempts.
 * {@code nextSegmentId} / {@code nextSequenceNo} point at the first segment not passed yet; both are null when every
 * segment is passed.
 */
public record VideoDictationProgressResult(UUID videoId, int totalSegments, int passedSegments, UUID nextSegmentId,
                                           Integer nextSequenceNo, List<SegmentProgress> segments) {

    public record SegmentProgress(UUID segmentId, int sequenceNo, int attempts, BigDecimal bestAccuracyPercent,
                                  boolean passed) {
    }
}
