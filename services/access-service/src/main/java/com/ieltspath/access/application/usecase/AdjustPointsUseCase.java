package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.AdjustPointsCommand;
import com.ieltspath.access.application.result.PointLedgerResult;
import com.ieltspath.access.domain.aggregate.PointWallet;
import com.ieltspath.access.domain.entity.PointLedgerEntry;
import com.ieltspath.access.domain.repository.PointLedgerRepository;
import com.ieltspath.access.domain.repository.PointWalletRepository;
import com.ieltspath.access.domain.vo.PointTransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AdjustPointsUseCase {

    private final PointWalletRepository pointWalletRepository;
    private final PointLedgerRepository pointLedgerRepository;

    public AdjustPointsUseCase(PointWalletRepository pointWalletRepository, PointLedgerRepository pointLedgerRepository) {
        this.pointWalletRepository = pointWalletRepository;
        this.pointLedgerRepository = pointLedgerRepository;
    }

    public PointLedgerResult execute(AdjustPointsCommand command) {
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
                .orElseGet(() -> PointWallet.create(command.userId()));

        if (command.delta() >= 0) {
            wallet.credit(command.delta());
        } else {
            wallet.debit(Math.abs(command.delta()));
        }
        pointWalletRepository.save(wallet);

        UUID refId = UUID.randomUUID();
        PointLedgerEntry entry = PointLedgerEntry.create(
                command.userId(),
                command.delta(),
                wallet.getBalance(),
                PointTransactionType.ADMIN_ADJUSTMENT,
                "ADMIN",
                refId,
                command.idempotencyKey(),
                command.reason()
        );
        PointLedgerEntry saved = pointLedgerRepository.save(entry);

        return new PointLedgerResult(
                saved.getId(), saved.getUserId(), saved.getDelta(), saved.getBalanceAfter(),
                saved.getTransactionType(), saved.getReferenceType(), saved.getReferenceId(),
                saved.getDescription(), saved.getCreatedAt()
        );
    }
}
