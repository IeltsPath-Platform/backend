package com.ieltspath.user.domain.repository;

import com.ieltspath.user.domain.aggregate.LoginAttempt;
import com.ieltspath.user.domain.vo.OwnedPage;

import java.time.LocalDateTime;
import java.util.UUID;

public interface LoginHistoryRepository {
    LoginAttempt save(LoginAttempt attempt);

    /** Attempts of one user, newest first. */
    OwnedPage<LoginAttempt> findByUserId(UUID userId, int page, int size);

    /** Removes at most {@code limit} attempts recorded before the cutoff. */
    int deleteOlderThan(LocalDateTime cutoff, int limit);

    /** Counts attempts recorded before the cutoff without returning identifying data. */
    long countOlderThan(LocalDateTime cutoff);
}
