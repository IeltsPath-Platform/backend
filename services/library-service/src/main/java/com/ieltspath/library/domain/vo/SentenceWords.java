package com.ieltspath.library.domain.vo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Splits a sentence into words the way the dictation player does (matchDictation / sentenceWords in the frontend), so
 * that the n-th word here is the n-th word tile there: hyphens and dashes separate words, a word is lower-cased,
 * typographic apostrophes are made straight, everything but letters, digits and apostrophes is dropped, and words
 * left empty by that are skipped. Words are the keys of the pronunciations kept in vocabulary_items.normalized_lemma.
 */
public final class SentenceWords {
    private SentenceWords() {
    }

    /** "It's the perfect snack." gives [it's, the, perfect, snack]. */
    public static List<String> normalized(String sentence) {
        List<String> words = new ArrayList<>();
        if (sentence == null) {
            return words;
        }
        for (String token : sentence.replaceAll("[-–—]", " ").trim().split("\\s+")) {
            String word = token.toLowerCase(Locale.ROOT)
                    .replaceAll("[‘’`´]", "'")
                    .replaceAll("[^\\p{L}\\p{N}']", "")
                    .replaceAll("^'+|'+$", "");
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }
}
