package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.aggregate.LearnerCurriculum;

import java.util.UUID;

public interface LearnerCurriculumRepository {
    /** The learner's topics; an empty curriculum when none is stored yet. */
    LearnerCurriculum find(UUID userId);

    void save(LearnerCurriculum curriculum);
}
