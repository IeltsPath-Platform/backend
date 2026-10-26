package com.ieltspath.content.application.result;

import java.util.Map;
import java.util.UUID;

/** Names for what staff work items are about. Unknown ids are absent. */
public record WorkItemSubjectsResult(Map<UUID, KnowledgePointLabel> knowledgePoints,
                                     Map<UUID, QuestionLabel> questions) {

    public record KnowledgePointLabel(String code, String name) {
    }

    /** {@code stemPreview} is the start of the stem, at most 120 characters. */
    public record QuestionLabel(UUID questionId, String stemPreview) {
    }
}
