package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.DebitPointsCommand;
import com.ieltspath.access.application.result.PointLedgerResult;
import com.ieltspath.access.domain.entity.PointLedgerEntry;
import com.ieltspath.access.domain.exception.DuplicateIdempotencyException;
import com.ieltspath.access.domain.exception.PointsActorMismatchException;
import com.ieltspath.access.domain.repository.PointLedgerRepository;
import com.ieltspath.access.domain.vo.PointTransactionType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Debits points for the authenticated learner, idempotently by key. Not transactional itself: the write runs in
 * {@link PointDebitWriter} so a concurrent duplicate can be resolved by reading the committed entry.
 */
@Service
public class DebitPointsUseCase {

    private final PointLedgerRepository pointLedgerRepository;
    private final PointDebitWriter pointDebitWriter;

    public DebitPointsUseCase(PointLedgerRepository pointLedgerRepository, PointDebitWriter pointDebitWriter) {
        this.pointLedgerRepository = pointLedgerRepository;
        this.pointDebitWriter = pointDebitWriter;
    }

    public PointLedgerResult execute(DebitPointsCommand command) {
        if (!Objects.equals(command.actorUserId(), command.userId())) {
            throw new PointsActorMismatchException();
        }
        var existing = pointLedgerRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return replay(existing.get(), command);
        }
        try {
            return pointDebitWriter.debit(command);
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
            // A concurrent request with the same key committed first; any other cause has no entry and is rethrown.
            PointLedgerEntry winner = pointLedgerRepository.findByIdempotencyKey(command.idempotencyKey())
                    .orElseThrow(() -> e);
            return replay(winner, command);
        }
    }

    /** A key may only be replayed by the same debit; anything else is a client reusing a key by mistake. */
    private static PointLedgerResult replay(PointLedgerEntry entry, DebitPointsCommand command) {
        boolean sameDebit = entry.getTransactionType() == PointTransactionType.AI_GRADING_DEBIT
                && entry.getUserId().equals(command.userId())
                && entry.getDelta() == -command.amount()
                && Objects.equals(entry.getReferenceType(), command.referenceType())
                && Objects.equals(entry.getReferenceId(), command.referenceId());
        if (!sameDebit) {
            throw new DuplicateIdempotencyException(command.idempotencyKey());
        }
        return toResult(entry);
    }

    static PointLedgerResult toResult(PointLedgerEntry e) {
        return new PointLedgerResult(
                e.getId(), e.getUserId(), e.getDelta(), e.getBalanceAfter(),
                e.getTransactionType(), e.getReferenceType(), e.getReferenceId(),
                e.getDescription(), e.getCreatedAt()
        );
    }
}
