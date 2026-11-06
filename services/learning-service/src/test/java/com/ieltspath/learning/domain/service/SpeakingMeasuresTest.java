package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.SpokenWord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpeakingMeasuresTest {
    private static SpokenWord word(String text, long start, long end, double accuracy, String error) {
        return new SpokenWord(text, start, end - start, accuracy, error);
    }

    @Test
    void rhythmCountsPausesBetweenWordsNotInsideThem() {
        List<SpokenWord> words = List.of(
                word("one", 0, 400, 90, "None"), word("two", 400, 800, 90, "None"),
                word("three", 1500, 1900, 90, "None"), word("four", 1900, 2300, 90, "None"),
                word("five", 2300, 2700, 90, "None"), word("six", 3000, 3400, 90, "None"));

        SpeakingRhythm rhythm = SpeakingRhythm.of(words, 500);

        assertEquals(6, rhythm.wordCount());
        assertEquals(3400, rhythm.speakingTimeMs());
        assertEquals(6 * 60_000.0 / 3400, rhythm.wordsPerMinute(), 1e-9);
        // Only the 700 ms silence reaches 500 ms; the 300 ms one is an ordinary breath.
        assertEquals(1, rhythm.longPauses());
        assertEquals(700, rhythm.longestPauseMs());
        assertEquals(3.0, rhythm.meanRunWords(), 1e-9);
        assertEquals(1000.0 / 3400, rhythm.pauseRatio(), 1e-9);
    }

    @Test
    void tooFewWordsGiveNoSpeakingRate() {
        SpeakingRhythm rhythm = SpeakingRhythm.of(List.of(word("a", 0, 300, 90, "None"), word("b", 300, 600, 90, "None"),
                word("c", 600, 900, 90, "None")), 500);

        assertEquals(0, rhythm.wordsPerMinute());
        assertEquals(3, rhythm.wordCount());
    }

    @Test
    void nothingSaidGivesAllZeros() {
        assertEquals(new SpeakingRhythm(0, 0, 0, 0, 0, 0, 0), SpeakingRhythm.of(List.of(), 500));
    }

    @Test
    void wordsThatOverlapAreNotNegativePauses() {
        SpeakingRhythm rhythm = SpeakingRhythm.of(List.of(word("a", 0, 500, 90, "None"), word("b", 400, 900, 90, "None"),
                word("c", 900, 1300, 90, "None"), word("d", 1300, 1700, 90, "None"), word("e", 1700, 2100, 90, "None")), 500);

        assertEquals(0, rhythm.longestPauseMs());
        assertEquals(0, rhythm.pauseRatio());
    }

    @Test
    void weakWordsAreTheWorstScoredEachOnceWithoutFunctionWords() {
        List<SpokenWord> words = List.of(
                word("the", 0, 100, 10, "None"),
                word("pronunciation", 100, 900, 35, "Mispronunciation"),
                word("teacher", 900, 1300, 55, "None"),
                word("Pronunciation", 1300, 2100, 20, "Mispronunciation"),
                word("good", 2100, 2400, 95, "None"),
                word("vocabulary", 2400, 3100, 80, "Omission"),
                word("garden", 3100, 3500, 59.9, "None"),
                word("window", 3500, 3900, 60, "None"));

        List<SpokenWord> weak = WeakWords.select(words, 60, 10);

        // Function words are dropped, a repeated word is kept once at its worst score, an error counts whatever the score,
        // and 60 itself is not below 60.
        assertEquals(List.of("Pronunciation", "teacher", "garden", "vocabulary"), weak.stream().map(SpokenWord::text).toList());
    }

    @Test
    void weakWordsAreCapped() {
        List<SpokenWord> words = List.of(word("alpha", 0, 100, 10, "None"), word("beta", 100, 200, 20, "None"),
                word("gamma", 200, 300, 30, "None"));

        assertEquals(List.of("alpha", "beta"), WeakWords.select(words, 60, 2).stream().map(SpokenWord::text).toList());
    }

    @Test
    void theBandTableStepsUpAtItsBoundaries() {
        PronunciationBandMapper mapper = PronunciationBandMapper.INTERIM;

        assertEquals(new BigDecimal("3.0"), mapper.band(0));
        assertEquals(new BigDecimal("3.0"), mapper.band(39.9));
        assertEquals(new BigDecimal("4.0"), mapper.band(40));
        assertEquals(new BigDecimal("6.5"), mapper.band(79.5));
        assertEquals(new BigDecimal("7.0"), mapper.band(84.3));
        assertEquals(new BigDecimal("7.5"), mapper.band(92.7));
        assertEquals(new BigDecimal("8.0"), mapper.band(93));
        assertEquals(new BigDecimal("8.0"), mapper.band(100));
        assertEquals("pr-interim-1", mapper.version());
    }

    @Test
    void aHigherScoreNeverGivesALowerBand() {
        BigDecimal previous = BigDecimal.ZERO;
        for (double score = 0; score <= 100; score += 0.5) {
            BigDecimal band = PronunciationBandMapper.INTERIM.band(score);
            org.junit.jupiter.api.Assertions.assertTrue(band.compareTo(previous) >= 0, "score " + score);
            previous = band;
        }
    }

    @Test
    void aBandTableMustRiseAndNotBeEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new PronunciationBandMapper("x", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PronunciationBandMapper("x", List.of(
                new PronunciationBandMapper.Step(50, new BigDecimal("5.0")),
                new PronunciationBandMapper.Step(50, new BigDecimal("6.0")))));
    }
}
