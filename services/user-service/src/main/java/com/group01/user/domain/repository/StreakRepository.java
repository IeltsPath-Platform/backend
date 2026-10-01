package com.group01.user.domain.repository;

import com.group01.user.domain.aggregate.Streak;

import java.util.Optional;
import java.util.UUID;

public interface StreakRepository {
    Optional<Streak> findByUserId(UUID userId);

    Streak save(Streak streak);
}
