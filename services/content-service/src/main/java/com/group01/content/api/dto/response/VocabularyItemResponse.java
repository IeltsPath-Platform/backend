package com.group01.content.api.dto.response;

import com.group01.content.application.result.VocabularyItemResult;
import com.group01.content.domain.vo.ContentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VocabularyItemResponse(
        UUID id,
        String lemma,
        String normalizedLemma,
        String ipa,
        String pronunciationAudioReference,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<VocabularySenseResponse> senses
) {
    public static VocabularyItemResponse from(VocabularyItemResult result) {
        List<VocabularySenseResponse> senseResponses = result.senses() != null
                ? result.senses().stream().map(VocabularySenseResponse::from).toList()
                : List.of();
        return new VocabularyItemResponse(
                result.id(),
                result.lemma(),
                result.normalizedLemma(),
                result.ipa(),
                result.pronunciationAudioReference(),
                result.status(),
                result.createdAt(),
                result.updatedAt(),
                senseResponses
        );
    }
}

