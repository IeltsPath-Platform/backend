package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.infrastructure.persistence.mapper.QuestionPersistenceMapper;
import com.ieltspath.content.infrastructure.persistence.repository.QuestionJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class QuestionRepositoryAdapter implements QuestionRepository {

    private final QuestionJpaRepository questionJpaRepository;
    private final QuestionPersistenceMapper mapper;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbc;

    public QuestionRepositoryAdapter(QuestionJpaRepository questionJpaRepository,
                                     QuestionPersistenceMapper mapper,
                                     JdbcTemplate jdbcTemplate,
                                     NamedParameterJdbcTemplate namedJdbc) {
        this.questionJpaRepository = questionJpaRepository;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbc = namedJdbc;
    }

    @Override
    public Question save(Question question) {
        var entity = mapper.toEntity(question);
        var saved = questionJpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Question> findById(UUID id) {
        return questionJpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Question> findByIdForUpdate(UUID id) {
        var rows = jdbcTemplate.query("SELECT id FROM questions WHERE id = ? FOR UPDATE",
                (rs, row) -> rs.getObject("id", UUID.class), id);
        return rows.isEmpty() ? Optional.empty() : findById(id);
    }

    @Override
    public List<Question> findByIds(List<UUID> ids) {
        return questionJpaRepository.findAllByIdIn(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Question> findByVersionIdsForUpdate(List<UUID> versionIds) {
        if (versionIds.isEmpty()) return List.of();
        return questionJpaRepository.findAllByVersionIdsForUpdate(versionIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Page findPage(Skill skill, QuestionPurpose purpose, PublicationStatus status, String code, int page, int size) {
        var result = questionJpaRepository.findPage(skill, purpose, status, code == null ? "" : code,
                PageRequest.of(page, size));
        return new Page(result.getTotalElements(), result.getContent().stream().map(mapper::toDomainSummary).toList());
    }

    @Override
    public Page findPageByKnowledgePoints(Skill skill, QuestionPurpose purpose, PublicationStatus status, String code,
                                          List<UUID> knowledgePointIds, int page, int size) {
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) return findPage(skill, purpose, status, code, page, size);
        var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("skill", skill == null ? null : skill.name())
                .addValue("purpose", purpose == null ? null : purpose.name())
                .addValue("status", status == null ? null : status.name())
                .addValue("code", code == null ? "" : code)
                .addValue("ids", knowledgePointIds).addValue("limit", size).addValue("offset", page * size);
        String where = """
                WHERE (:skill IS NULL OR q.skill = :skill) AND (:purpose IS NULL OR q.purpose = :purpose)
                  AND (:status IS NULL OR q.status = :status)
                  AND (:code = '' OR LOWER(q.code) LIKE LOWER(CONCAT('%', :code, '%')))
                  AND q.current_published_version_id IS NOT NULL
                  AND EXISTS (SELECT 1 FROM question_knowledge_points qkp
                              WHERE qkp.question_version_id = q.current_published_version_id
                                AND qkp.knowledge_point_id IN (:ids))
                """;
        Long total = namedJdbc.queryForObject("SELECT COUNT(*) FROM questions q " + where, params, Long.class);
        List<UUID> ids = namedJdbc.query("SELECT q.id FROM questions q " + where
                        + " ORDER BY q.created_at DESC, q.id LIMIT :limit OFFSET :offset", params,
                (rs, row) -> rs.getObject(1, UUID.class));
        if (ids.isEmpty()) return new Page(total == null ? 0 : total, List.of());
        var byId = questionJpaRepository.findAllByIdIn(ids).stream().map(mapper::toDomainSummary)
                .collect(java.util.stream.Collectors.toMap(Question::getId, q -> q));
        return new Page(total == null ? 0 : total, ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList());
    }

    @Override
    public Page findFreePage(Skill skill, QuestionPurpose purpose, PublicationStatus status, String code,
                             UUID exceptBlockId, int page, int size) {
        var pageable = PageRequest.of(page, size);
        String skillValue = skill == null ? null : skill.name();
        String purposeValue = purpose == null ? null : purpose.name();
        String statusValue = status == null ? null : status.name();
        var result = exceptBlockId == null
                ? questionJpaRepository.findFreePage(skillValue, purposeValue, statusValue, code == null ? "" : code, pageable)
                : questionJpaRepository.findFreePageExceptBlock(skillValue, purposeValue, statusValue, code == null ? "" : code,
                        exceptBlockId, pageable);
        return new Page(result.getTotalElements(), result.getContent().stream().map(mapper::toDomainSummary).toList());
    }

    @Override
    public Page findFreePageByKnowledgePoints(Skill skill, QuestionPurpose purpose, PublicationStatus status, String code,
                                             UUID exceptBlockId, List<UUID> knowledgePointIds, int page, int size) {
        if (knowledgePointIds == null || knowledgePointIds.isEmpty())
            return findFreePage(skill, purpose, status, code, exceptBlockId, page, size);
        var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("skill", skill == null ? null : skill.name())
                .addValue("purpose", purpose == null ? null : purpose.name())
                .addValue("status", status == null ? null : status.name())
                .addValue("code", code == null ? "" : code)
                .addValue("exceptBlockId", exceptBlockId)
                .addValue("ids", knowledgePointIds).addValue("limit", size).addValue("offset", page * size);
        String where = """
                WHERE (:skill IS NULL OR q.skill = :skill) AND (:purpose IS NULL OR q.purpose = :purpose)
                  AND (:status IS NULL OR q.status = :status)
                  AND (:code = '' OR LOWER(q.code) LIKE LOWER(CONCAT('%', :code, '%')))
                  AND q.current_published_version_id IS NOT NULL
                  AND EXISTS (SELECT 1 FROM question_knowledge_points qkp
                              WHERE qkp.question_version_id = q.current_published_version_id
                                AND qkp.knowledge_point_id IN (:ids))
                  AND NOT EXISTS (SELECT 1 FROM lesson_block_questions lbq
                                  WHERE lbq.question_version_id = q.current_published_version_id
                                    AND (:exceptBlockId IS NULL OR lbq.block_id <> :exceptBlockId))
                  AND NOT EXISTS (SELECT 1 FROM section_questions sq
                                  WHERE sq.question_version_id = q.current_published_version_id)
                """;
        Long total = namedJdbc.queryForObject("SELECT COUNT(*) FROM questions q " + where, params, Long.class);
        List<UUID> ids = namedJdbc.query("SELECT q.id FROM questions q " + where
                        + " ORDER BY q.created_at DESC, q.id LIMIT :limit OFFSET :offset", params,
                (rs, row) -> rs.getObject(1, UUID.class));
        if (ids.isEmpty()) return new Page(total == null ? 0 : total, List.of());
        var byId = questionJpaRepository.findAllByIdIn(ids).stream().map(mapper::toDomainSummary)
                .collect(java.util.stream.Collectors.toMap(Question::getId, q -> q));
        return new Page(total == null ? 0 : total,
                ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList());
    }

    @Override
    public List<VersionStem> findVersionStems(List<UUID> versionIds) {
        if (versionIds.isEmpty()) return List.of();
        return namedJdbc.query("SELECT id, stem FROM question_versions WHERE id IN (:ids)",
                java.util.Map.of("ids", versionIds), (rs, row) -> new VersionStem(
                        rs.getObject("id", UUID.class), rs.getString("stem")));
    }

    @Override
    public List<VersionKnowledgePoint> findVersionKnowledgePoints(List<UUID> versionIds) {
        if (versionIds.isEmpty()) return List.of();
        return namedJdbc.query("""
                WITH ranked_points AS (
                    SELECT qkp.question_version_id, kp.code, kp.name,
                           COUNT(*) OVER (PARTITION BY qkp.question_version_id) AS total,
                           ROW_NUMBER() OVER (PARTITION BY qkp.question_version_id
                               ORDER BY qkp.weight DESC, kp.code ASC) AS point_rank
                    FROM question_knowledge_points qkp
                    JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id
                    WHERE qkp.question_version_id IN (:ids)
                )
                SELECT question_version_id, code, name, total FROM ranked_points
                WHERE point_rank <= 4 ORDER BY question_version_id, point_rank
                """, java.util.Map.of("ids", versionIds), (rs, row) -> new VersionKnowledgePoint(
                rs.getObject("question_version_id", UUID.class), rs.getString("code"), rs.getString("name"),
                rs.getInt("total")));
    }

    @Override
    public Optional<Question> findByImportRef(String importRef) {
        return questionJpaRepository.findFirstByImportRef(importRef).map(mapper::toDomainSummary);
    }

    @Override
    public boolean existsByCode(String code) {
        return questionJpaRepository.existsByCode(code);
    }
}

