package com.group01.game.application.result;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public content projection. Answer keys and explanations intentionally have no representation here.
 */
public record GameItemResult(Object canonicalId, Object vocabularySenseId, Object questionVersionId,
                             Object prompt, Object options) {
    private static final List<String> PUBLIC_FIELDS =
            List.of("canonicalId", "vocabularySenseId", "questionVersionId", "prompt", "options");

    public static GameItemResult from(Map<String, Object> snapshotItem) {
        Map<String, Object> item = new LinkedHashMap<>();
        for (String field : PUBLIC_FIELDS) item.put(field, snapshotItem.get(field));
        return new GameItemResult(item.get("canonicalId"), item.get("vocabularySenseId"),
                item.get("questionVersionId"), item.get("prompt"), item.get("options"));
    }
}
