package com.ieltspath.library.domain.repository;

import com.ieltspath.library.domain.aggregate.VocabularyProgress;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VocabularyReviewRepository {
    void lockUser(UUID userId);
    boolean isAvailableSense(UUID senseId);
    Optional<VocabularyProgress> findProgress(UUID userId, UUID senseId);
    void save(VocabularyProgress progress);
    List<QueueItem> dueItems(UUID userId, Instant now, int limit);
    Optional<Session> findSessionByRequest(UUID userId, UUID requestId);
    Optional<Session> findSession(UUID userId, UUID id);
    boolean hasReviewableItems(UUID userId, UUID sessionId, Instant now);
    void saveSession(Session session);
    Optional<Receipt> findReceipt(UUID userId, UUID requestId);
    void saveReceipt(Receipt receipt);
    Summary summarize(UUID userId, Instant now);
    record QueueItem(UUID vocabularySenseId, long stateVersion, String lemma, String meaning,
                     String exampleSentence, boolean answered) {}
    record Session(UUID id, UUID userId, UUID requestId, int limit, Instant createdAt, Instant expiresAt,
                   List<QueueItem> items) {}
    record Receipt(UUID userId, UUID requestId, UUID sessionId, UUID vocabularySenseId, long expectedVersion,
                   VocabularyProgress.Outcome outcome, VocabularyProgress.State state, long stateVersion,
                   Instant nextReviewAt, Instant reviewedAt) {}
    record StateCount(VocabularyProgress.State state, long count, long dueCount, Instant dataFrom) {}
    record Summary(List<StateCount> buckets) {}
}
