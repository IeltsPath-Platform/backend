package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.vo.VideoLevel;

import java.util.List;
import java.util.UUID;

/** A video with the sentences the player pauses on; everything the frontend needs to run a dictation. */
public record DictationVideoDetailResult(UUID id, String youtubeVideoId, String youtubeUrl, String title,
                                         String description, String thumbnailUrl, Integer durationSeconds,
                                         VideoLevel level, List<Segment> segments) {

    /**
     * {@code ipaWords} has one entry per word of {@code transcript}, in order (see SentenceWords): the pronunciation
     * of that word, or {@code null} when it has none.
     */
    public record Segment(UUID id, int sequenceNo, int startMs, int endMs, String transcript, String translationVi,
                          List<String> ipaWords) {
    }
}
