package com.ieltspath.access.domain.repository;

import com.ieltspath.access.domain.aggregate.PointWallet;

import java.util.Optional;
import java.util.UUID;

public interface PointWalletRepository {

    Optional<PointWallet> findByUserId(UUID userId);

    PointWallet save(PointWallet pointWallet);
}
