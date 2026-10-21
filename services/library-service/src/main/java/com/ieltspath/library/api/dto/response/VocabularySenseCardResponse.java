package com.ieltspath.library.api.dto.response;

import com.ieltspath.library.application.result.VocabularySenseCardResult;
import com.ieltspath.library.domain.vo.PartOfSpeech;

import java.util.UUID;

public record VocabularySenseCardResponse(
        UUID senseId,
        UUID vocabularyItemId,
        String lemma,
        String ipa,
        String pronunciationAudioReference,
        PartOfSpeech partOfSpeech,
        String englishDefinition,
        String vietnameseMeaning,
        String exampleSentence,
        String imageUrl
) {
    public static VocabularySenseCardResponse from(VocabularySenseCardResult result) {
        return new VocabularySenseCardResponse(
                result.senseId(),
                result.vocabularyItemId(),
                result.lemma(),
                result.ipa(),
                result.pronunciationAudioReference(),
                result.partOfSpeech(),
                result.englishDefinition(),
                result.vietnameseMeaning(),
                result.exampleSentence(),
                result.imageUrl()
        );
    }
}
