package com.group01.access.domain.repository;

import com.group01.access.domain.entity.KeyActivation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KeyActivationRepository {

    Optional<KeyActivation> findById(UUID id);

    Optional<KeyActivation> findByIdempotencyKey(String idempotencyKey);

    Optional<KeyActivation> findByKeyId(UUID keyId);

    List<KeyActivation> findByUserId(UUID userId);

    KeyActivation save(KeyActivation keyActivation);
}
