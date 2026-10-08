package com.ieltspath.assessment.domain.repository;

import com.ieltspath.assessment.domain.entity.AttemptSection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptSectionRepository {
    List<AttemptSection> saveAll(List<AttemptSection> sections);
    AttemptSection save(AttemptSection section);
    Optional<AttemptSection> findById(UUID id);
    List<AttemptSection> findByAttemptId(UUID attemptId);
}
