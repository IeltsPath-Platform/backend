package com.ieltspath.learning.domain.vo;

import java.util.List;

/** Confidence in the recognized words, separate from pronunciation performance. */
public record TranscriptQuality(Level level, Double meanConfidence, double lowAccuracyWordRatio) {
    public enum Level { HIGH, MEDIUM, LOW }

    private static final double HIGH_CONFIDENCE = 0.92;
    private static final double LOW_CONFIDENCE = 0.85;
    private static final double HIGH_MAX_LOW_RATIO = 0.05;

    public static TranscriptQuality of(List<SpeechAnalysis> analyses) {
        double confidenceTotal = 0;
        int confidenceCount = 0;
        long words = 0;
        long lowWords = 0;
        for (SpeechAnalysis analysis : analyses) {
            Double confidence = analysis.meanConfidence();
            if (confidence != null) {
                int weight = Math.max(1, analysis.words().size());
                confidenceTotal += confidence * weight;
                confidenceCount += weight;
            }
            words += analysis.words().size();
            lowWords += analysis.words().stream().filter(word -> word.accuracy() < 60).count();
        }
        Double mean = confidenceCount == 0 ? null : confidenceTotal / confidenceCount;
        double lowRatio = words == 0 ? 0 : (double) lowWords / words;
        // Only what the recogniser was unsure of makes the text unreliable. Many badly pronounced words say something
        // about the learner, not about the transcript: they keep it from being HIGH, but they must not make it LOW, or a
        // weak speaker would be warned about the transcript and have the result kept out of the learning path.
        Level level = mean == null ? Level.MEDIUM
                : mean < LOW_CONFIDENCE ? Level.LOW
                : mean >= HIGH_CONFIDENCE && lowRatio <= HIGH_MAX_LOW_RATIO ? Level.HIGH : Level.MEDIUM;
        return new TranscriptQuality(level, mean, lowRatio);
    }
}
