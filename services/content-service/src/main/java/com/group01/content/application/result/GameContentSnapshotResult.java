package com.group01.content.application.result;

import java.util.List;
import java.util.UUID;

public record GameContentSnapshotResult(List<GameContentItem> items) {
    public GameContentSnapshotResult { items = List.copyOf(items); }
    public record GameContentItem(UUID canonicalId, UUID vocabularySenseId, UUID questionVersionId,
                                  String prompt, List<?> options, String answerSpecJson, String explanation) {
        public GameContentItem { options = options == null ? List.of() : List.copyOf(options); }
    }
}
