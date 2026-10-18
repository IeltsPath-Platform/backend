package com.ieltspath.game.application.port;

import com.ieltspath.game.application.result.WordlyRankingResult;
import java.util.Optional;
import java.util.UUID;

public interface WordlyRankingRepository {
    Optional<WordlyRankingResult> find(UUID matchId, UUID userId);
}
