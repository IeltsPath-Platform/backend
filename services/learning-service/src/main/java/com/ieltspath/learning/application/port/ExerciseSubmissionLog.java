package com.ieltspath.learning.application.port;

import com.ieltspath.learning.application.command.SubmitExerciseCommand;
import com.ieltspath.learning.application.result.SubmissionResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Lesson exercise submissions as answered: replays a {@code requestId}, tells whether a block was answered before, and
 * which questions a learner ever got wrong (hints) or got wrong on the first try (reviews).
 */
public interface ExerciseSubmissionLog {
    Optional<Stored> find(UUID requestId);

    boolean exists(UUID userId, UUID lessonId, UUID blockId);

    /** False when the {@code requestId} is already stored. */
    boolean save(UUID userId, UUID lessonId, UUID blockId, SubmitExerciseCommand command, SubmissionResult response);

    /** The first response of each block of the lesson. */
    List<SubmissionResult> firstResponses(UUID userId, UUID lessonId);

    /** Questions answered wrong in any submission, per block. */
    Map<UUID, Set<UUID>> wrongQuestions(UUID userId, UUID lessonId);

    record Stored(UUID userId, UUID lessonId, UUID blockId, SubmissionResult response) {}
}
