package com.group01.content.application.command;

import com.group01.content.domain.vo.PartOfSpeech;

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

