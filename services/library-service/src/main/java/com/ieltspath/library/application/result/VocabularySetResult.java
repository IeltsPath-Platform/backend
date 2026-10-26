package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.vo.CefrLevel;
import com.ieltspath.library.domain.vo.ContentStatus;
import com.ieltspath.library.domain.vo.IeltsBandRange;
import com.ieltspath.library.domain.vo.VocabularySetKind;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A vocabulary set. The public list fills the counts, the detail view also fills {@code topics}, and the admin
 * list fills status and timestamps.
 */
public record VocabularySetResult(
        UUID id,
        String title,
        String description,
        VocabularySetKind kind,
        CefrLevel cefrLevel,
        UUID topicId,
        String topicName,
        String topicNameVi,
        String coverKey,
        String coverUrl,
        int sortOrder,
        ContentStatus status,
        IeltsBandRange bandRange,
        Long topicCount,
        Long wordCount,
        List<VocabularyTopicResult> topics,
        Instant createdAt,
        Instant updatedAt
) {
}
