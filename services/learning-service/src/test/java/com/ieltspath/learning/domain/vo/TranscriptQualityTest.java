package com.ieltspath.learning.domain.vo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptQualityTest {

    @Test
    void highConfidenceAndFewWeakWordsIsHigh() {
        assertThat(TranscriptQuality.of(List.of(analysis(0.95, 90, 20))).level()).isEqualTo(TranscriptQuality.Level.HIGH);
    }

    @Test
    void lowConfidenceIsLow() {
        assertThat(TranscriptQuality.of(List.of(analysis(0.80, 90, 20))).level()).isEqualTo(TranscriptQuality.Level.LOW);
    }

    @Test
    void manyWeaklyPronouncedWordsIsLowEvenWithGoodConfidence() {
        assertThat(TranscriptQuality.of(List.of(analysis(0.95, 40, 20))).level()).isEqualTo(TranscriptQuality.Level.LOW);
    }

    @Test
    void inBetweenIsMediumAndUnknownConfidenceIsMedium() {
        assertThat(TranscriptQuality.of(List.of(analysis(0.88, 90, 20))).level()).isEqualTo(TranscriptQuality.Level.MEDIUM);
        assertThat(TranscriptQuality.of(List.of(analysis(null, 90, 20))).level()).isEqualTo(TranscriptQuality.Level.MEDIUM);
        assertThat(TranscriptQuality.of(List.of(analysis(null, 90, 20))).meanConfidence()).isNull();
    }

    private static SpeechAnalysis analysis(Double confidence, double wordAccuracy, int words) {
        List<SpokenWord> spoken = new ArrayList<>();
        for (int index = 0; index < words; index++) spoken.add(new SpokenWord("w" + index, index * 300L, 250, wordAccuracy, "None"));
        return new SpeechAnalysis(List.of(new SpeechSegment("text", wordAccuracy, wordAccuracy, null, 100, wordAccuracy,
                confidence, spoken)));
    }
}
