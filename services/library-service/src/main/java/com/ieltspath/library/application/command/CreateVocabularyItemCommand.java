package com.ieltspath.library.application.command;

import com.ieltspath.library.domain.catalog.VocabularyItemDetails;

public record CreateVocabularyItemCommand(
        String lemma,
        String ipa,
        String pronunciationAudioReference,
        VocabularyItemDetails dictionary
) {}
