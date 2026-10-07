package com.ieltspath.user.domain.repository;

import com.ieltspath.user.domain.aggregate.Streak;

import java.util.Optional;
import java.util.UUID;

public interface StreakRepository {
    Optional<Streak> findByUserId(UUID userId);

    Streak save(Streak streak);
}
