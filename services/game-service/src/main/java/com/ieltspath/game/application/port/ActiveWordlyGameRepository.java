package com.ieltspath.game.application.port;

import com.ieltspath.game.application.result.ActiveWordlyGameResult;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ActiveWordlyGameRepository {
    /** Serializes room admission for one user until the surrounding transaction ends. */
    void lockUser(UUID userId);

    /** Closes the user's unstarted or already finished SOLO rooms; nobody else can be inside them. */
    void closeIdleSoloRooms(UUID userId, Instant now);

    Optional<ActiveWordlyGameResult> find(UUID userId);

    /** Removes a member who no longer plays the running match (finished or eliminated) from its room. */
    void leaveRunningMatchRoom(UUID roomId, UUID userId, Instant now);
}
