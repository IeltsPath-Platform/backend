package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.AiUsageQuota;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.AiDraftClaim;
import com.ieltspath.content.domain.vo.AiUsageKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AiDraftJobStateUseCases {
    private final AiDraftRepository drafts;
    private final AiUsageQuota quota;
    private final Clock clock;

    public AiDraftJobStateUseCases(AiDraftRepository drafts, AiUsageQuota quota, Clock clock) {
        this.drafts = drafts;
        this.quota = quota;
        this.clock = clock;
    }

    @Transactional
    public boolean complete(AiDraftClaim claim, String result, String model, String promptVersion, Instant now) {
        return drafts.completeIfClaimed(claim.draftId(), claim.claimToken(), result, model, promptVersion, now) == 1;
    }

    @Transactional
    public boolean fail(AiDraftClaim claim, AiDraft draft, String code, Instant now) {
        if (drafts.failIfClaimed(claim.draftId(), claim.claimToken(), code, now) != 1) return false;
        refund(draft.getRequestedBy(), draft.getCreatedAt());
        return true;
    }

    @Transactional
    public int recoverStale(Instant startedBefore, int maxAttempts, Instant now) {
        List<AiDraft> exhausted = drafts.requeueStaleGenerating(startedBefore, maxAttempts, now);
        exhausted.forEach(draft -> refund(draft.getRequestedBy(), draft.getCreatedAt()));
        return exhausted.size();
    }

    private void refund(UUID author, Instant createdAt) {
        LocalDate day = createdAt.atZone(clock.getZone()).toLocalDate();
        quota.refund(author, day, AiUsageKind.AI);
    }
}
