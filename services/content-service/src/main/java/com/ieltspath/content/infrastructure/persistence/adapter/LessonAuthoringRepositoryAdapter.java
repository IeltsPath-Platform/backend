package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.Lesson;
import com.ieltspath.content.domain.aggregate.LessonBlock;
import com.ieltspath.content.domain.exception.DuplicateCodeException;
import com.ieltspath.content.domain.exception.LessonBlockInvalidException;
import com.ieltspath.content.domain.exception.InvalidLessonBlockException;
import com.ieltspath.content.domain.exception.LessonSortConflictException;
import com.ieltspath.content.domain.repository.LessonAuthoringRepository;
import com.ieltspath.content.domain.vo.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
public class LessonAuthoringRepositoryAdapter implements LessonAuthoringRepository {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public LessonAuthoringRepositoryAdapter(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = namedJdbc;
    }

    @Override
    public boolean topicExists(UUID topicId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM topics WHERE id = ?)",
                Boolean.class, topicId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Skill> courseTopicSkillForLesson(UUID lessonId) {
        return jdbc.query("""
                SELECT COALESCE(t.skill, 'ALL') AS skill FROM lessons l JOIN topics t ON t.id = l.topic_id
                WHERE l.id = ? AND t.course_id IS NOT NULL
                """, rs -> rs.next() ? Optional.ofNullable(Skill.valueOf(rs.getString("skill"))) : Optional.empty(),
                lessonId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Skill> knowledgePointSkills(List<UUID> knowledgePointIds) {
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) return Map.of();
        Map<UUID, Skill> skills = new LinkedHashMap<>();
        namedJdbc.query("SELECT id, skill FROM knowledge_points WHERE id IN (:ids)",
                Map.of("ids", knowledgePointIds), (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                        skills.put(rs.getObject("id", UUID.class), Skill.valueOf(rs.getString("skill"))));
        return Map.copyOf(skills);
    }

    @Override
    @Transactional
    public Lesson create(UUID topicId, String code, String title, String summary, String summaryMarkdown,
                         Integer estimatedMinutes, Integer sortOrder) {
        Integer topicOrder = jdbc.queryForObject("SELECT COALESCE(MAX(sort_order), -1) FROM lessons WHERE topic_id = ?",
                Integer.class, topicId);
        int position = sortOrder == null ? topicOrder + 1 : sortOrder;
        if (position < 0) throw new IllegalArgumentException("sortOrder must not be negative");
        UUID lessonId = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO lessons(id, topic_id, code, title, summary, summary_markdown,
                                        estimated_minutes, sort_order, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT')
                    """, lessonId, topicId, code, title, summary, summaryMarkdown, estimatedMinutes, position);
        } catch (DataIntegrityViolationException exception) {
            translateLessonConstraint(exception, code);
        }
        return loadAggregate(lessonId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonAuthoringSnapshot> findByTopic(UUID topicId) {
        List<LessonRow> rows = jdbc.query("SELECT * FROM lessons WHERE topic_id = ? ORDER BY sort_order",
                lessonRowMapper(), topicId);
        return loadSnapshots(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LessonAuthoringSnapshot> findSnapshotById(UUID lessonId) {
        List<LessonRow> rows = jdbc.query("SELECT * FROM lessons WHERE id = ?", lessonRowMapper(), lessonId);
        return loadSnapshots(rows).stream().findFirst();
    }

    @Override
    @Transactional
    public Optional<Lesson> lockAndFindById(UUID lessonId) {
        List<UUID> locked = jdbc.query("SELECT id FROM lessons WHERE id = ? FOR UPDATE",
                (rs, row) -> rs.getObject(1, UUID.class), lessonId);
        return locked.isEmpty() ? Optional.empty() : Optional.of(loadAggregate(lessonId));
    }

    @Override
    @Transactional
    public Lesson save(Lesson lesson) {
        jdbc.update("""
                UPDATE lessons SET title = ?, summary = ?, summary_markdown = ?, estimated_minutes = ?,
                    sort_order = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, lesson.getTitle(), lesson.getSummary(), lesson.getSummaryMarkdown(),
                lesson.getEstimatedMinutes(), lesson.getSortOrder(), lesson.getStatus().name(), lesson.getId());
        saveKnowledgePoints(lesson);
        saveBlocks(lesson);
        return loadAggregate(lesson.getId());
    }

    @Override
    @Transactional
    public void reorderLessons(UUID topicId, List<UUID> lessonIds) {
        List<UUID> currentIds = jdbc.query("SELECT id FROM lessons WHERE topic_id = ? ORDER BY sort_order",
                (rs, row) -> rs.getObject(1, UUID.class), topicId);
        validateOrder(currentIds, lessonIds);
        jdbc.update("UPDATE lessons SET sort_order = sort_order + 1000000 WHERE topic_id = ?", topicId);
        for (int index = 0; index < lessonIds.size(); index++) {
            jdbc.update("UPDATE lessons SET sort_order = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                    index, lessonIds.get(index));
        }
    }

    @Override
    public List<String> publicationViolations(UUID lessonId) {
        List<String> errors = new ArrayList<>();
        if (count("SELECT COUNT(*) FROM lesson_knowledge_points lkp JOIN knowledge_points kp ON kp.id=lkp.knowledge_point_id WHERE lkp.lesson_id=? AND kp.status='ACTIVE'", lessonId) == 0) {
            errors.add("LESSON_HAS_NO_ACTIVE_KP");
        }
        if (count("SELECT COUNT(*) FROM lesson_block_knowledge_points lbkp JOIN knowledge_points kp ON kp.id=lbkp.knowledge_point_id JOIN lesson_blocks b ON b.id=lbkp.block_id WHERE b.lesson_id=? AND kp.status<>'ACTIVE'", lessonId) > 0) {
            errors.add("TEXT_BLOCK_KP_INACTIVE");
        }
        if (count("SELECT COUNT(*) FROM lesson_block_questions lbq JOIN lesson_blocks b ON b.id=lbq.block_id JOIN question_versions qv ON qv.id=lbq.question_version_id JOIN questions q ON q.id=qv.question_id WHERE b.lesson_id=? AND qv.status<>'PUBLISHED'", lessonId) > 0) {
            errors.add("EXERCISE_QUESTION_NOT_PUBLISHED");
        }
        if (count("SELECT COUNT(*) FROM lesson_block_questions lbq JOIN lesson_blocks b ON b.id=lbq.block_id JOIN questions q ON q.id=(SELECT question_id FROM question_versions WHERE id=lbq.question_version_id) WHERE b.lesson_id=? AND q.purpose IS DISTINCT FROM 'LEARNING'", lessonId) > 0) {
            errors.add("EXERCISE_QUESTION_PURPOSE_INVALID");
        }
        if (count("SELECT COUNT(*) FROM lesson_block_questions lbq JOIN lesson_blocks b ON b.id=lbq.block_id WHERE b.lesson_id=? AND ((SELECT COUNT(*) FROM lesson_block_questions all_l WHERE all_l.question_version_id=lbq.question_version_id) <> 1 OR EXISTS (SELECT 1 FROM section_questions sq WHERE sq.question_version_id=lbq.question_version_id))", lessonId) > 0) {
            errors.add("EXERCISE_QUESTION_ALREADY_OWNED");
        }
        errors.addAll(exerciseBlockIssues(lessonId));
        return errors;
    }

    @Override
    public List<UUID> questionVersionsOwnedElsewhere(UUID exceptBlockId, List<UUID> questionVersionIds) {
        if (questionVersionIds.isEmpty()) return List.of();
        java.util.Set<UUID> owned = new java.util.HashSet<>();
        org.springframework.jdbc.core.namedparam.MapSqlParameterSource params =
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("ids", questionVersionIds)
                        .addValue("except", exceptBlockId);
        // `except` may be null: `block_id <> NULL` is never true, so the null case is a separate query.
        String inBlocks = exceptBlockId == null
                ? "SELECT question_version_id FROM lesson_block_questions WHERE question_version_id IN (:ids)"
                : "SELECT question_version_id FROM lesson_block_questions WHERE question_version_id IN (:ids) AND block_id <> :except";
        namedJdbc.query(inBlocks + " UNION SELECT question_version_id FROM section_questions WHERE question_version_id IN (:ids)",
                params, rs -> { owned.add(rs.getObject(1, UUID.class)); });
        return questionVersionIds.stream().filter(owned::contains).distinct().toList();
    }

    private List<String> exerciseBlockIssues(UUID lessonId) {
        Map<UUID, List<LessonBlockKind.QuestionSpec>> questionsByBlock = new LinkedHashMap<>();
        Map<UUID, List<AssetType>> assetsByQuestion = new HashMap<>();
        org.springframework.jdbc.core.RowCallbackHandler assetHandler = rs -> {
            assetsByQuestion.computeIfAbsent(rs.getObject(1, UUID.class), ignored -> new ArrayList<>())
                    .add(AssetType.valueOf(rs.getString(2)));
        };
        jdbc.query("""
                SELECT l.question_version_id, a.asset_type
                FROM content_asset_links l JOIN content_assets a ON a.id = l.asset_id
                JOIN lesson_block_questions lbq ON lbq.question_version_id = l.question_version_id
                JOIN lesson_blocks b ON b.id = lbq.block_id
                WHERE b.lesson_id = ?
                """, assetHandler, lessonId);
        org.springframework.jdbc.core.RowCallbackHandler questionHandler = rs -> {
            UUID blockId = rs.getObject("block_id", UUID.class);
            questionsByBlock.computeIfAbsent(blockId, ignored -> new ArrayList<>());
            UUID questionVersionId = uuidOrNull(rs.getObject("question_version_id"));
            if (questionVersionId == null) return;
            questionsByBlock.get(blockId).add(new LessonBlockKind.QuestionSpec(
                    Objects.requireNonNullElse(rs.getString("answer_type"), ""), rs.getString("task"),
                    rs.getString("pass_band"), rs.getString("chart_facts"),
                    assetsByQuestion.getOrDefault(questionVersionId, List.of())));
        };
        jdbc.query("""
                SELECT b.id AS block_id, qv.id AS question_version_id, qv.answer_spec->>'type' AS answer_type,
                       qv.answer_spec->>'task' AS task, qv.answer_spec->>'passBand' AS pass_band,
                       qv.answer_spec->>'chartFacts' AS chart_facts
                FROM lesson_blocks b
                LEFT JOIN lesson_block_questions lbq ON lbq.block_id = b.id
                LEFT JOIN question_versions qv ON qv.id = lbq.question_version_id
                WHERE b.lesson_id = ? AND b.block_type = 'EXERCISE'
                ORDER BY b.id, lbq.sort_order
                """, questionHandler, lessonId);
        boolean invalid = false;
        for (List<LessonBlockKind.QuestionSpec> questions : questionsByBlock.values()) {
            if (questions.isEmpty()) {
                invalid = true;
                continue;
            }
            try {
                LessonBlockKind.classify(questions);
            } catch (InvalidLessonBlockException exception) {
                invalid = true;
            }
        }
        return invalid ? List.of("ESSAY_TASK_INVALID") : List.of();
    }

    private int count(String sql, UUID lessonId) {
        Integer value = jdbc.queryForObject(sql, Integer.class, lessonId);
        return value == null ? 0 : value;
    }

    private void saveKnowledgePoints(Lesson lesson) {
        List<UUID> pointIds = lesson.getKnowledgePointIds();
        ensureUnique(pointIds, "Knowledge point IDs must be unique");
        if (!pointIds.isEmpty()) {
            Integer activeCount = namedJdbc.queryForObject("""
                    SELECT COUNT(*) FROM knowledge_points WHERE id IN (:ids) AND status = 'ACTIVE'
                    """, new MapSqlParameterSource("ids", pointIds), Integer.class);
            if (activeCount == null || activeCount != pointIds.size()) {
                throw new LessonBlockInvalidException("All knowledge points must be active");
            }
        }
        jdbc.update("DELETE FROM lesson_knowledge_points WHERE lesson_id = ?", lesson.getId());
        for (UUID pointId : pointIds) {
            jdbc.update("INSERT INTO lesson_knowledge_points(lesson_id, knowledge_point_id) VALUES (?, ?)",
                    lesson.getId(), pointId);
        }
    }

    private void saveBlocks(Lesson lesson) {
        List<UUID> existingIds = jdbc.query("SELECT id FROM lesson_blocks WHERE lesson_id = ?",
                (rs, row) -> rs.getObject(1, UUID.class), lesson.getId());
        Set<UUID> desiredIds = lesson.getBlocks().stream().map(LessonBlock::getId).collect(java.util.stream.Collectors.toSet());
        for (UUID existingId : existingIds) {
            if (!desiredIds.contains(existingId)) jdbc.update("DELETE FROM lesson_blocks WHERE id = ?", existingId);
        }
        jdbc.update("UPDATE lesson_blocks SET sort_order = sort_order + 1000000 WHERE lesson_id = ?", lesson.getId());
        for (LessonBlock block : lesson.getBlocks()) saveBlock(block);
    }

    private void saveBlock(LessonBlock block) {
        validateBlockReferences(block);
        int updated;
        try {
            updated = jdbc.update("""
                UPDATE lesson_blocks SET sort_order = ?, block_type = ?, text_content = ?, asset_id = ?,
                    updated_at = CURRENT_TIMESTAMP WHERE id = ? AND lesson_id = ?
                """, block.getSortOrder(), block.getType().name(), block.getTextContent(), block.getAssetId(),
                block.getId(), block.getLessonId());
            if (updated == 0) {
                jdbc.update("""
                    INSERT INTO lesson_blocks(id, lesson_id, sort_order, block_type, text_content, asset_id)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, block.getId(), block.getLessonId(), block.getSortOrder(), block.getType().name(),
                        block.getTextContent(), block.getAssetId());
            }
        } catch (DataIntegrityViolationException exception) {
            if ("uq_lesson_blocks_sort_order".equals(constraintName(exception))) {
                throw new LessonSortConflictException();
            }
            throw exception;
        }
        jdbc.update("DELETE FROM lesson_block_vocabulary WHERE block_id = ?", block.getId());
        jdbc.update("DELETE FROM lesson_block_questions WHERE block_id = ?", block.getId());
        jdbc.update("DELETE FROM lesson_block_knowledge_points WHERE block_id = ?", block.getId());
        saveBlockChildren(block);
    }

    private void validateBlockReferences(LessonBlock block) {
        ensureUnique(block.getVocabularySenseIds(), "Vocabulary sense IDs must be unique");
        ensureUnique(block.getQuestionVersionIds(), "Question version IDs must be unique");
        ensureUnique(block.getKnowledgePointIds(), "Knowledge point IDs must be unique");
        if (block.getAssetId() != null && !exists("SELECT EXISTS(SELECT 1 FROM content_assets WHERE id = ?)", block.getAssetId())) {
            throw new LessonBlockInvalidException("Referenced asset does not exist");
        }
        if (!block.getQuestionVersionIds().isEmpty()) {
            Integer count = namedJdbc.queryForObject("SELECT COUNT(*) FROM question_versions WHERE id IN (:ids)",
                    new MapSqlParameterSource("ids", block.getQuestionVersionIds()), Integer.class);
            if (count == null || count != block.getQuestionVersionIds().size()) {
                throw new LessonBlockInvalidException("One or more question versions do not exist");
            }
        }
        if (!block.getKnowledgePointIds().isEmpty()) {
            Integer count = namedJdbc.queryForObject("SELECT COUNT(*) FROM knowledge_points WHERE id IN (:ids) AND status='ACTIVE'",
                    new MapSqlParameterSource("ids", block.getKnowledgePointIds()), Integer.class);
            if (count == null || count != block.getKnowledgePointIds().size()) {
                throw new LessonBlockInvalidException("All block knowledge points must be active");
            }
        }
    }

    private boolean exists(String sql, UUID id) { return Boolean.TRUE.equals(jdbc.queryForObject(sql, Boolean.class, id)); }

    private void saveBlockChildren(LessonBlock block) {
        for (int index = 0; index < block.getVocabularySenseIds().size(); index++) {
            jdbc.update("INSERT INTO lesson_block_vocabulary(block_id, vocabulary_sense_id, sort_order) VALUES (?, ?, ?)",
                    block.getId(), block.getVocabularySenseIds().get(index), index);
        }
        for (int index = 0; index < block.getQuestionVersionIds().size(); index++) {
            jdbc.update("INSERT INTO lesson_block_questions(block_id, question_version_id, sort_order) VALUES (?, ?, ?)",
                    block.getId(), block.getQuestionVersionIds().get(index), index);
        }
        for (UUID knowledgePointId : block.getKnowledgePointIds()) {
            jdbc.update("INSERT INTO lesson_block_knowledge_points(block_id, knowledge_point_id) VALUES (?, ?)",
                    block.getId(), knowledgePointId);
        }
    }

    private Lesson loadAggregate(UUID lessonId) {
        LessonAuthoringSnapshot snapshot = findSnapshotById(lessonId)
                .orElseThrow(() -> new com.ieltspath.content.domain.exception.LessonNotFoundException(lessonId));
        List<LessonBlock> blocks = snapshot.blocks().stream().map(block -> new LessonBlock(block.id(), lessonId,
                block.blockType(), block.sortOrder(), block.textContent(), block.assetId(), block.vocabularySenseIds(),
                block.questions().stream().map(LessonAuthoringSnapshot.QuestionVersion::id).toList(),
                block.knowledgePointIds())).toList();
        return new Lesson(snapshot.id(), snapshot.topicId(), snapshot.code(), snapshot.title(), snapshot.summary(),
                snapshot.summaryMarkdown(), snapshot.estimatedMinutes(), snapshot.sortOrder(), snapshot.status(),
                snapshot.updatedAt(), blocks, snapshot.knowledgePointIds());
    }

    private List<LessonAuthoringSnapshot> loadSnapshots(List<LessonRow> lessons) {
        if (lessons.isEmpty()) return List.of();
        List<UUID> lessonIds = lessons.stream().map(LessonRow::id).toList();
        Map<UUID, List<UUID>> lessonPoints = groupedIds("""
                SELECT lesson_id AS owner_id, knowledge_point_id AS value_id FROM lesson_knowledge_points
                WHERE lesson_id IN (:ids) ORDER BY knowledge_point_id
                """, lessonIds);
        List<BlockRow> blocks = namedJdbc.query("""
                SELECT id, lesson_id, sort_order, block_type, text_content, asset_id
                FROM lesson_blocks WHERE lesson_id IN (:ids) ORDER BY lesson_id, sort_order
                """, new MapSqlParameterSource("ids", lessonIds), (rs, row) -> new BlockRow(
                rs.getObject("id", UUID.class), rs.getObject("lesson_id", UUID.class), rs.getInt("sort_order"),
                BlockType.valueOf(rs.getString("block_type")), rs.getString("text_content"),
                uuidOrNull(rs.getObject("asset_id"))));
        List<UUID> blockIds = blocks.stream().map(BlockRow::id).toList();
        Map<UUID, List<UUID>> vocabularies = blockIds.isEmpty() ? Map.of() : groupedIds("""
                SELECT block_id AS owner_id, vocabulary_sense_id AS value_id FROM lesson_block_vocabulary
                WHERE block_id IN (:ids) ORDER BY sort_order
                """, blockIds);
        Map<UUID, List<UUID>> blockPoints = blockIds.isEmpty() ? Map.of() : groupedIds("""
                SELECT block_id AS owner_id, knowledge_point_id AS value_id FROM lesson_block_knowledge_points
                WHERE block_id IN (:ids) ORDER BY knowledge_point_id
                """, blockIds);
        Map<UUID, List<LessonAuthoringSnapshot.QuestionVersion>> questions = loadQuestionVersions(blockIds);
        Map<UUID, List<LessonAuthoringSnapshot.Block>> blocksByLesson = new HashMap<>();
        for (BlockRow block : blocks) {
            var result = new LessonAuthoringSnapshot.Block(block.id(), block.sortOrder(), block.type(), block.text(),
                    block.assetId(), vocabularies.getOrDefault(block.id(), List.of()),
                    blockPoints.getOrDefault(block.id(), List.of()), questions.getOrDefault(block.id(), List.of()));
            blocksByLesson.computeIfAbsent(block.lessonId(), ignored -> new ArrayList<>()).add(result);
        }
        return lessons.stream().map(lesson -> new LessonAuthoringSnapshot(lesson.id(), lesson.topicId(), lesson.code(),
                lesson.title(), lesson.summary(), lesson.summaryMarkdown(), lesson.estimatedMinutes(), lesson.sortOrder(),
                lesson.status(), lesson.createdAt(), lesson.updatedAt(), lessonPoints.getOrDefault(lesson.id(), List.of()),
                blocksByLesson.getOrDefault(lesson.id(), List.of()))).toList();
    }

    private Map<UUID, List<LessonAuthoringSnapshot.QuestionVersion>> loadQuestionVersions(List<UUID> blockIds) {
        if (blockIds.isEmpty()) return Map.of();
        Map<UUID, List<LessonAuthoringSnapshot.QuestionVersion>> result = new HashMap<>();
        org.springframework.jdbc.core.RowCallbackHandler questionVersionHandler = rs -> {
            UUID blockId = rs.getObject("block_id", UUID.class);
            result.computeIfAbsent(blockId, ignored -> new ArrayList<>()).add(new LessonAuthoringSnapshot.QuestionVersion(
                    rs.getObject("id", UUID.class), rs.getInt("version_number"),
                    QuestionType.valueOf(rs.getString("question_type")), enumOrNull(Skill.class, rs.getString("skill")),
                    enumOrNull(QuestionPurpose.class, rs.getString("purpose")),
                    PublicationStatus.valueOf(rs.getString("status")), rs.getString("stem"), rs.getString("answer_spec")));
        };
        namedJdbc.query("""
                SELECT lbq.block_id, qv.id, qv.version_number, q.question_type, q.skill, q.purpose,
                       qv.status, qv.stem, qv.answer_spec::text
                FROM lesson_block_questions lbq
                JOIN question_versions qv ON qv.id = lbq.question_version_id
                JOIN questions q ON q.id = qv.question_id
                WHERE lbq.block_id IN (:ids) ORDER BY lbq.block_id, lbq.sort_order
                """, new MapSqlParameterSource("ids", blockIds), questionVersionHandler);
        return result;
    }

    private Map<UUID, List<UUID>> groupedIds(String sql, List<UUID> ids) {
        Map<UUID, List<UUID>> result = new HashMap<>();
        org.springframework.jdbc.core.RowCallbackHandler handler = rs -> {
            UUID ownerId = rs.getObject("owner_id", UUID.class);
            result.computeIfAbsent(ownerId, ignored -> new ArrayList<>()).add(rs.getObject("value_id", UUID.class));
        };
        namedJdbc.query(sql, new MapSqlParameterSource("ids", ids), handler);
        return result;
    }

    private void translateLessonConstraint(DataIntegrityViolationException exception, String code) {
        String constraint = constraintName(exception);
        if ("lessons_code_key".equals(constraint)) throw new DuplicateCodeException("Lesson", code);
        if ("uq_lessons_topic_sort_order".equals(constraint)) throw new LessonSortConflictException();
        throw exception;
    }

    private static String constraintName(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof org.postgresql.util.PSQLException postgresException
                    && postgresException.getServerErrorMessage() != null) {
                return postgresException.getServerErrorMessage().getConstraint();
            }
            cause = cause.getCause();
        }
        return null;
    }

    private static void ensureUnique(List<UUID> ids, String message) {
        if (ids.stream().distinct().count() != ids.size()) throw new LessonBlockInvalidException(message);
    }

    private static void validateOrder(List<UUID> currentIds, List<UUID> desiredIds) {
        if (currentIds.size() != desiredIds.size() || desiredIds.stream().distinct().count() != desiredIds.size()
                || !new HashSet<>(currentIds).equals(new HashSet<>(desiredIds))) {
            throw new LessonSortConflictException();
        }
    }

    private RowMapper<LessonRow> lessonRowMapper() {
        return (rs, row) -> new LessonRow(rs.getObject("id", UUID.class), rs.getObject("topic_id", UUID.class),
                rs.getString("code"), rs.getString("title"), rs.getString("summary"),
                rs.getString("summary_markdown"), integerOrNull(rs.getObject("estimated_minutes")),
                rs.getInt("sort_order"), LessonStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private static UUID uuidOrNull(Object value) { return value == null ? null : (UUID) value; }
    private static Integer integerOrNull(Object value) { return value == null ? null : ((Number) value).intValue(); }
    private static <T extends Enum<T>> T enumOrNull(Class<T> enumType, String value) {
        return value == null ? null : Enum.valueOf(enumType, value);
    }

    private record LessonRow(UUID id, UUID topicId, String code, String title, String summary, String summaryMarkdown,
                             Integer estimatedMinutes, int sortOrder, LessonStatus status,
                             java.time.Instant createdAt, java.time.Instant updatedAt) {}
    private record BlockRow(UUID id, UUID lessonId, int sortOrder, BlockType type, String text, UUID assetId) {}
}
