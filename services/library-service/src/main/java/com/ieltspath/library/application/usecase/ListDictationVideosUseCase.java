package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.result.DictationVideoListItemResult;
import com.ieltspath.library.domain.repository.DictationVideoRepository;
import com.ieltspath.library.domain.repository.VideoDictationAttemptRepository;
import com.ieltspath.library.domain.vo.DictationVideoSummary;
import com.ieltspath.library.domain.vo.VideoLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListDictationVideosUseCase {
    private final DictationVideoRepository videoRepository;
    private final VideoDictationAttemptRepository attemptRepository;

    /**
     * Published videos (one level when {@code level} is given) with the learner's passed-sentence count and the
     * time of their latest answer on each.
     */
    public List<DictationVideoListItemResult> execute(UUID userId, VideoLevel level) {
        List<DictationVideoSummary> videos = videoRepository.findPublished(level);
        List<UUID> videoIds = videos.stream().map(DictationVideoSummary::id).toList();
        Map<UUID, Integer> passed = attemptRepository.countPassedSegmentsByVideo(userId, videoIds);
        Map<UUID, Instant> lastPracticed = attemptRepository.lastAttemptAtByVideo(userId, videoIds);
        return videos.stream()
                .map(video -> new DictationVideoListItemResult(video.id(), video.youtubeVideoId(), video.title(),
                        video.thumbnailUrl(), video.durationSeconds(), video.level(), video.totalSegments(),
                        passed.getOrDefault(video.id(), 0), lastPracticed.get(video.id())))
                .toList();
    }
}
