package com.group01.learning.infrastructure.persistence;

import com.group01.learning.domain.repository.KnowledgePointCatalogRepository;
import com.group01.learning.domain.vo.KnowledgePointCatalogEntry;
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
                INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set) VALUES (?, ?, ?)
                ON CONFLICT (kp_id) DO UPDATE SET topic_id = EXCLUDED.topic_id,
                has_practice_set = EXCLUDED.has_practice_set, refreshed_at = clock_timestamp()
                """, ordered, ordered.size(), (statement, kp) -> {
            statement.setObject(1, kp.knowledgePointId());
            statement.setObject(2, kp.topicId());
            statement.setBoolean(3, kp.hasPracticeSet());
        });
    }
}
