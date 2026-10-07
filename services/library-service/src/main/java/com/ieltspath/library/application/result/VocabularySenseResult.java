package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.vo.ContentStatus;
import com.ieltspath.library.domain.vo.PartOfSpeech;

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
