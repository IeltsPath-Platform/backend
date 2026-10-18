package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.Hearts;

import java.util.Optional;
import java.util.UUID;

public interface HeartsRepository {
    Optional<Hearts> find(UUID userId);

    /** Inserts or replaces the learner's hearts. */
    void save(Hearts hearts);
}
