package com.ieltspath.learning.domain.repository;

import java.util.Map;
import java.util.UUID;

/** Admin overrides of the learning rules, as raw JSON values keyed by rule name. */
public interface LearningRuleConfigRepository {
    Map<String, String> findAll();

    void upsert(String key, String jsonValue, UUID updatedBy);
}
