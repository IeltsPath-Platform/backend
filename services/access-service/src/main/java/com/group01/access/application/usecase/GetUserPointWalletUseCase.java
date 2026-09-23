package com.group01.access.application.usecase;

import com.group01.access.application.result.PointWalletResult;
import com.group01.access.domain.aggregate.PointWallet;
import com.group01.access.domain.repository.PointWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetUserPointWalletUseCase {

    private final PointWalletRepository pointWalletRepository;

    public GetUserPointWalletUseCase(PointWalletRepository pointWalletRepository) {
        this.pointWalletRepository = pointWalletRepository;
    }

    public PointWalletResult execute(UUID userId) {
        return pointWalletRepository.findByUserId(userId)
                .map(w -> new PointWalletResult(w.getUserId(), w.getBalance(), w.getTotalCredited(), w.getTotalDebited(), w.getUpdatedAt()))
                .orElseGet(() -> new PointWalletResult(userId, 0L, 0L, 0L, Instant.now()));
    }
}
