package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.LearningRuleConfigRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
public class JdbcLearningRuleConfigRepository implements LearningRuleConfigRepository {
    private final JdbcTemplate jdbc;

    public JdbcLearningRuleConfigRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, String> findAll() {
        Map<String, String> values = new HashMap<>();
        jdbc.query("SELECT key, value::text AS value FROM learning_rule_configs",
                rows -> {
                    values.put(rows.getString("key"), rows.getString("value"));
                });
        return values;
    }

    @Override
    public void upsert(String key, String jsonValue, UUID updatedBy) {
        jdbc.update("""
                INSERT INTO learning_rule_configs (key, value, updated_by, updated_at)
                VALUES (?, ?::jsonb, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, updated_by = EXCLUDED.updated_by,
                    updated_at = CURRENT_TIMESTAMP
                """, key, jsonValue, updatedBy);
    }
}
