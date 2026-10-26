package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.AiDraftGenerator;
import com.ieltspath.content.application.port.AiUsageQuota;
import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.result.GenerationOutcome;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.AiUsageKind;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Claims a batch of queued drafts and runs each one. Generation happens outside any transaction, so a slow model call
 * never holds a database lock; each state change is saved on its own.
 */
@Service
public class RunAiDraftJobUseCase {

    private static final Logger log = LoggerFactory.getLogger(RunAiDraftJobUseCase.class);
    private static final String UNSUPPORTED_KIND = "UNSUPPORTED_KIND";
    private static final String INTERNAL = "INTERNAL";

    private final AiDraftRepository drafts;
    private final AiUsageQuota quota;
    private final Map<DraftKind, AiDraftGenerator> generators;
    private final Clock clock;

    public RunAiDraftJobUseCase(AiDraftRepository drafts, AiUsageQuota quota, List<AiDraftGenerator> generators,
                                Clock clock) {
        this.drafts = drafts;
        this.quota = quota;
        this.generators = generators.stream()
                .collect(Collectors.toMap(AiDraftGenerator::kind, Function.identity()));
        this.clock = clock;
    }

    /** Returns how many drafts this run claimed. */
    public int runBatch(int batchSize, Duration staleAfter) {
        int requeued = drafts.requeueStaleGenerating(clock.instant().minus(staleAfter));
        if (requeued > 0) {
            log.info("Requeued {} stale AI drafts", requeued);
        }
        List<UUID> claimed = drafts.claimQueued(batchSize);
        claimed.forEach(this::process);
        return claimed.size();
    }

    private void process(UUID id) {
        AiDraft draft = drafts.findById(id).orElse(null);
        if (draft == null || draft.getStatus() != DraftStatus.GENERATING) {
            return;
        }
        AiDraftGenerator generator = generators.get(draft.getKind());
        if (generator == null) {
            fail(draft, UNSUPPORTED_KIND);
            return;
        }
        try {
            GenerationOutcome outcome = generator.generate(draft);
            draft.complete(outcome.resultJson(), outcome.model(), outcome.promptVersion(), clock.instant());
            drafts.save(draft);
        } catch (ContentGenerationException failure) {
            fail(draft, failure.code());
        } catch (RuntimeException unexpected) {
            log.error("AI draft {} failed unexpectedly: {}", draft.getId(), unexpected.getClass().getSimpleName());
            fail(draft, INTERNAL);
        }
    }

    /** Every failure gives the author's use back: they received nothing usable for it. */
    private void fail(AiDraft draft, String errorCode) {
        draft.fail(errorCode, clock.instant());
        drafts.save(draft);
        LocalDate day = draft.getCreatedAt().atZone(clock.getZone()).toLocalDate();
        quota.refund(draft.getRequestedBy(), day, AiUsageKind.AI);
        log.warn("AI draft {} failed with {}", draft.getId(), errorCode);
    }
}
