package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.VideoDictationProgressResult;
import com.ieltspath.library.application.result.VideoDictationProgressResult.SegmentProgress;
import com.ieltspath.library.domain.aggregate.LearningVideo;
import com.ieltspath.library.domain.aggregate.VideoDictationAttempt;
import com.ieltspath.library.domain.entity.VideoSegment;
import com.ieltspath.library.domain.exception.VideoNotFoundException;
import com.ieltspath.library.domain.repository.DictationVideoRepository;
import com.ieltspath.library.domain.repository.VideoDictationAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetVideoDictationProgressUseCase {
    private final DictationVideoRepository videoRepository;
    private final VideoDictationAttemptRepository attemptRepository;

    public VideoDictationProgressResult execute(UUID userId, UUID videoId) {
        LearningVideo video = videoRepository.findPublishedById(videoId)
                .orElseThrow(() -> new VideoNotFoundException(videoId));
        Map<UUID, List<VideoDictationAttempt>> bySegment = attemptRepository.findByUserIdAndVideoId(userId, videoId)
                .stream()
                .collect(Collectors.groupingBy(VideoDictationAttempt::getSegmentId));

        List<SegmentProgress> segments = new ArrayList<>();
        for (VideoSegment segment : video.getSegments().stream()
                .sorted(Comparator.comparingInt(VideoSegment::getSequenceNo)).toList()) {
            List<VideoDictationAttempt> attempts = bySegment.getOrDefault(segment.getId(), List.of());
            BigDecimal best = attempts.stream()
                    .map(VideoDictationAttempt::getAccuracyPercent)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            segments.add(new SegmentProgress(segment.getId(), segment.getSequenceNo(), attempts.size(), best,
                    attempts.stream().anyMatch(VideoDictationAttempt::isCorrect)));
        }
        SegmentProgress next = segments.stream().filter(segment -> !segment.passed()).findFirst().orElse(null);
        return new VideoDictationProgressResult(
                video.getId(),
                segments.size(),
                (int) segments.stream().filter(SegmentProgress::passed).count(),
                next == null ? null : next.segmentId(),
                next == null ? null : next.sequenceNo(),
                segments
        );
    }
}
