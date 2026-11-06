package com.ieltspath.library.domain.study;

import java.util.UUID;

public record StudyWordSnapshot(UUID vocabularyItemId, UUID vocabularySenseId, int sortOrder, String lemma,
        String partOfSpeech, String vietnameseMeaning, String englishDefinition, String ipaUk, String ipaUs,
        String audioUkUrl, String audioUsUrl, String imageUrl, String exampleSentence,
        String exampleTranslationVi, boolean active) {}
