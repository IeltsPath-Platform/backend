package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.DictationVideoListItemResult;
import com.ieltspath.library.domain.vo.VideoLevel;

import java.time.Instant;
import java.util.UUID;

public record DictationVideoListItemResponse(UUID id, String youtubeVideoId, String title, String thumbnailUrl,
                                             Integer durationSeconds, VideoLevel level, int totalSegments,
                                             int passedSegments, Instant lastPracticedAt) {
    public static DictationVideoListItemResponse from(DictationVideoListItemResult result) {
        return new DictationVideoListItemResponse(result.id(), result.youtubeVideoId(), result.title(),
                result.thumbnailUrl(), result.durationSeconds(), result.level(), result.totalSegments(),
                result.passedSegments(), result.lastPracticedAt());
    }
}
