package com.group01.game.api.dto.response;

import com.group01.game.application.result.GameItemResult;

public record GameItemResponse(Object canonicalId, Object vocabularySenseId, Object questionVersionId,
                               Object prompt, Object options) {
    public static GameItemResponse from(GameItemResult result) {
        return new GameItemResponse(result.canonicalId(), result.vocabularySenseId(), result.questionVersionId(),
                result.prompt(), result.options());
    }
}
