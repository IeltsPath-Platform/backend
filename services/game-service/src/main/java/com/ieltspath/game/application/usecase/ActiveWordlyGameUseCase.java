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

    public ActiveWordlyGameUseCase(ActiveWordlyGameRepository games, ChangeGameRoomMembershipUseCase membership,
            SoloGameplayUseCase gameplay, Clock clock) {
        this.games = games;
        this.membership = membership;
        this.gameplay = gameplay;
        this.clock = clock;
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
            // Forfeiting a running match is game-specific; only Wordly matches can run so far.
            if (!"WORDLY".equals(game.gameType()))
                throw new SoloGameplayException(409, "GAME_NOT_SUPPORTED", "This game cannot be left yet");
            gameplay.leave(game.matchId(), userId);
        }
        else games.leaveRunningMatchRoom(game.roomId(), userId, clock.instant());
    }
}
