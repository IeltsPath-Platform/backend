package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.KnowledgePointCatalogRepository;
import com.ieltspath.learning.domain.vo.KnowledgePointCatalogEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
public class JdbcKnowledgePointCatalogRepository implements KnowledgePointCatalogRepository {
    private final JdbcTemplate jdbc;

    public JdbcKnowledgePointCatalogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsert(List<KnowledgePointCatalogEntry> entries) {
        if (entries.isEmpty()) return;
        // Shared catalog rows need the same lock order even when Content reorders topics.
        var ordered = entries.stream().sorted(Comparator.comparing(kp -> kp.knowledgePointId().toString())).toList();
        jdbc.batchUpdate("""
                INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set, skill) VALUES (?, ?, ?, ?)
                ON CONFLICT (kp_id) DO UPDATE SET topic_id = EXCLUDED.topic_id,
                has_practice_set = EXCLUDED.has_practice_set, skill = EXCLUDED.skill,
                refreshed_at = clock_timestamp()
                """, ordered, ordered.size(), (statement, kp) -> {
            statement.setObject(1, kp.knowledgePointId());
            statement.setObject(2, kp.topicId());
            statement.setBoolean(3, kp.hasPracticeSet());
            statement.setString(4, kp.skill() == null ? null : kp.skill().name());
        });
    }
}
