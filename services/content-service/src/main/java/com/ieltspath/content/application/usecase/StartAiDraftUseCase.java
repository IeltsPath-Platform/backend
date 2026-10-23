package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.AiUsageQuota;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.exception.AiQuotaExceededException;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.AiUsageKind;
import com.ieltspath.content.domain.vo.DraftKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/** Takes one use from the author's allowance and queues the request. The job does the generating later. */
@Service
@Transactional
public class StartAiDraftUseCase {

    private final AiDraftRepository drafts;
    private final AiUsageQuota quota;
    private final Clock clock;

    public StartAiDraftUseCase(AiDraftRepository drafts, AiUsageQuota quota, Clock clock) {
        this.drafts = drafts;
        this.quota = quota;
        this.clock = clock;
    }

    /**
     * {@code dailyLimit} comes from configuration. The quota and the queued row share one transaction, so a failure
     * after the use is counted gives the use back.
     */
    public AiDraft execute(UUID requestedBy, DraftKind kind, String requestJson, int dailyLimit) {
        LocalDate today = LocalDate.now(clock);
        if (!quota.tryConsume(requestedBy, today, AiUsageKind.AI, dailyLimit)) {
            throw new AiQuotaExceededException();
        }
        return drafts.save(AiDraft.request(kind, requestedBy, requestJson, clock.instant()));
    }
}
