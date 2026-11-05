package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import com.ieltspath.learning.domain.vo.SpokenWord;

import java.util.List;

/**
 * How fast and how steadily the learner spoke, measured from the time of every word: a number for Fluency that does not
 * depend on what was said. A pause is the silence between the end of one word and the start of the next.
 *
 * @param speakingTimeMs   from the start of the first word to the end of the last, pauses included
 * @param wordsPerMinute   words over speaking time; 0 when too little was said to measure
 * @param longPauses       pauses of at least {@code longPauseMs}
 * @param longestPauseMs   the longest silence between two words
 * @param meanRunWords     words said between two long pauses, on average
 * @param pauseRatio       share of the speaking time spent in silence (0 to 1)
 */
public record SpeakingRhythm(int wordCount, long speakingTimeMs, double wordsPerMinute, int longPauses,
                             long longestPauseMs, double meanRunWords, double pauseRatio) {

    /** Silence this long between two words counts as a hesitation. */
    public static final long LONG_PAUSE_MS = 500;
    /** Fewer words than this give a rate that says nothing. */
    private static final int MIN_WORDS_TO_MEASURE = 5;

    /**
     * The rhythm of several recordings taken as one speech. Silence between recordings is not counted: each ends and the
     * next begins when the learner chose, so only pauses inside a recording are pauses in speaking.
     */
    public static SpeakingRhythm combine(List<SpeakingRhythm> rhythms) {
        int words = 0;
        long time = 0;
        int longPauses = 0;
        long longest = 0;
        double silence = 0;
        int recordings = 0;
        for (SpeakingRhythm rhythm : rhythms) {
            if (rhythm.wordCount() == 0) continue;
            recordings++;
            words += rhythm.wordCount();
            time += rhythm.speakingTimeMs();
            longPauses += rhythm.longPauses();
            longest = Math.max(longest, rhythm.longestPauseMs());
            silence += rhythm.pauseRatio() * rhythm.speakingTimeMs();
        }
        if (recordings == 0) return new SpeakingRhythm(0, 0, 0, 0, 0, 0, 0);
        double perMinute = words < MIN_WORDS_TO_MEASURE || time == 0 ? 0 : words * 60_000.0 / time;
        return new SpeakingRhythm(words, time, perMinute, longPauses, longest, (double) words / (longPauses + recordings),
                time == 0 ? 0 : Math.min(1.0, silence / time));
    }

    public static SpeakingRhythm of(SpeechAnalysis analysis) {
        return of(analysis.words(), LONG_PAUSE_MS);
    }

    public static SpeakingRhythm of(List<SpokenWord> words, long longPauseMs) {
        if (words.isEmpty()) return new SpeakingRhythm(0, 0, 0, 0, 0, 0, 0);
        long start = words.get(0).startMs();
        long end = words.get(words.size() - 1).endMs();
        long speakingTime = Math.max(0, end - start);

        int longPauses = 0;
        long longest = 0;
        long silence = 0;
        for (int index = 1; index < words.size(); index++) {
            long gap = Math.max(0, words.get(index).startMs() - words.get(index - 1).endMs());
            silence += gap;
            longest = Math.max(longest, gap);
            if (gap >= longPauseMs) longPauses++;
        }
        double perMinute = words.size() < MIN_WORDS_TO_MEASURE || speakingTime == 0 ? 0
                : words.size() * 60_000.0 / speakingTime;
        double ratio = speakingTime == 0 ? 0 : Math.min(1.0, (double) silence / speakingTime);
        return new SpeakingRhythm(words.size(), speakingTime, perMinute, longPauses, longest,
                (double) words.size() / (longPauses + 1), ratio);
    }
}
