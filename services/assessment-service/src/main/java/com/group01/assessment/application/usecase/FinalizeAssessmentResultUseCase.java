package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.FinalizeAssessmentResultCommand;
import com.group01.assessment.application.result.AssessmentResultResult;
import com.group01.assessment.domain.entity.AssessmentResult;
import com.group01.assessment.domain.exception.AssessmentNotFoundException;
import com.group01.assessment.domain.exception.InvalidAssessmentStateException;
import com.group01.assessment.domain.repository.AssessmentAttemptRepository;
import com.group01.assessment.domain.repository.AssessmentResultRepository;
import com.group01.assessment.domain.repository.AttemptItemRepository;
import com.group01.assessment.domain.repository.ItemResultRepository;
import com.group01.assessment.domain.vo.AttemptStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grader entry that makes one fully graded result version final. The COMPLETED transition and the
 * AssessmentCompleted.v2 outbox row commit in the same transaction (see {@link AssessmentResultCompleter}).
 */
@Service
public class FinalizeAssessmentResultUseCase {
    private final AssessmentResultRepository results;
    private final AssessmentAttemptRepository attempts;
    private final AttemptItemRepository attemptItems;
    private final ItemResultRepository itemResults;
    private final AssessmentResultCompleter completer;

    public FinalizeAssessmentResultUseCase(AssessmentResultRepository results,
                                           AssessmentAttemptRepository attempts,
                                           AttemptItemRepository attemptItems,
                                           ItemResultRepository itemResults,
                                           AssessmentResultCompleter completer) {
        this.results = results;
        this.attempts = attempts;
        this.attemptItems = attemptItems;
        this.itemResults = itemResults;
        this.completer = completer;
    }

    @Transactional
    public AssessmentResultResult execute(FinalizeAssessmentResultCommand command) {
        AssessmentResult result = results.findForUpdateById(command.resultId())
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment result not found"));
        if (result.isCompleted()) {
            // Already final: its outbox row was written by the transaction that completed it.
            return toResult(result);
        }
        AssessmentResult latest = results.findLatestByAttemptId(result.attemptId()).orElse(result);
        if (latest.resultVersion() != result.resultVersion()) {
            throw new InvalidAssessmentStateException("Only the latest result version can be finalized");
        }
        var attempt = attempts.findById(result.attemptId())
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));
        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new InvalidAssessmentStateException("Only a submitted attempt can have a final result");
        }

        AssessmentResult completed = completer.complete(attempt, result, attemptItems.findByAttemptId(attempt.getId()),
                itemResults.findByResultId(result.id()));
        return toResult(completed);
    }

    private static AssessmentResultResult toResult(AssessmentResult result) {
        return new AssessmentResultResult(result.id(), result.attemptId(), result.resultVersion(), result.status(),
                result.overallBand(), result.completedAt());
    }
}
