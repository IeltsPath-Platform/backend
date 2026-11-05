package com.ieltspath.learning.domain.vo;

import java.util.List;

/**
 * One word the speech service heard: when it started and how long it lasted (milliseconds from the start of the
 * recording), how well it was pronounced (0 to 100), the error the service named for it ({@code None} when none) and its
 * sounds with their own scores. A result stored before sounds were kept has none, which reads as an empty list.
 */
public record SpokenWord(String text, long startMs, long durationMs, double accuracy, String errorType,
                         List<SpokenPhoneme> phonemes) {

    public SpokenWord {
        phonemes = phonemes == null ? List.of() : List.copyOf(phonemes);
    }

    public SpokenWord(String text, long startMs, long durationMs, double accuracy, String errorType) {
        this(text, startMs, durationMs, accuracy, errorType, List.of());
    }

    public long endMs() {
        return startMs + durationMs;
    }

    public boolean hasError() {
        return errorType != null && !errorType.isBlank() && !"None".equalsIgnoreCase(errorType);
    }

    /** The sounds of this word that scored low, worst first. */
    public List<SpokenPhoneme> weakPhonemes() {
        return phonemes.stream().filter(SpokenPhoneme::weak)
                .sorted(java.util.Comparator.comparingDouble(SpokenPhoneme::accuracy)).toList();
    }
}
