package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.repository.KnowledgePrerequisiteRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JdbcKnowledgePrerequisiteRepository implements KnowledgePrerequisiteRepository {
    private static final long KNOWLEDGE_TREE_LOCK_KEY = 726510283947L;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcKnowledgePrerequisiteRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void lockTree() {
        jdbc.getJdbcTemplate().query("SELECT pg_advisory_xact_lock(?)",
                statement -> statement.setLong(1, KNOWLEDGE_TREE_LOCK_KEY), resultSet -> {
                    resultSet.next();
                    return null;
                });
    }

    @Override
    public List<UUID> findPrerequisiteIds(UUID knowledgePointId) {
        return jdbc.query("""
                SELECT prerequisite_id FROM knowledge_point_prerequisites
                WHERE knowledge_point_id = :knowledgePointId
                ORDER BY prerequisite_id
                """, Map.of("knowledgePointId", knowledgePointId),
                (rs, row) -> uuid(rs, "prerequisite_id"));
    }

    @Override
    public List<Edge> findAll() {
        return jdbc.query("""
                SELECT prerequisite_id, knowledge_point_id FROM knowledge_point_prerequisites
                ORDER BY prerequisite_id, knowledge_point_id
                """, (rs, row) -> new Edge(uuid(rs, "prerequisite_id"), uuid(rs, "knowledge_point_id")));
    }

    @Override
    public void replace(UUID knowledgePointId, List<UUID> prerequisiteIds) {
        jdbc.update("DELETE FROM knowledge_point_prerequisites WHERE knowledge_point_id = :knowledgePointId",
                Map.of("knowledgePointId", knowledgePointId));
        if (!prerequisiteIds.isEmpty()) {
            SqlParameterSource[] parameters = prerequisiteIds.stream()
                    .map(prerequisiteId -> new MapSqlParameterSource()
                            .addValue("knowledgePointId", knowledgePointId)
                            .addValue("prerequisiteId", prerequisiteId))
                    .toArray(SqlParameterSource[]::new);
            jdbc.batchUpdate("""
                    INSERT INTO knowledge_point_prerequisites (knowledge_point_id, prerequisite_id)
                    VALUES (:knowledgePointId, :prerequisiteId)
                    """, parameters);
        }
    }

    private static UUID uuid(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, UUID.class);
    }
}
