package com.group01.access.application.usecase;

import com.group01.access.application.command.DebitPointsCommand;
import com.group01.access.application.result.PointLedgerResult;
import com.group01.access.domain.aggregate.PointWallet;
import com.group01.access.domain.entity.OutboxEvent;
import com.group01.access.domain.entity.PointLedgerEntry;
import com.group01.access.domain.exception.InsufficientPointsException;
import com.group01.access.domain.repository.OutboxEventRepository;
import com.group01.access.domain.repository.PointLedgerRepository;
import com.group01.access.domain.repository.PointWalletRepository;
import com.group01.access.domain.vo.PointTransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class DebitPointsUseCase {

    private final PointWalletRepository pointWalletRepository;
    private final PointLedgerRepository pointLedgerRepository;
    private final OutboxEventRepository outboxEventRepository;

    public DebitPointsUseCase(
            PointWalletRepository pointWalletRepository,
            PointLedgerRepository pointLedgerRepository,
            OutboxEventRepository outboxEventRepository
    ) {
        this.pointWalletRepository = pointWalletRepository;
        this.pointLedgerRepository = pointLedgerRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    public PointLedgerResult execute(DebitPointsCommand command) {
        // Idempotent replay
        Optional<PointLedgerEntry> existing = pointLedgerRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            PointLedgerEntry e = existing.get();
            return new PointLedgerResult(
                    e.getId(), e.getUserId(), e.getDelta(), e.getBalanceAfter(),
                    e.getTransactionType(), e.getReferenceType(), e.getReferenceId(),
                    e.getDescription(), e.getCreatedAt()
            );
        }

        PointWallet wallet = pointWalletRepository.findByUserId(command.userId())
                .orElseThrow(() -> new InsufficientPointsException(command.userId(), 0L, command.amount()));

        wallet.debit(command.amount());
        pointWalletRepository.save(wallet);

        PointLedgerEntry ledgerEntry = PointLedgerEntry.create(
                command.userId(),
                -command.amount(),
                wallet.getBalance(),
                PointTransactionType.AI_GRADING_DEBIT,
                command.referenceType(),
                command.referenceId(),
                command.idempotencyKey(),
                command.description()
        );
        PointLedgerEntry savedEntry = pointLedgerRepository.save(ledgerEntry);

        outboxEventRepository.save(OutboxEvent.create(
                "PointWallet", command.userId(), "PointDebited",
                String.format("{\"userId\":\"%s\",\"amount\":%d,\"balance\":%d}", command.userId(), command.amount(), wallet.getBalance())
        ));

        return new PointLedgerResult(
                savedEntry.getId(), savedEntry.getUserId(), savedEntry.getDelta(), savedEntry.getBalanceAfter(),
                savedEntry.getTransactionType(), savedEntry.getReferenceType(), savedEntry.getReferenceId(),
                savedEntry.getDescription(), savedEntry.getCreatedAt()
        );
    }
}
