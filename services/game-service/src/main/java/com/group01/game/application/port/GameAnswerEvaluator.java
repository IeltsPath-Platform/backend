package com.group01.game.application.port;

public interface GameAnswerEvaluator {
    boolean isCorrect(Object answerSpec, Object submittedAnswer);
}
