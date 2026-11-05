package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.domain.vo.SpokenPhoneme;
import com.ieltspath.learning.domain.vo.SpokenWord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * The sounds a learner most needs to practise, taken from the scores the speech service gave every sound of every word.
 * A sound that is weak in several words is one entry with its count, so a habit (saying /θ/ as /s/ again and again) shows
 * up as one thing to fix and not as five separate words.
 */
public final class WeakSounds {
    private static final int EXAMPLES = 4;

    private WeakSounds() {
    }

    /**
     * @param heardAs the sound the service most often heard instead; null when it never named one
     * @param words   words where this sound was weak, worst first, at most a few
     */
    public record WeakSound(String symbol, int count, double meanAccuracy, String heardAs, List<String> words) { }

    private record Hit(String word, SpokenPhoneme phoneme) { }

    /** Most frequent first, then the lowest mean score. Words said without sound scores add nothing. */
    public static List<WeakSound> select(List<SpokenWord> words, int limit) {
        Map<String, List<Hit>> bySymbol = new LinkedHashMap<>();
        for (SpokenWord word : words) {
            for (SpokenPhoneme phoneme : word.phonemes()) {
                if (phoneme.weak() && !word.text().isBlank()) {
                    bySymbol.computeIfAbsent(phoneme.symbol(), key -> new ArrayList<>())
                            .add(new Hit(word.text().toLowerCase(Locale.ROOT), phoneme));
                }
            }
        }
        List<WeakSound> sounds = new ArrayList<>();
        bySymbol.forEach((symbol, hits) -> sounds.add(new WeakSound(symbol, hits.size(),
                hits.stream().mapToDouble(hit -> hit.phoneme().accuracy()).average().orElse(0), mostHeard(hits), examples(hits))));
        return sounds.stream().sorted(Comparator.comparingInt(WeakSound::count).reversed()
                .thenComparingDouble(WeakSound::meanAccuracy).thenComparing(WeakSound::symbol)).limit(limit).toList();
    }

    private static String mostHeard(List<Hit> hits) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        hits.stream().map(hit -> hit.phoneme().heardAs()).filter(heard -> heard != null && !heard.isBlank())
                .forEach(heard -> counts.merge(heard, 1, Integer::sum));
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    private static List<String> examples(List<Hit> hits) {
        TreeSet<String> seen = new TreeSet<>();
        List<String> examples = new ArrayList<>();
        hits.stream().sorted(Comparator.comparingDouble(hit -> hit.phoneme().accuracy())).forEach(hit -> {
            if (examples.size() < EXAMPLES && seen.add(hit.word())) examples.add(hit.word());
        });
        return examples;
    }
}
