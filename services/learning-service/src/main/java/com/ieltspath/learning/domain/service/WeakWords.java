package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.SpeechAnalysis;
import com.ieltspath.learning.domain.vo.SpokenWord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * The words the learner most needs to practise, taken straight from the speech service's word scores. Each word appears
 * once, at its worst score, so a word said badly five times is one entry and not five.
 */
public final class WeakWords {
    /** Words scoring below this are weak, unless the service named an error for them (those always count). */
    public static final double WEAK_BELOW = 60.0;
    /** Short function words are heard badly in fast speech without that being a pronunciation problem to teach. */
    private static final Set<String> IGNORED = Set.of("a", "an", "the", "i", "to", "of", "and", "in", "is", "it");

    private WeakWords() {
    }

    public static List<SpokenWord> select(SpeechAnalysis analysis, int limit) {
        return select(analysis.words(), WEAK_BELOW, limit);
    }

    public static List<SpokenWord> select(List<SpokenWord> words, double weakBelow, int limit) {
        List<SpokenWord> candidates = words.stream()
                .filter(word -> !word.text().isBlank() && !IGNORED.contains(word.text().toLowerCase(Locale.ROOT)))
                .filter(word -> word.accuracy() < weakBelow || word.hasError())
                .sorted(Comparator.comparingDouble(SpokenWord::accuracy).thenComparingLong(SpokenWord::startMs))
                .toList();
        Set<String> seen = new TreeSet<>();
        List<SpokenWord> chosen = new ArrayList<>();
        for (SpokenWord word : candidates) {
            if (chosen.size() >= limit) break;
            if (seen.add(word.text().toLowerCase(Locale.ROOT))) chosen.add(word);
        }
        return chosen;
    }
}
