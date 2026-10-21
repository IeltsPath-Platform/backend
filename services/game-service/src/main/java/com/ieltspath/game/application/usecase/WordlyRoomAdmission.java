package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.ActiveWordlyGameRepository;
import com.ieltspath.game.domain.exception.SoloGameplayException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** A user belongs to at most one Wordly room; creating or joining another requires leaving it first. */
@Service
public class WordlyRoomAdmission {
    public static final String ACTIVE_GAME_EXISTS = "ACTIVE_GAME_EXISTS";

    private final ActiveWordlyGameRepository games;
    private final Clock clock;

    public WordlyRoomAdmission(ActiveWordlyGameRepository games, Clock clock) {
        this.games = games;
        this.clock = clock;
    }

    /** Runs inside the caller's create/join transaction and keeps the user lock until it commits. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireNoActiveGame(UUID userId) {
        games.lockUser(userId);
        games.closeIdleSoloRooms(userId, clock.instant());
        if (games.find(userId).isPresent())
            throw new SoloGameplayException(409, ACTIVE_GAME_EXISTS, "You are already in a Wordly room");
    }
}
