package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.KnowledgePointCatalogRepository;
import com.ieltspath.learning.domain.vo.KnowledgePointCatalogEntry;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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
                INSERT INTO knowledge_point_catalog (kp_id, topic_id, has_practice_set, skill, band, parent_kp_id,
                error_code) VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (kp_id) DO UPDATE SET topic_id = EXCLUDED.topic_id,
                has_practice_set = EXCLUDED.has_practice_set, skill = EXCLUDED.skill, band = EXCLUDED.band,
                parent_kp_id = EXCLUDED.parent_kp_id, error_code = EXCLUDED.error_code,
                refreshed_at = clock_timestamp()
                """, ordered, ordered.size(), (statement, kp) -> {
            statement.setObject(1, kp.knowledgePointId());
            statement.setObject(2, kp.topicId());
            statement.setBoolean(3, kp.hasPracticeSet());
            statement.setString(4, kp.skill() == null ? null : kp.skill().name());
            statement.setObject(5, kp.band(), Types.NUMERIC);
            statement.setObject(6, kp.parentKnowledgePointId());
            statement.setString(7, kp.errorCode());
        });
    }

    @Override
    public List<KnowledgePointCatalogEntry> findAll() {
        return jdbc.query("SELECT kp_id, topic_id, has_practice_set, skill, band, parent_kp_id, error_code " +
                        "FROM knowledge_point_catalog",
                (rows, index) -> new KnowledgePointCatalogEntry(rows.getObject("kp_id", UUID.class),
                        rows.getObject("topic_id", UUID.class), rows.getBoolean("has_practice_set"),
                        rows.getString("skill") == null ? null : LearningSkill.valueOf(rows.getString("skill")),
                        rows.getBigDecimal("band"), rows.getObject("parent_kp_id", UUID.class),
                        rows.getString("error_code")));
    }
}
