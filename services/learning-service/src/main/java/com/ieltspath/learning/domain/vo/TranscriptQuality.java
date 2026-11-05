package com.ieltspath.learning.domain.vo;

import java.util.List;

/** Confidence in the recognized words, separate from pronunciation performance. */
public record TranscriptQuality(Level level, Double meanConfidence, double lowAccuracyWordRatio) {
    public enum Level { HIGH, MEDIUM, LOW }

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
        Level level = mean == null ? Level.MEDIUM
                : mean >= 0.92 && lowRatio <= 0.05 ? Level.HIGH
                : mean < 0.85 || lowRatio > 0.10 ? Level.LOW : Level.MEDIUM;
        return new TranscriptQuality(level, mean, lowRatio);
    }
}
