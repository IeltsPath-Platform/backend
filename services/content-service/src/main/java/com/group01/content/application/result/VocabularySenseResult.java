package com.group01.content.application.result;

import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.PartOfSpeech;

import java.time.Instant;
import java.util.UUID;

public record VocabularySenseResult(
        UUID id,
        UUID vocabularyItemId,
        PartOfSpeech partOfSpeech,
        String englishDefinition,
        String vietnameseMeaning,
        String exampleSentence,
        String imageUrl,
        int sortOrder,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt
) {}

