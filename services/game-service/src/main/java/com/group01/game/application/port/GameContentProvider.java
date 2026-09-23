package com.group01.game.application.port;

import java.util.List;
import java.util.UUID;

public interface GameContentProvider {
    List<GameContentItem> loadSnapshot(String gameType, String learningDomain, List<UUID> contentIds);

    record GameContentItem(UUID canonicalId, UUID vocabularySenseId, UUID questionVersionId,
                          String prompt, List<?> options, String answerSpecJson, String explanation) {
        public GameContentItem { options = options == null ? List.of() : List.copyOf(options); }
    }
}
