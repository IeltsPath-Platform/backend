package com.ieltspath.game.application.port;

public interface GameAnswerEvaluator {
    boolean isCorrect(Object answerSpec, Object submittedAnswer);
}
