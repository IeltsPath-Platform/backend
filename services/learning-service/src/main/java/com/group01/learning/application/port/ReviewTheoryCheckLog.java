package com.group01.learning.application.port;

import com.group01.learning.application.command.SubmitExerciseCommand;
import com.group01.learning.application.result.TheoryCheckResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Answered quick checks of reviews at the theory stage, replayed for the same {@code requestId}. */
public interface ReviewTheoryCheckLog {
    Optional<Stored> find(UUID requestId);

    void save(UUID userId, UUID reviewId, List<UUID> questionVersionIds, SubmitExerciseCommand command,
              TheoryCheckResult response);

    record Stored(UUID userId, UUID reviewId, TheoryCheckResult response) {}
}
