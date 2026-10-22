package com.ieltspath.game.domain.repository;

import com.ieltspath.game.domain.aggregate.RocketRound;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RocketRoundRepository {
    /** Inserts the rounds of a new match; they all start PENDING. */
    void createAll(List<RocketRound> rounds);

    /** Stores the round's lifecycle and any answers it did not have yet; answers are never rewritten. */
    void save(RocketRound round);

    List<RocketRound> findByMatch(UUID matchId);

    /** Locks the round row until the transaction ends; callers lock the match first. */
    Optional<RocketRound> findForUpdate(UUID roundId);

    /** OPEN rounds whose deadline has passed, oldest first, for the deadline worker. Rows locked by others are skipped. */
    List<UUID> lockExpiredOpen(Instant now, int limit);

    /** PENDING rounds whose scheduled opening has come, for the worker that opens the next question. */
    List<UUID> lockDueToOpen(Instant now, int limit);
}
