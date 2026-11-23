package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.port.QuestionPassageReader.Passage;

import java.util.UUID;

/**
 * A reading passage or a recording that goes with a question; {@code source} is DIRECT, LESSON or SECTION and
 * {@code type} is PASSAGE or AUDIO. For audio, {@code text} is the transcript (may be null) and {@code mediaUrl} a
 * playable link (null when it could not be made).
 */
public record QuestionPassageResponse(UUID assetId, String text, String source, String sourceName, String type,
                                      String mediaUrl, Integer durationSeconds) {

    public static QuestionPassageResponse from(Passage passage) {
        return new QuestionPassageResponse(passage.assetId(), passage.text(), passage.source().name(), passage.sourceName(),
                passage.type().name(), passage.mediaUrl(), passage.durationSeconds());
    }
}
