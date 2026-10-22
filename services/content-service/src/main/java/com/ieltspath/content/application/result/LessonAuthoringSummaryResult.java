package com.ieltspath.content.application.result;

import com.ieltspath.content.domain.vo.LessonStatus;
import com.ieltspath.content.domain.vo.LessonAuthoringSnapshot;

import java.util.List;
import java.util.UUID;

public record LessonAuthoringSummaryResult(UUID id, String code, String title, String summary,
                                           String summaryMarkdown, LessonStatus status, int sortOrder,
                                           Integer estimatedMinutes, int blockCount,
                                           List<UUID> knowledgePointIds) {
    public LessonAuthoringSummaryResult {
        knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds);
    }

    public static LessonAuthoringSummaryResult from(LessonAuthoringSnapshot snapshot) {
        return new LessonAuthoringSummaryResult(snapshot.id(), snapshot.code(), snapshot.title(), snapshot.summary(),
                snapshot.summaryMarkdown(), snapshot.status(), snapshot.sortOrder(), snapshot.estimatedMinutes(),
                snapshot.blocks().size(), snapshot.knowledgePointIds());
    }
}
