package com.group01.game.domain.repository;
import com.group01.game.domain.aggregate.GameMatch;
import java.util.Optional;
import java.util.UUID;
public interface GameMatchRepository {
    GameMatch save(GameMatch match);
    Optional<GameMatch> findById(UUID id);
    Optional<GameMatch> findForUpdate(UUID id);
}
