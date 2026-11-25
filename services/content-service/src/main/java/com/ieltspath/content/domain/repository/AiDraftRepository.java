package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.domain.vo.AiDraftClaim;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiDraftRepository {
    AiDraft save(AiDraft draft);

    Optional<AiDraft> findById(UUID id);

    Optional<AiDraft> findOpenSectionDraft(UUID authorId, UUID sectionId);

    Optional<AiDraft> findOpenLessonQuestionDraft(UUID authorId, UUID lessonId);

    Optional<AiDraft> findByIdForUpdate(UUID id);

    /**
     * Deletes the draft only while it is in one of {@code statuses}, in one statement, so a draft approved or claimed a
     * moment earlier is left alone. Returns how many rows went: 0 when the draft is gone or its status changed.
     */
    int deleteIfStatusIn(UUID id, Collection<DraftStatus> statuses);

    /** One author's drafts, newest first. Filters are optional. */
    List<AiDraft> findByRequester(UUID requestedBy, DraftKind kind, DraftStatus status, int page, int size);

    default List<AiDraft> findByRequester(UUID requestedBy, List<DraftKind> kinds, DraftStatus status, int page, int size) {
        if (kinds == null || kinds.isEmpty()) return findByRequester(requestedBy, (DraftKind) null, status, page, size);
        if (kinds.size() == 1) return findByRequester(requestedBy, kinds.getFirst(), status, page, size);
        throw new UnsupportedOperationException("Filtering by multiple draft kinds is not supported by this repository");
    }

    /**
     * Moves up to {@code limit} queued drafts to GENERATING in one step and returns their ids. Two workers never
     * receive the same draft.
     */
    List<AiDraftClaim> claimQueued(int limit);

    /** Completes only the active claim. Returns the number of changed rows. */
    int completeIfClaimed(UUID id, UUID claimToken, String result, String model, String promptVersion, Instant now);

    /** Fails only the active claim. Returns the number of changed rows. */
    int failIfClaimed(UUID id, UUID claimToken, String code, Instant now);

    /** Requeues stale work and fails exhausted claims atomically; returns only terminal failures for quota refunds. */
    List<AiDraft> requeueStaleGenerating(Instant startedBefore, int maxAttempts, Instant now);

    int expireReadySectionDrafts(Instant finishedBefore);
}
