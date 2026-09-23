package com.group01.assessment.domain.repository;

import com.group01.assessment.domain.entity.AttemptItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptItemRepository {
    List<AttemptItem> saveAll(List<AttemptItem> items);
    Optional<AttemptItem> findById(UUID id);
    List<AttemptItem> findByAttemptId(UUID attemptId);
    Optional<AttemptItem> findByIdAndAttemptId(UUID id, UUID attemptId);
}
