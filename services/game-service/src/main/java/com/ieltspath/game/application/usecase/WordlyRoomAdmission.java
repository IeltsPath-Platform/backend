package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.ActiveWordlyGameRepository;
import com.ieltspath.game.domain.exception.SoloGameplayException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A user belongs to at most one room across every {@link com.ieltspath.game.domain.vo.RoomGame}; creating or joining
 * another room, of the same game or not, requires leaving it first.
 */
@Service
public class WordlyRoomAdmission {
    public static final String ACTIVE_GAME_EXISTS = "ACTIVE_GAME_EXISTS";

    private final ActiveWordlyGameRepository games;
    private final Clock clock;
    private final LobbySeatUseCase lobbySeats;

    public WordlyRoomAdmission(ActiveWordlyGameRepository games, Clock clock) {
        this(games, clock, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public WordlyRoomAdmission(ActiveWordlyGameRepository games, Clock clock, LobbySeatUseCase lobbySeats) {
        this.games = games;
        this.clock = clock;
        this.lobbySeats = lobbySeats;
    }

    /** Runs inside the caller's create/join transaction and keeps the user lock until it commits. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireNoActiveGame(UUID userId) {
        games.lockUser(userId);
        games.closeIdleSoloRooms(userId, clock.instant());
        if (lobbySeats != null) lobbySeats.expireFor(userId);
        if (games.find(userId).isPresent())
            throw new SoloGameplayException(409, ACTIVE_GAME_EXISTS, "You are already in a game room");
    }
}
