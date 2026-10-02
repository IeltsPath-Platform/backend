package com.group01.learning.domain.repository;

import com.group01.learning.domain.aggregate.LearnerCurriculum;

import java.util.UUID;

public interface LearnerCurriculumRepository {
    /** The learner's topics; an empty curriculum when none is stored yet. */
    LearnerCurriculum find(UUID userId);

    void save(LearnerCurriculum curriculum);
}
