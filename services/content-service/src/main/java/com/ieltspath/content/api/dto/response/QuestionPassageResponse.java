package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.port.QuestionPassageReader.Passage;

import java.util.UUID;

/** A reading passage that goes with a question; {@code source} is DIRECT, LESSON or SECTION. */
public record QuestionPassageResponse(UUID assetId, String text, String source, String sourceName) {

    public static QuestionPassageResponse from(Passage passage) {
        return new QuestionPassageResponse(passage.assetId(), passage.text(), passage.source().name(), passage.sourceName());
    }
}
