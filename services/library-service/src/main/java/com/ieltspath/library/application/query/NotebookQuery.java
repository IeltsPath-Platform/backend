package com.ieltspath.library.application.query;

import com.ieltspath.library.domain.vo.UnderstandingLevel;

import java.util.List;
import java.util.UUID;

public record NotebookQuery(String q, List<UnderstandingLevel> levels, Boolean due, String sort, int page, int size,
                            UUID topicId, UUID setId) {
    public NotebookQuery(String q, List<UnderstandingLevel> levels, Boolean due, String sort, int page, int size) {
        this(q, levels, due, sort, page, size, null, null);
    }
}
