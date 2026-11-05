package com.ieltspath.user.domain.repository;

import com.ieltspath.user.domain.aggregate.PasswordChangeAttempts;

import java.util.Optional;
import java.util.UUID;

public interface PasswordChangeAttemptRepository {
    Optional<PasswordChangeAttempts> findByUserId(UUID userId);

    void save(PasswordChangeAttempts attempts);

    void deleteByUserId(UUID userId);
}
