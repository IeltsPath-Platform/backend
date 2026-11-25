package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.port.AiDraftGenerator;
import com.ieltspath.content.application.port.ContentGenerationException;
import com.ieltspath.content.application.result.GenerationOutcome;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import com.ieltspath.content.domain.vo.DraftKind;
import com.ieltspath.content.domain.vo.DraftStatus;
import com.ieltspath.content.domain.vo.AiDraftClaim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private final AiDraftJobStateUseCases state;
    private final Map<DraftKind, AiDraftGenerator> generators;
    private final Clock clock;

    public RunAiDraftJobUseCase(AiDraftRepository drafts, AiDraftJobStateUseCases state, List<AiDraftGenerator> generators,
                                Clock clock) {
        this.drafts = drafts;
        this.state = state;
        this.generators = generators.stream()
                .collect(Collectors.toMap(AiDraftGenerator::kind, Function.identity()));
        this.clock = clock;
    }

    /** Returns how many drafts this run claimed. */
    public int runBatch(int batchSize, Duration staleAfter) {
        return runBatch(batchSize, staleAfter, 3);
    }

    public int runBatch(int batchSize, Duration staleAfter, int maxAttempts) {
        int failed = state.recoverStale(clock.instant().minus(staleAfter), maxAttempts, clock.instant());
        if (failed > 0) {
            log.warn("Failed {} AI drafts after repeated interrupted attempts", failed);
        }
        int claimedCount = 0;
        for (int index = 0; index < batchSize; index++) {
            List<AiDraftClaim> claimed = drafts.claimQueued(1);
            if (claimed.isEmpty()) break;
            claimedCount++;
            process(claimed.getFirst());
        }
        return claimedCount;
    }

    private void process(AiDraftClaim claim) {
        AiDraft draft = drafts.findById(claim.draftId()).orElse(null);
        if (draft == null || draft.getStatus() != DraftStatus.GENERATING) {
            return;
        }
        AiDraftGenerator generator = generators.get(draft.getKind());
        if (generator == null) {
            fail(claim, draft, UNSUPPORTED_KIND);
            return;
        }
        try {
            GenerationOutcome outcome = generator.generate(draft);
            if (!state.complete(claim, outcome.resultJson(), outcome.model(), outcome.promptVersion(), clock.instant())) {
                log.info("Ignored completion of replaced AI draft claim {}", draft.getId());
            }
        } catch (ContentGenerationException failure) {
            fail(claim, draft, failure.code());
        } catch (RuntimeException unexpected) {
            log.error("AI draft {} failed unexpectedly: {}", draft.getId(), unexpected.getClass().getSimpleName());
            fail(claim, draft, INTERNAL);
        }
    }

    /** Every failure gives the author's use back: they received nothing usable for it. */
    private void fail(AiDraftClaim claim, AiDraft draft, String errorCode) {
        if (state.fail(claim, draft, errorCode, clock.instant())) {
            log.warn("AI draft {} failed with {}", draft.getId(), errorCode);
        } else {
            log.info("Ignored failure of replaced AI draft claim {}", draft.getId());
        }
    }
}
