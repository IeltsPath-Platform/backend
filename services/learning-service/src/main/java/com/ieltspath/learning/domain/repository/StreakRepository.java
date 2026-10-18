package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.vo.StreakState;

import java.util.Optional;
import java.util.UUID;

/** The streak state of each learner. */
public interface StreakRepository {
    Optional<StreakState> find(UUID userId);

    void save(UUID userId, StreakState state);
}
