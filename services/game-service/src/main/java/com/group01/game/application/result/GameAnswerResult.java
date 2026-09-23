package com.group01.game.application.result;

import java.util.UUID;

public record GameAnswerResult(UUID answerId, int itemSequence, boolean correct, int score,
                               String sessionStatus, boolean duplicate) {
}
