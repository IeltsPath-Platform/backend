package com.ieltspath.content.application.result;

import java.util.Map;
import java.util.UUID;

/**
 * What the author's package view needs beyond the aggregate: each section's passage and audio, and the stem, status
 * and purpose of each attached question, read for the whole package at once.
 */
public record AuthorPackageExtras(Map<UUID, SectionMedia> sections, Map<UUID, QuestionInfo> questions) {

    public static final AuthorPackageExtras EMPTY = new AuthorPackageExtras(Map.of(), Map.of());

    public SectionMedia media(UUID sectionId) {
        return sections.getOrDefault(sectionId, SectionMedia.NONE);
    }

    /** {@code null} when the question version is unknown, which only happens if it was removed meanwhile. */
    public QuestionInfo question(UUID sectionQuestionId) {
        return questions.get(sectionQuestionId);
    }

    public AuthorPackageExtras withAudioUrls(java.util.function.Function<String, String> resolver) {
        Map<UUID, SectionMedia> resolved = new java.util.LinkedHashMap<>();
        sections.forEach((id, media) -> resolved.put(id, media.withAudioUrl(resolver)));
        return new AuthorPackageExtras(resolved, questions);
    }

    public record SectionMedia(String passage, Audio audio) {
        public static final SectionMedia NONE = new SectionMedia(null, null);

        SectionMedia withAudioUrl(java.util.function.Function<String, String> resolver) {
            return audio == null ? this : new SectionMedia(passage, audio.withMediaUrl(resolver));
        }
    }

    public record Audio(UUID assetId, String mediaReference, String mediaUrl, Integer durationSeconds,
                        String transcript) {
        Audio withMediaUrl(java.util.function.Function<String, String> resolver) {
            String url;
            try {
                url = resolver.apply(mediaReference);
            } catch (RuntimeException unresolvable) {
                url = null;
            }
            return new Audio(assetId, mediaReference, url, durationSeconds, transcript);
        }
    }

    public record QuestionInfo(UUID questionId, String stem, String status, String purpose) {
    }
}
