package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.port.QuestionPassageReader.Passage;

import java.util.UUID;

/**
 * A reading passage, a recording or a prompt image that goes with a question; {@code source} is DIRECT, LESSON or
 * SECTION and {@code type} is PASSAGE, AUDIO or IMAGE. For audio, {@code text} is the transcript (may be null) and
 * {@code mediaUrl} a playable link (null when it could not be made). For an image, {@code text} is its description and
 * {@code mediaUrl} the link to show it (a Writing chart is always a direct link).
 */
public record QuestionPassageResponse(UUID assetId, String text, String source, String sourceName, String type,
                                      String mediaUrl, Integer durationSeconds) {

    public static QuestionPassageResponse from(Passage passage) {
        return new QuestionPassageResponse(passage.assetId(), passage.text(), passage.source().name(), passage.sourceName(),
                passage.type().name(), passage.mediaUrl(), passage.durationSeconds());
    }
}
