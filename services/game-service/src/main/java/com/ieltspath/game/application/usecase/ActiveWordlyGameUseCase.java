package com.ieltspath.game.application.usecase;

import com.ieltspath.game.application.port.ActiveWordlyGameRepository;
import com.ieltspath.game.application.result.ActiveWordlyGameResult;
import com.ieltspath.game.domain.exception.SoloGameplayException;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Finds or leaves the room the caller still belongs to, whichever room game it is. */
@Service
public class ActiveWordlyGameUseCase {
    private final ActiveWordlyGameRepository games;
    private final ChangeGameRoomMembershipUseCase membership;
    private final SoloGameplayUseCase gameplay;
    private final Clock clock;
    private final RocketGameplayUseCase rocket;

    /** Without Rocket; a running Rocket match then cannot be left through here. */
    public ActiveWordlyGameUseCase(ActiveWordlyGameRepository games, ChangeGameRoomMembershipUseCase membership,
            SoloGameplayUseCase gameplay, Clock clock) {
        this(games, membership, gameplay, clock, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ActiveWordlyGameUseCase(ActiveWordlyGameRepository games, ChangeGameRoomMembershipUseCase membership,
            SoloGameplayUseCase gameplay, Clock clock, RocketGameplayUseCase rocket) {
        this.games = games;
        this.membership = membership;
        this.gameplay = gameplay;
        this.clock = clock;
        this.rocket = rocket;
    }

    @Transactional(readOnly = true)
    public Optional<ActiveWordlyGameResult> find(UUID userId) {
        return games.find(userId);
    }

    /** Leaves the current room: forfeits a running match, otherwise uses the waiting-room lifecycle. */
    public void leave(UUID userId) {
        var active = games.find(userId);
        if (active.isEmpty()) return;
        var game = active.get();
        if ("WAITING".equals(game.roomStatus())) membership.leave(game.roomId(), userId);
        else if (game.playing()) {
            // Forfeiting a running match is game-specific.
            if ("WORDLY".equals(game.gameType())) gameplay.leave(game.matchId(), userId);
            else if ("ROCKET".equals(game.gameType()) && rocket != null) rocket.leave(game.matchId(), userId);
            else throw new SoloGameplayException(409, "GAME_NOT_SUPPORTED", "This game cannot be left yet");
        }
        else games.leaveRunningMatchRoom(game.roomId(), userId, clock.instant());
    }
}
