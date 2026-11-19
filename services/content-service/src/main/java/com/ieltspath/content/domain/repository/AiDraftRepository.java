package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiDraftRepository {
    AiDraft save(AiDraft draft);

    Optional<AiDraft> findById(UUID id);

    /**
     * Deletes the draft only while it is in one of {@code statuses}, in one statement, so a draft approved or claimed a
     * moment earlier is left alone. Returns how many rows went: 0 when the draft is gone or its status changed.
     */
    int deleteIfStatusIn(UUID id, Collection<DraftStatus> statuses);

    /** One author's drafts, newest first. Filters are optional. */
    List<AiDraft> findByRequester(UUID requestedBy, DraftKind kind, DraftStatus status, int page, int size);

    /**
     * Moves up to {@code limit} queued drafts to GENERATING in one step and returns their ids. Two workers never
     * receive the same draft.
     */
    List<UUID> claimQueued(int limit);

    /** Puts back drafts that have been generating since before {@code startedBefore}. Returns how many. */
    int requeueStaleGenerating(Instant startedBefore);
}
