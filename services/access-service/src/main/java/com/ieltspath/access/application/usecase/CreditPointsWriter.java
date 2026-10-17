package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.CreditPointsCommand;
import com.ieltspath.access.application.result.PointCreditResult;
import com.ieltspath.access.domain.aggregate.PointWallet;
import com.ieltspath.access.domain.entity.PointCreditRequest;
import com.ieltspath.access.domain.entity.PointLedgerEntry;
import com.ieltspath.access.domain.exception.DuplicateIdempotencyException;
import com.ieltspath.access.domain.repository.PointCreditRequestRepository;
import com.ieltspath.access.domain.repository.PointLedgerRepository;
import com.ieltspath.access.domain.repository.PointWalletRepository;
import com.ieltspath.access.domain.vo.PointTransactionType;
import org.springframework.beans.factory.annotation.Value;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Service
public class CreditPointsWriter {
    @PersistenceContext
    private EntityManager entityManager;

    private final PointWalletRepository walletRepository;
    private final PointLedgerRepository ledgerRepository;
    private final PointCreditRequestRepository creditRequestRepository;
    private final int dailyLimit;

    public CreditPointsWriter(PointWalletRepository walletRepository,
                              PointLedgerRepository ledgerRepository,
                              PointCreditRequestRepository creditRequestRepository,
                              @Value("${access.points.daily-earn-limit:20}") int dailyLimit) {
        if (dailyLimit < 0) throw new IllegalArgumentException("Daily point earning limit cannot be negative");
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
        this.creditRequestRepository = creditRequestRepository;
        this.dailyLimit = dailyLimit;
    }

    @Transactional
    public PointCreditResult credit(CreditPointsCommand command) {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        creditRequestRepository.lock(command.idempotencyKey(), command.userId(), todayUtc);

        var previous = creditRequestRepository.findByIdempotencyKey(command.idempotencyKey());
        if (previous.isPresent()) return replay(previous.get(), command);
        if (ledgerRepository.findByIdempotencyKey(command.idempotencyKey()).isPresent()) {
            throw new DuplicateIdempotencyException(command.idempotencyKey());
        }

        Instant startOfDayUtc = todayUtc.atStartOfDay().toInstant(ZoneOffset.UTC);
        long earnedToday = creditRequestRepository.sumGrantedSince(command.userId(), startOfDayUtc);
        int granted = (int) Math.min(command.amount(), Math.max(0L, (long) dailyLimit - earnedToday));
        boolean capped = granted < command.amount();

        PointWallet wallet = walletRepository.findByUserId(command.userId())
                .orElseGet(() -> PointWallet.create(command.userId()));
        UUID transactionId = null;
        if (granted > 0) {
            wallet.credit(granted);
            PointWallet savedWallet = walletRepository.save(wallet);
            PointLedgerEntry ledgerEntry = ledgerRepository.save(PointLedgerEntry.create(
                    command.userId(), granted, savedWallet.getBalance(), PointTransactionType.LEARNING_REWARD,
                    command.reason().name(), command.referenceId(), command.idempotencyKey(), null));
            entityManager.flush();
            transactionId = ledgerEntry.getId();
            wallet = savedWallet;
        }

        PointCreditResult result = new PointCreditResult(granted, capped, wallet.getBalance(), transactionId);
        creditRequestRepository.save(new PointCreditRequest(command.idempotencyKey(), command.userId(),
                command.amount(), command.reason(), command.referenceId(), result.granted(), result.capped(),
                result.balance(), result.transactionId()));
        return result;
    }

    private static PointCreditResult replay(PointCreditRequest request, CreditPointsCommand command) {
        boolean sameRequest = request.userId().equals(command.userId())
                && request.amount() == command.amount()
                && request.reason() == command.reason()
                && Objects.equals(request.referenceId(), command.referenceId());
        if (!sameRequest) throw new DuplicateIdempotencyException(command.idempotencyKey());
        return new PointCreditResult(request.granted(), request.capped(), request.balance(), request.transactionId());
    }
}
