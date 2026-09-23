package com.group01.access.domain.repository;

import com.group01.access.domain.aggregate.PointWallet;

import java.util.Optional;
import java.util.UUID;

public interface PointWalletRepository {

    Optional<PointWallet> findByUserId(UUID userId);

    PointWallet save(PointWallet pointWallet);
}
