package com.group01.content.api.dto.internal;

import java.util.List;
import java.util.UUID;
import com.group01.content.application.result.GameContentSnapshotResult;

public record GameContentSnapshotResponse(List<GameContentItem> items) {
    public static GameContentSnapshotResponse from(GameContentSnapshotResult result) {
        return new GameContentSnapshotResponse(result.items().stream().map(item -> new GameContentItem(
                item.canonicalId(), item.vocabularySenseId(), item.questionVersionId(), item.prompt(),
                item.options(), item.answerSpecJson(), item.explanation())).toList());
    }

    public record GameContentItem(
            UUID canonicalId,
            UUID vocabularySenseId,
            UUID questionVersionId,
            String prompt,
            List<?> options,
            String answerSpecJson,
            String explanation
    ) {}
}
