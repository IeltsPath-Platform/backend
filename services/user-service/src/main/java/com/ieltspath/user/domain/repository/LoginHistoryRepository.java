package com.ieltspath.user.domain.repository;

import com.ieltspath.user.domain.aggregate.LoginAttempt;
import com.ieltspath.user.domain.vo.OwnedPage;

import java.util.UUID;

public interface LoginHistoryRepository {
    LoginAttempt save(LoginAttempt attempt);

    /** Attempts of one user, newest first. */
    OwnedPage<LoginAttempt> findByUserId(UUID userId, int page, int size);
}
