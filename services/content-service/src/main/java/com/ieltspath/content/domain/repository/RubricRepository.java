package com.ieltspath.content.domain.repository;

import com.ieltspath.content.domain.aggregate.Rubric;

import java.util.List;
import java.util.Optional;

public interface RubricRepository {
    Rubric create(Rubric rubric);
    Rubric updateDraft(String code, int version, Rubric rubric);
    Rubric activate(String code, int version);
    List<Rubric> findActive(String task);
    List<Rubric> findDrafts(String task);
    Optional<Rubric> findByCodeAndVersion(String code, int version);
}
