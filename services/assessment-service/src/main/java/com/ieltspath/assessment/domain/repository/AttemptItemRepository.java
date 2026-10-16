package com.ieltspath.assessment.domain.repository;

import com.ieltspath.assessment.domain.entity.AttemptItem;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AttemptItemRepository {
    List<AttemptItem> saveAll(List<AttemptItem> items);
    Optional<AttemptItem> findById(UUID id);
    List<AttemptItem> findByAttemptId(UUID attemptId);
    Optional<AttemptItem> findByIdAndAttemptId(UUID id, UUID attemptId);

    Set<UUID> findExistingIdsByAttemptId(Set<UUID> itemIds, UUID attemptId);

    /**
     * Keeps the larger of the stored and the given elapsed time. A running total from the client can only grow, so a
     * late, smaller report never shrinks the stored value. The caller clamps the value first.
     */
    void recordElapsed(UUID attemptItemId, long elapsedMs);

    /** Stored elapsed time per item; an item without a report is absent from the map. */
    Map<UUID, Long> elapsedMsByItemId(Collection<UUID> attemptItemIds);
}
