package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.ErrorCategory;
import com.ieltspath.content.domain.vo.ErrorReviewStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JdbcErrorTypeRepository implements ErrorTypeRepository {
    private static final String COLUMNS = "id, code, name, category, severity, knowledge_point_id, "
            + "trap_knowledge_point_id, fix_hint, status, "
            + "review_status, created_at, updated_at";

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcErrorTypeRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public ErrorType save(ErrorType errorType) {
        jdbc.update("""
                INSERT INTO error_types (id, code, name, category, severity, knowledge_point_id, trap_knowledge_point_id,
                                         fix_hint, status,
                                         review_status, created_at, updated_at)
                VALUES (:id, :code, :name, :category, :severity, :knowledgePointId, :trapKnowledgePointId, :fixHint, :status,
                        :reviewStatus, :createdAt, :updatedAt)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, category = EXCLUDED.category,
                    severity = EXCLUDED.severity, knowledge_point_id = EXCLUDED.knowledge_point_id,
                    trap_knowledge_point_id = EXCLUDED.trap_knowledge_point_id,
                    fix_hint = EXCLUDED.fix_hint, status = EXCLUDED.status, review_status = EXCLUDED.review_status,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", errorType.getId())
                .addValue("code", errorType.getCode())
                .addValue("name", errorType.getName())
                .addValue("category", errorType.getCategory().name())
                .addValue("severity", errorType.getSeverity())
                .addValue("knowledgePointId", errorType.getKnowledgePointId())
                .addValue("trapKnowledgePointId", errorType.getTrapKnowledgePointId())
                .addValue("fixHint", errorType.getFixHint())
                .addValue("status", errorType.getStatus().name())
                .addValue("reviewStatus", errorType.getReviewStatus().name())
                .addValue("createdAt", Timestamp.from(errorType.getCreatedAt()))
                .addValue("updatedAt", Timestamp.from(errorType.getUpdatedAt())));
        return errorType;
    }

    @Override
    public Optional<ErrorType> findById(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM error_types WHERE id = :id",
                new MapSqlParameterSource("id", id), JdbcErrorTypeRepository::map).stream().findFirst();
    }

    @Override
    public List<ErrorType> findByKnowledgePointId(UUID knowledgePointId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM error_types WHERE knowledge_point_id = :knowledgePointId ORDER BY code",
                new MapSqlParameterSource("knowledgePointId", knowledgePointId), JdbcErrorTypeRepository::map);
    }

    @Override
    public boolean existsByCode(String code) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM error_types WHERE code = :code",
                new MapSqlParameterSource("code", code), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public List<ErrorType> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM error_types ORDER BY code", JdbcErrorTypeRepository::map);
    }

    @Override
    public List<ErrorType> findActive() {
        return jdbc.query("SELECT " + COLUMNS + " FROM error_types WHERE status = 'ACTIVE' ORDER BY code",
                JdbcErrorTypeRepository::map);
    }

    @Override
    public List<ErrorType> findApprovedActive() {
        return jdbc.query("SELECT " + COLUMNS + " FROM error_types WHERE status = 'ACTIVE' AND review_status = 'APPROVED' ORDER BY code",
                JdbcErrorTypeRepository::map);
    }

    @Override
    public Set<UUID> findTrapKnowledgePointIds(Collection<UUID> knowledgePointIds) {
        if (knowledgePointIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(jdbc.queryForList("SELECT trap_knowledge_point_id FROM error_types "
                        + "WHERE trap_knowledge_point_id IN (:ids)",
                new MapSqlParameterSource("ids", knowledgePointIds), UUID.class));
    }

    @Override
    public boolean containsTrapKnowledgePoints(Collection<UUID> knowledgePointIds) {
        if (knowledgePointIds.isEmpty()) {
            return false;
        }
        Boolean found = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM error_types "
                        + "WHERE trap_knowledge_point_id IN (:ids))",
                new MapSqlParameterSource("ids", knowledgePointIds), Boolean.class);
        return Boolean.TRUE.equals(found);
    }

    @Override
    public Set<String> findActiveCodes(Collection<String> codes) {
        if (codes.isEmpty()) {
            return Set.of();
        }
        return jdbc.queryForList("SELECT code FROM error_types WHERE status = 'ACTIVE' AND review_status = 'APPROVED' AND code IN (:codes)",
                        new MapSqlParameterSource("codes", codes), String.class)
                .stream().collect(Collectors.toUnmodifiableSet());
    }

    private static ErrorType map(ResultSet rs, int row) throws SQLException {
        UUID knowledgePointId = rs.getObject("knowledge_point_id", UUID.class);
        return new ErrorType(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"),
                ErrorCategory.valueOf(rs.getString("category")), rs.getInt("severity"), knowledgePointId,
                rs.getObject("trap_knowledge_point_id", UUID.class),
                rs.getString("fix_hint"), ContentStatus.valueOf(rs.getString("status")),
                ErrorReviewStatus.valueOf(rs.getString("review_status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
