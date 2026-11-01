package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.WordLookupResult;

import java.util.List;

public record WordLookupResponse(String lemma, String normalizedLemma, String ipa, String audioUk, String audioUs,
                                 List<Sense> senses) {

    public record Sense(String partOfSpeech, String englishDefinition, String vietnameseMeaning, String exampleSentence) {}

    public static WordLookupResponse from(WordLookupResult result) {
        return new WordLookupResponse(result.lemma(), result.normalizedLemma(), result.ipa(), result.audioUk(),
                result.audioUs(), result.senses().stream()
                .map(sense -> new Sense(sense.partOfSpeech(), sense.englishDefinition(), sense.vietnameseMeaning(),
                        sense.exampleSentence()))
                .toList());
    }
}
