package com.ieltspath.library.application.query;

import com.ieltspath.library.domain.vo.CefrLevel;

import java.util.UUID;

/**
 * Public dictionary browsing. {@code vietnamese} searches the Vietnamese meaning instead of the English lemma; an
 * empty text matches every word.
 */
public record CatalogBrowseQuery(String text, boolean vietnamese, CefrLevel cefrLevel, UUID topicId, PageQuery page) {
    public CatalogBrowseQuery {
        text = text == null ? "" : text.trim();
    }
}
