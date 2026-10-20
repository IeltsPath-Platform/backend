package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.DictationVideoDetailResult;
import com.ieltspath.library.domain.vo.VideoLevel;

import java.util.List;
import java.util.UUID;

public record DictationVideoDetailResponse(UUID id, String youtubeVideoId, String youtubeUrl, String title,
                                           String description, String thumbnailUrl, Integer durationSeconds,
                                           VideoLevel level, List<Segment> segments) {

    /** {@code ipaWords[i]} is the pronunciation of the i-th word of {@code transcript}, or null when it has none. */
    public record Segment(UUID id, int sequenceNo, int startMs, int endMs, String transcript, String translationVi,
                          List<String> ipaWords) {
    }

    public static DictationVideoDetailResponse from(DictationVideoDetailResult result) {
        return new DictationVideoDetailResponse(result.id(), result.youtubeVideoId(), result.youtubeUrl(),
                result.title(), result.description(), result.thumbnailUrl(), result.durationSeconds(), result.level(),
                result.segments().stream()
                        .map(s -> new Segment(s.id(), s.sequenceNo(), s.startMs(), s.endMs(), s.transcript(),
                                s.translationVi(), s.ipaWords()))
                        .toList());
    }
}
