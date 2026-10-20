package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.VideoDictationProgressResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VideoDictationProgressResponse(UUID videoId, int totalSegments, int passedSegments, UUID nextSegmentId,
                                             Integer nextSequenceNo, List<SegmentProgress> segments) {

    public record SegmentProgress(UUID segmentId, int sequenceNo, int attempts, BigDecimal bestAccuracyPercent,
                                  boolean passed) {
    }

    public static VideoDictationProgressResponse from(VideoDictationProgressResult result) {
        return new VideoDictationProgressResponse(result.videoId(), result.totalSegments(), result.passedSegments(),
                result.nextSegmentId(), result.nextSequenceNo(),
                result.segments().stream()
                        .map(s -> new SegmentProgress(s.segmentId(), s.sequenceNo(), s.attempts(),
                                s.bestAccuracyPercent(), s.passed()))
                        .toList());
    }
}
