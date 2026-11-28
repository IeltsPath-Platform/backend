package com.ieltspath.assessment.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AttemptStructureResult(List<Section> sections) {
    public record Section(UUID id, UUID contentSectionId, int sortOrder, Integer partNo, String snapshot, List<Item> items,
                          Instant startedAt, Instant completedAt) {
        public Section(UUID id, UUID contentSectionId, int sortOrder, String snapshot, List<Item> items) {
            this(id, contentSectionId, sortOrder, null, snapshot, items, null, null);
        }
    }

    /** {@code answerSeconds} is how long a spoken answer may last; null for any other kind of question. */
    public record Item(UUID id, UUID questionVersionId, int sortOrder,
                       String questionSnapshot, String knowledgeSnapshot, Integer answerSeconds) {
        public Item(UUID id, UUID questionVersionId, int sortOrder, String questionSnapshot, String knowledgeSnapshot) {
            this(id, questionVersionId, sortOrder, questionSnapshot, knowledgeSnapshot, null);
        }
    }
}
