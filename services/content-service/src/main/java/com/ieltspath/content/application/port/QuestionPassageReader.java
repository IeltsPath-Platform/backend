package com.ieltspath.content.application.port;

import java.util.List;
import java.util.UUID;

/**
 * Finds the reading passages that go with a question version. A question holds no link to its passage; the passage is
 * tied to it in one of three ways, listed here from the most direct.
 */
public interface QuestionPassageReader {

    /** Where a passage was found: linked to the version itself, before the version's exercise block, or on its section. */
    enum Source { DIRECT, LESSON, SECTION }

    /** {@code sourceName} is the lesson or section title; null for a direct link. */
    record Passage(UUID assetId, String text, Source source, String sourceName) {
    }

    /** The passages of the version, each asset once, most direct source first. Empty when none is found. */
    List<Passage> passagesOf(UUID questionVersionId);
}
