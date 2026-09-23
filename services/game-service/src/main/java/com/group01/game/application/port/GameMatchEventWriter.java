package com.group01.game.application.port;

import com.group01.game.domain.aggregate.GameAnswer;
import com.group01.game.domain.aggregate.GameSession;

public interface GameMatchEventWriter {
    boolean recordAnswer(GameAnswer answer, GameSession session);
}
