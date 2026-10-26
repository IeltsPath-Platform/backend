package com.ieltspath.library.application.result;

import com.ieltspath.library.domain.vo.ContentStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * A vocabulary topic. Public lists fill {@code wordCount}; the admin list fills status and timestamps; a topic
 * nested in a sense carries only its identity and cover.
 */
public record VocabularyTopicResult(
        UUID id,
        String slug,
        String name,
        String nameVi,
        String coverKey,
        String coverUrl,
        Integer sortOrder,
        Long wordCount,
        ContentStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
