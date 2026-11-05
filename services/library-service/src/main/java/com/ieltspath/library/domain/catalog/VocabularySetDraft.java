package com.ieltspath.library.domain.catalog;

import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.vo.CefrLevel;
import com.ieltspath.library.domain.vo.ContentStatus;
import com.ieltspath.library.domain.vo.VocabularySetKind;

import java.util.UUID;

/**
 * The editable fields of a vocabulary set. A band set is defined by its CEFR level and a topic set by its topic,
 * never both. A missing sort order is 0 and a missing status is ACTIVE.
 */
public record VocabularySetDraft(
        String title,
        String description,
        VocabularySetKind kind,
        CefrLevel cefrLevel,
        UUID topicId,
        String coverKey,
        Integer sortOrder,
        ContentStatus status
) {
    public VocabularySetDraft {
        title = CatalogText.required(title, 180, "title");
        description = CatalogText.optional(description, 2000, "description");
        if (kind == null) {
            throw new InvalidDataException("kind is required");
        }
        if (kind == VocabularySetKind.BAND && (cefrLevel == null || topicId != null)) {
            throw new InvalidDataException("a BAND set needs a cefrLevel and no topicId");
        }
        if (kind == VocabularySetKind.TOPIC && (topicId == null || cefrLevel != null)) {
            throw new InvalidDataException("a TOPIC set needs a topicId and no cefrLevel");
        }
        coverKey = CatalogText.coverKey(coverKey);
        sortOrder = CatalogText.sortOrder(sortOrder);
        status = status == null ? ContentStatus.ACTIVE : status;
    }
}
