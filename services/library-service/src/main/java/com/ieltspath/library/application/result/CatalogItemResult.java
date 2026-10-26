package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.vo.ContentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A dictionary word with its meanings. {@code ipaUk} and {@code audioUkReference} fall back to the older single
 * pronunciation when the UK value is not set, so existing words still show one pronunciation.
 */
public record CatalogItemResult(
        UUID id,
        String lemma,
        String normalizedLemma,
        String ipa,
        String pronunciationAudioReference,
        String ipaUk,
        String ipaUs,
        String audioUkReference,
        String audioUsReference,
        Integer frequencyRank,
        boolean academicWordList,
        String wordFamily,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<CatalogSenseResult> senses,
        CatalogCountsResult counts
) {
}
