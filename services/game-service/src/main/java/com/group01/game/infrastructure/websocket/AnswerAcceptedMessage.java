package com.group01.game.infrastructure.websocket;

import com.group01.game.application.result.GameAnswerResult;

import java.util.UUID;

public record AnswerAcceptedMessage(String type, String userId, Answer answer) {
    public static AnswerAcceptedMessage of(String userId, GameAnswerResult result) {
        return new AnswerAcceptedMessage("ANSWER_ACCEPTED", userId, new Answer(result.answerId(),
                result.itemSequence(), result.correct(), result.score(), result.sessionStatus(), result.duplicate()));
    }

    public record Answer(UUID answerId, int itemSequence, boolean correct, int score,
                         String sessionStatus, boolean duplicate) {
    }
}
