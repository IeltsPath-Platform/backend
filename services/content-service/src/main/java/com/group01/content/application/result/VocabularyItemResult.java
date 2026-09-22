package com.group01.content.application.result;

import com.group01.content.domain.vo.ContentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VocabularyItemResult(
        UUID id,
        String lemma,
        String normalizedLemma,
        String ipa,
        String pronunciationAudioReference,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<VocabularySenseResult> senses
) {}

