package com.ieltspath.library.domain.vo;

import java.util.UUID;

/** A published video as the dictation library lists it. */
public record DictationVideoSummary(UUID id, String youtubeVideoId, String title, String thumbnailUrl,
                                    Integer durationSeconds, VideoLevel level, int totalSegments) {
}
