package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.vo.PartOfSpeech;

import java.util.UUID;

public record VocabularySenseCardResult(
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
) {}
