package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.service.HeartsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** The learner's hearts: their state, and a refill bought with points. */
@Service
public class HeartsUseCase {
    private final LearnerLock lock;
    private final HeartsService hearts;

    public HeartsUseCase(LearnerLock lock, HeartsService hearts) {
        this.lock = lock;
        this.hearts = hearts;
    }

    @Transactional
    public HeartsService.Snapshot view(UUID userId) {
        lock.lock(userId);
        return hearts.snapshot(userId);
    }

    @Transactional
    public HeartsService.Snapshot refill(UUID userId, UUID requestId) {
        lock.lock(userId);
        return hearts.refill(userId, requestId);
    }
}
