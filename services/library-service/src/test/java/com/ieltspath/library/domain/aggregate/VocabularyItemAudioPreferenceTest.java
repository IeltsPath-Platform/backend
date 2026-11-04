package com.ieltspath.library.domain.aggregate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VocabularyItemAudioPreferenceTest {
    @Test
    void prefersUsBeforeUkAndLegacy() {
        assertEquals("us.mp3", VocabularyItem.preferredAudioReference(" us.mp3 ", "uk.mp3", "legacy.mp3"));
    }

    @Test
    void fallsBackToUkWhenUsIsMissingOrBlank() {
        assertEquals("uk.mp3", VocabularyItem.preferredAudioReference("  ", " uk.mp3 ", "legacy.mp3"));
    }

    @Test
    void fallsBackToLegacyWhenUsAndUkAreMissingOrBlank() {
        assertEquals("legacy.mp3", VocabularyItem.preferredAudioReference(null, "\t", " legacy.mp3 "));
    }

    @Test
    void returnsNullWhenEveryReferenceIsMissingOrBlank() {
        assertNull(VocabularyItem.preferredAudioReference(" ", "\t", ""));
    }
}
