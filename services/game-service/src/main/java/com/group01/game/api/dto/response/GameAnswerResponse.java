package com.group01.game.api.dto.response;

import com.group01.game.application.result.GameAnswerResult;

import java.util.UUID;

public record GameAnswerResponse(UUID answerId, int itemSequence, boolean correct, int score,
                                 String sessionStatus, boolean duplicate) {
    public static GameAnswerResponse from(GameAnswerResult result) {
        return new GameAnswerResponse(result.answerId(), result.itemSequence(), result.correct(), result.score(),
                result.sessionStatus(), result.duplicate());
    }
}
