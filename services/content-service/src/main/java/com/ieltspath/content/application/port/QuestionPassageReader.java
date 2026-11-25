package com.ieltspath.content.application.port;

import com.ieltspath.content.domain.vo.AccessTier;
import com.ieltspath.content.domain.vo.AssetType;

import java.util.List;
import java.util.UUID;

/**
 * Finds the passages and the audio that go with a question version. A question holds no link to its passage or audio; the
 * asset is tied to it in one of three ways, listed here from the most direct.
 */
public interface QuestionPassageReader {

    /** Where an asset was found: linked to the version itself, before the version's exercise block, or on its section. */
    enum Source { DIRECT, LESSON, SECTION }

    /**
     * A reading passage ({@code type} PASSAGE, {@code text} is the passage) or a recording ({@code type} AUDIO,
     * {@code text} is its transcript when there is one). {@code mediaReference} and {@code tier} are what the reader
     * finds for audio; {@code mediaUrl} is filled in by the use case once the reference is turned into a playable link.
     * {@code sourceName} is the lesson or section title; null for a direct link.
     */
    record Passage(UUID assetId, AssetType type, String text, Source source, String sourceName, String mediaReference,
                   AccessTier tier, Integer durationSeconds, String mediaUrl) {

        /** A reading passage. */
        public Passage(UUID assetId, String text, Source source, String sourceName) {
            this(assetId, AssetType.PASSAGE, text, source, sourceName, null, null, null, null);
        }

        public Passage withMediaUrl(String url) {
            return new Passage(assetId, type, text, source, sourceName, mediaReference, tier, durationSeconds, url);
        }
    }

    /** The assets of the version, each once, most direct source first. Empty when none is found. */
    List<Passage> passagesOf(UUID questionVersionId);
}
