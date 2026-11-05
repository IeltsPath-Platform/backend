package com.ieltspath.library.domain.catalog;

import com.ieltspath.library.domain.vo.ContentStatus;

/** The editable fields of a vocabulary topic. A missing sort order is 0 and a missing status is ACTIVE. */
public record VocabularyTopicDraft(
        String slug,
        String name,
        String nameVi,
        String coverKey,
        Integer sortOrder,
        ContentStatus status
) {
    public VocabularyTopicDraft {
        slug = CatalogText.required(slug, 100, "slug");
        name = CatalogText.required(name, 120, "name");
        nameVi = CatalogText.required(nameVi, 120, "nameVi");
        coverKey = CatalogText.coverKey(coverKey);
        sortOrder = CatalogText.sortOrder(sortOrder);
        status = status == null ? ContentStatus.ACTIVE : status;
    }
}
