package com.ieltspath.learning.domain.vo;

import java.util.List;

/**
 * A stretch of speech the service set apart at a pause (about a sentence). Scores are 0 to 100; {@code prosody} is
 * null where the service gives none (only for some locales).
 */
public record SpeechSegment(String text, double accuracy, double fluency, Double prosody, double completeness,
                            double pronunciation, Double confidence, List<SpokenWord> words) {
    public SpeechSegment {
        words = words == null ? List.of() : List.copyOf(words);
    }

    public SpeechSegment(String text, double accuracy, double fluency, Double prosody, double completeness,
                         double pronunciation, List<SpokenWord> words) {
        this(text, accuracy, fluency, prosody, completeness, pronunciation, null, words);
    }
}
