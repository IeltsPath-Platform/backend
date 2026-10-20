package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.vo.VideoLevel;

import java.time.Instant;
import java.util.UUID;

/**
 * One library card: the video, how many of its sentences the learner has already passed and when they last typed
 * an answer on it ({@code null} when they never did), which is what "continue watching" sorts by.
 */
public record DictationVideoListItemResult(UUID id, String youtubeVideoId, String title, String thumbnailUrl,
                                           Integer durationSeconds, VideoLevel level, int totalSegments,
                                           int passedSegments, Instant lastPracticedAt) {
}
