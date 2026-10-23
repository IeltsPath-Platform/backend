package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.AiDraftNotFoundException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Read and reject for an author's own drafts. Another author's draft is reported as not found, so the response never
 * reveals that it exists.
 */
@Service
public class AiDraftReviewUseCases {

    private final AiDraftRepository drafts;
    private final Clock clock;

    public AiDraftReviewUseCases(AiDraftRepository drafts, Clock clock) {
        this.drafts = drafts;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AiDraft> list(UUID requestedBy, DraftKind kind, DraftStatus status, int page, int size) {
        return drafts.findByRequester(requestedBy, kind, status, page, size);
    }

    @Transactional(readOnly = true)
    public AiDraft get(UUID requestedBy, UUID id) {
        return owned(requestedBy, id);
    }

    @Transactional
    public AiDraft reject(UUID requestedBy, UUID id, String note) {
        AiDraft draft = owned(requestedBy, id);
        draft.reject(requestedBy, note, clock.instant());
        return drafts.save(draft);
    }

    private AiDraft owned(UUID requestedBy, UUID id) {
        return drafts.findById(id)
                .filter(draft -> draft.getRequestedBy().equals(requestedBy))
                .orElseThrow(() -> new AiDraftNotFoundException(id));
    }
}
