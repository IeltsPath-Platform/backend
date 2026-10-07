package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.DebitPointsCommand;
import com.ieltspath.access.application.result.PointLedgerResult;
import com.ieltspath.access.domain.aggregate.PointWallet;
import com.ieltspath.access.domain.entity.OutboxEvent;
import com.ieltspath.access.domain.entity.PointLedgerEntry;
import com.ieltspath.access.domain.exception.InsufficientPointsException;
import com.ieltspath.access.domain.repository.OutboxEventRepository;
import com.ieltspath.access.domain.repository.PointLedgerRepository;
import com.ieltspath.access.domain.repository.PointWalletRepository;
import com.ieltspath.access.domain.vo.PointTransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes one debit in its own transaction, so a unique-key violation from a concurrent request with the same
 * idempotency key surfaces after rollback, where {@link DebitPointsUseCase} can read the winning entry.
 */
@Service
public class PointDebitWriter {

    private final PointWalletRepository pointWalletRepository;
    private final PointLedgerRepository pointLedgerRepository;
    private final OutboxEventRepository outboxEventRepository;

    public PointDebitWriter(
            PointWalletRepository pointWalletRepository,
            PointLedgerRepository pointLedgerRepository,
            OutboxEventRepository outboxEventRepository
    ) {
        this.pointWalletRepository = pointWalletRepository;
        this.pointLedgerRepository = pointLedgerRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public PointLedgerResult debit(DebitPointsCommand command) {
        PointWallet wallet = pointWalletRepository.findByUserId(command.userId())
                .orElseThrow(() -> new InsufficientPointsException(command.userId(), 0L, command.amount()));

        wallet.debit(command.amount());
        pointWalletRepository.save(wallet);

        PointLedgerEntry savedEntry = pointLedgerRepository.save(PointLedgerEntry.create(
                command.userId(),
                -command.amount(),
                wallet.getBalance(),
                PointTransactionType.AI_GRADING_DEBIT,
                command.referenceType(),
                command.referenceId(),
                command.idempotencyKey(),
                command.description()
        ));

        outboxEventRepository.save(OutboxEvent.create(
                "PointWallet", command.userId(), "PointDebited",
                String.format("{\"userId\":\"%s\",\"amount\":%d,\"balance\":%d}", command.userId(), command.amount(), wallet.getBalance())
        ));

        return DebitPointsUseCase.toResult(savedEntry);
    }
}
