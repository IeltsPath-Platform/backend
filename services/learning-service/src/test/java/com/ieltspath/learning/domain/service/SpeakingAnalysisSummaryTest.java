package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import com.ieltspath.learning.domain.vo.SpeechSegment;
import com.ieltspath.learning.domain.vo.SpokenWord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeakingAnalysisSummaryTest {
    /** {@code count} words of 400 ms each, one every 500 ms starting at 0 (so a 100 ms gap between words). */
    private static SpeechAnalysis speech(int count, double pronunciation, Double prosody, double wordAccuracy) {
        List<SpokenWord> words = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            words.add(new SpokenWord("word" + index, index * 500L, 400, wordAccuracy, "None"));
        }
        return new SpeechAnalysis(List.of(new SpeechSegment("text", pronunciation, pronunciation, prosody, 100,
                pronunciation, words)));
    }

    @Test
    void scoresAreWeightedByWordsSoALongAnswerCountsForMore() {
        SpeakingAnalysisSummary summary = SpeakingAnalysisSummary.of(
                List.of(speech(30, 90, 80.0, 90), speech(10, 50, 40.0, 90)), PronunciationBandMapper.INTERIM, 5);

        assertEquals(80.0, summary.pronunciation(), 1e-9);
        assertEquals(80.0, summary.accuracy(), 1e-9);
        assertEquals(70.0, summary.prosody(), 1e-9);
        assertEquals(new BigDecimal("6.5"), summary.pronunciationBand());
        assertEquals("pr-interim-1", summary.bandTableVersion());
    }

    @Test
    void rhythmIsMeasuredWithinEachRecordingNotAcrossThem() {
        SpeakingAnalysisSummary summary = SpeakingAnalysisSummary.of(
                List.of(speech(10, 80, null, 90), speech(10, 80, null, 90)), PronunciationBandMapper.INTERIM, 5);

        SpeakingRhythm rhythm = summary.rhythm();
        assertEquals(20, rhythm.wordCount());
        // Each recording lasts 9 x 500 + 400 = 4900 ms; the time between the two recordings is not speaking time.
        assertEquals(9800, rhythm.speakingTimeMs());
        assertEquals(20 * 60_000.0 / 9800, rhythm.wordsPerMinute(), 1e-9);
        assertEquals(0, rhythm.longPauses());
        assertEquals(100, rhythm.longestPauseMs());
        assertEquals(20.0 / 2, rhythm.meanRunWords(), 1e-9);
    }

    @Test
    void anEmptyRecordingDoesNotCountAndNothingHeardGivesNoBand() {
        SpeechAnalysis silence = new SpeechAnalysis(List.of());

        SpeakingAnalysisSummary onlySilence = SpeakingAnalysisSummary.of(List.of(silence), PronunciationBandMapper.INTERIM, 5);
        assertNull(onlySilence.pronunciationBand());
        assertNull(onlySilence.prosody());
        assertEquals(0, onlySilence.rhythm().wordCount());
        assertTrue(onlySilence.weakWords().isEmpty());

        SpeakingAnalysisSummary mixed = SpeakingAnalysisSummary.of(List.of(silence, speech(10, 90, null, 90)),
                PronunciationBandMapper.INTERIM, 5);
        assertEquals(10, mixed.rhythm().wordCount());
        assertEquals(90, mixed.pronunciation(), 1e-9);
    }

    @Test
    void weakWordsComeFromEveryRecordingWorstFirstAndCapped() {
        SpeakingAnalysisSummary summary = SpeakingAnalysisSummary.of(
                List.of(speech(8, 70, null, 40), speech(8, 70, null, 20)), PronunciationBandMapper.INTERIM, 3);

        assertEquals(3, summary.weakWords().size());
        assertTrue(summary.weakWords().stream().allMatch(word -> word.accuracy() == 20));
    }
}
