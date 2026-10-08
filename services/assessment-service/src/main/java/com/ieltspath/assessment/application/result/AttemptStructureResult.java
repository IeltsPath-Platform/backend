package com.ieltspath.assessment.application.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AttemptStructureResult(List<Section> sections) {
    public record Section(UUID id, UUID contentSectionId, int sortOrder, String snapshot, List<Item> items,
                          Instant startedAt, Instant completedAt) {
        public Section(UUID id, UUID contentSectionId, int sortOrder, String snapshot, List<Item> items) {
            this(id, contentSectionId, sortOrder, snapshot, items, null, null);
        }
    }

    public record Item(UUID id, UUID questionVersionId, int sortOrder,
                       String questionSnapshot, String knowledgeSnapshot) {}
}
