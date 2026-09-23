package com.group01.assessment.domain.repository;

import com.group01.assessment.domain.entity.AttemptSection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttemptSectionRepository {
    List<AttemptSection> saveAll(List<AttemptSection> sections);
    Optional<AttemptSection> findById(UUID id);
    List<AttemptSection> findByAttemptId(UUID attemptId);
}
