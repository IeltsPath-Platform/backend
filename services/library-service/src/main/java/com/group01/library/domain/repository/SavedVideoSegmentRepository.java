package com.group01.library.domain.repository;

import com.group01.library.domain.aggregate.SavedVideoSegment;
import com.group01.library.domain.vo.OwnedPage;

import java.util.Optional;
import java.util.UUID;

public interface SavedVideoSegmentRepository {
    SavedVideoSegment save(SavedVideoSegment segment);

    OwnedPage<SavedVideoSegment> findByUserId(UUID userId, int page, int size);

    Optional<SavedVideoSegment> findByIdAndUserId(UUID id, UUID userId);

    void delete(SavedVideoSegment segment);
}
