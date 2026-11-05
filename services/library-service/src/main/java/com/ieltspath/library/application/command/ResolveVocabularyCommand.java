package com.ieltspath.library.application.command;

import com.ieltspath.library.domain.vo.PartOfSpeech;

import java.util.List;

/** Words an author has reviewed: each is looked up by lemma and only written when the library does not have it yet. */
public record ResolveVocabularyCommand(List<Entry> entries) {

    public record Entry(String lemma, String ipa, PartOfSpeech partOfSpeech, String englishDefinition,
                        String vietnameseMeaning, String exampleSentence) {
    }
}
