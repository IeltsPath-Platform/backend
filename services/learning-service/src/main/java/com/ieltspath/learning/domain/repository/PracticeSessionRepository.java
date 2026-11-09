package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.PracticeSession;
import java.util.Optional;
import java.util.UUID;

public interface PracticeSessionRepository {
    Optional<PracticeSession> findByRequest(UUID userId, UUID requestId);
    Optional<PracticeSession> findOwned(UUID id, UUID userId);
    void save(PracticeSession session);
}
