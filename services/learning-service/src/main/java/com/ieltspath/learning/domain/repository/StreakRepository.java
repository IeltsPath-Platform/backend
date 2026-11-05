package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.vo.StreakState;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** The streak state of each learner. */
public interface StreakRepository {
    Optional<StreakState> find(UUID userId);

    /** The stored state of each learner that has one, read in one query; learners without a streak are absent. */
    Map<UUID, StreakState> findAll(Collection<UUID> userIds);

    void save(UUID userId, StreakState state);
}
