package com.group01.content.api.dto.response;

import com.group01.content.application.result.VocabularySenseResult;
import com.group01.content.domain.vo.ContentStatus;
import com.group01.content.domain.vo.PartOfSpeech;

import java.time.Instant;
import java.util.UUID;

public record VocabularySenseResponse(
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
) {
    public static VocabularySenseResponse from(VocabularySenseResult result) {
        return new VocabularySenseResponse(
                result.id(),
                result.vocabularyItemId(),
                result.partOfSpeech(),
                result.englishDefinition(),
                result.vietnameseMeaning(),
                result.exampleSentence(),
                result.imageUrl(),
                result.sortOrder(),
                result.status(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}

