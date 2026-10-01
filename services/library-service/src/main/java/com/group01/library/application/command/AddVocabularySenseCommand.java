package com.group01.library.application.command;

import com.group01.library.domain.vo.PartOfSpeech;

import java.util.UUID;

public record AddVocabularySenseCommand(
        UUID vocabularyItemId,
        PartOfSpeech partOfSpeech,
        String englishDefinition,
        String vietnameseMeaning,
        String exampleSentence,
        String imageUrl,
        int sortOrder
) {}
