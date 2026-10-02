package com.group01.assessment.domain.repository;

import com.group01.assessment.domain.entity.AttemptResponse;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptResponseRepository {
    AttemptResponse save(AttemptResponse response);
    Optional<AttemptResponse> findByAttemptItemId(UUID attemptItemId);
    List<AttemptResponse> findByAttemptItemIds(Collection<UUID> attemptItemIds);
}
