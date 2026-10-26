package com.ieltspath.library.domain.vo;

import java.util.UUID;

public record VocabularySenseCard(
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
