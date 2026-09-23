package com.group01.access.domain.repository;

import com.group01.access.domain.aggregate.ActivationKey;
import com.group01.access.domain.vo.KeyStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivationKeyRepository {

    Optional<ActivationKey> findById(UUID id);

    Optional<ActivationKey> findByCodeHash(String codeHash);

    List<ActivationKey> findAll(int offset, int limit);

    List<ActivationKey> findByStatus(KeyStatus status, int offset, int limit);

    List<ActivationKey> findByProductId(UUID productId, int offset, int limit);

    ActivationKey save(ActivationKey activationKey);

    List<ActivationKey> saveAll(List<ActivationKey> activationKeys);
}
