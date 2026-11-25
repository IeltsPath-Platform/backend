package com.ieltspath.library.application.result;

import java.util.List;
import java.util.UUID;

/**
 * What a learner sees of a word while reading: how it is written, how it is said (IPA and a recording per accent, either
 * of which can be null), and the senses that are in use. A word that only has a pronunciation has no senses. A sense
 * carries its id so the reader can save exactly that meaning into the notebook.
 */
public record WordLookupResult(String lemma, String normalizedLemma, String ipa, String audioUk, String audioUs,
                               List<Sense> senses) {

    public record Sense(UUID id, String partOfSpeech, String englishDefinition, String vietnameseMeaning, String exampleSentence) {}
}
