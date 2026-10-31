package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.LearningContentReader;
import com.ieltspath.content.application.result.LessonContentResult;
import com.ieltspath.content.application.result.LessonPracticeSetResult;
import com.ieltspath.content.application.result.LessonPracticeSetsResult;
import com.ieltspath.content.application.result.LessonSummaryResult;
import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.application.result.PracticeSetResult;
import com.ieltspath.content.application.result.TopicSequenceResult;
import com.ieltspath.content.application.result.TopicTestPackageResult;
import com.ieltspath.content.application.result.PackageQuestionSpec;
import com.ieltspath.content.application.result.KnowledgeTreeNodeResult;
import com.ieltspath.content.domain.vo.QuestionType;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.LessonBlockKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.QuestionUsageConflict;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.QuestionDifficulty;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.CefrLevel;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Plain SQL projections of the curriculum. Collections are loaded per request with {@code IN} lists and joined in
 * memory by id, so no method issues one query per row.
 */
@Component
public class JdbcLearningContentReader implements LearningContentReader {
    private static final String LESSON_SKILLS = """
            SELECT q.skill FROM lesson_blocks b
            JOIN lesson_block_questions bq ON bq.block_id = b.id
            JOIN question_versions qv ON qv.id = bq.question_version_id
            JOIN questions q ON q.id = qv.question_id
            WHERE b.lesson_id = l.id AND b.block_type = 'EXERCISE' AND q.skill <> 'ALL'
            UNION
            SELECT kp.skill FROM lesson_blocks b
            JOIN lesson_block_knowledge_points bk ON bk.block_id = b.id
            JOIN knowledge_points kp ON kp.id = bk.knowledge_point_id
            WHERE b.lesson_id = l.id AND b.block_type = 'TEXT' AND kp.skill <> 'ALL'
            """;

    private static final String RANKED_COURSE_LESSONS = """
            SELECT l.id, l.topic_id, t.course_id, t.skill,
                   row_number() OVER (PARTITION BY t.course_id, t.skill
                       ORDER BY t.sort_order, t.id, l.sort_order, l.id) AS skill_position
            FROM lessons l JOIN topics t ON t.id = l.topic_id
            WHERE t.course_id IS NOT NULL AND t.status = 'ACTIVE' AND l.status = 'PUBLISHED'
            """;

    private static final String PRACTICE_SKILLS = """
            SELECT DISTINCT q.skill FROM content_sections s
            JOIN section_questions sq ON sq.section_id = s.id
            JOIN question_versions qv ON qv.id = sq.question_version_id
            JOIN questions q ON q.id = qv.question_id
            WHERE s.package_version_id = p.current_published_version_id AND q.skill <> 'ALL'
            """;

    /** Question versions used by a lesson, final test, mock test or placement test; review must not repeat them. */
    private static final String LESSON_OR_TEST_QUESTION_VERSIONS = """
            SELECT bq.question_version_id FROM lesson_block_questions bq
            UNION
            SELECT tsq.question_version_id
            FROM section_questions tsq
            JOIN content_sections ts ON ts.id = tsq.section_id
            JOIN content_package_versions tv ON tv.id = ts.package_version_id
            JOIN content_packages tp ON tp.id = tv.package_id
            WHERE tp.package_type IN ('TOPIC_TEST', 'COURSE_TEST', 'MOCK_TEST', 'PLACEMENT_TEST', 'PRACTICE_TEST')
            """;

    /**
     * Eligibility of package {@code p} as review material for a knowledge point. The placeholder is the knowledge
     * point id expression; {@code :minQuestions} is bound by the caller.
     */
    private static final String ELIGIBLE_PRACTICE_SET = """
            p.package_type = 'PRACTICE_SET'
            AND p.status = 'PUBLISHED'
            AND p.current_published_version_id IS NOT NULL
            AND (SELECT count(*) FROM content_sections s JOIN section_questions sq ON sq.section_id = s.id
                 WHERE s.package_version_id = p.current_published_version_id) >= :minQuestions
            AND EXISTS (SELECT 1 FROM content_sections s
                        JOIN section_questions sq ON sq.section_id = s.id
                        JOIN question_knowledge_points qkp ON qkp.question_version_id = sq.question_version_id
                        WHERE s.package_version_id = p.current_published_version_id
                          AND qkp.knowledge_point_id = %s)
            AND NOT EXISTS (SELECT 1 FROM content_sections s
                            JOIN section_questions sq ON sq.section_id = s.id
                            WHERE s.package_version_id = p.current_published_version_id
                              AND sq.question_version_id IN (%s))
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final boolean hasTopicDescription;
    private final boolean hasCourseCoverImageUrl;
    private final boolean hasCourseDescription;
    private final boolean hasComboUnlockPoints;
    private final boolean hasCourseSkillPrices;

    public JdbcLearningContentReader(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.hasTopicDescription = hasColumn("topics", "description");
        this.hasCourseCoverImageUrl = hasColumn("courses", "cover_image_url");
        this.hasCourseDescription = hasColumn("courses", "description");
        this.hasComboUnlockPoints = hasColumn("courses", "combo_unlock_points");
        this.hasCourseSkillPrices = hasTable("course_skill_prices");
    }

    private static String eligiblePracticeSet(String knowledgePointExpression) {
        return ELIGIBLE_PRACTICE_SET.formatted(knowledgePointExpression, LESSON_OR_TEST_QUESTION_VERSIONS);
    }

    @Override
    public com.ieltspath.content.application.result.ProgressMetadataResult progressMetadata(Collection<UUID> courseIds) {
        if (courseIds.isEmpty()) return new com.ieltspath.content.application.result.ProgressMetadataResult("empty", true, List.of());
        var rows = jdbc.query("""
                SELECT c.id AS course_id, c.name AS course_name, c.band_level, t.id AS topic_id,
                       l.id AS lesson_id, ARRAY(SELECT skill FROM (%s) ls ORDER BY skill) AS skills
                FROM courses c JOIN topics t ON t.course_id = c.id
                JOIN lessons l ON l.topic_id = t.id
                WHERE c.id IN (:courseIds) AND c.status = 'ACTIVE' AND t.status = 'ACTIVE'
                  AND l.status = 'PUBLISHED'
                ORDER BY c.id, t.sort_order, t.id, l.sort_order, l.id
                LIMIT 10001
                """.formatted(LESSON_SKILLS), Map.of("courseIds", courseIds), (rs, i) ->
                new com.ieltspath.content.application.result.ProgressMetadataResult.Lesson(
                        uuid(rs, "course_id"), rs.getString("course_name"), rs.getBigDecimal("band_level"),
                        uuid(rs, "topic_id"), uuid(rs, "lesson_id"), skills(rs)));
        // Hash the denominator and skill membership, including removals; timestamps alone miss deletions.
        String revision = UUID.nameUUIDFromBytes(rows.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        boolean complete = rows.size() <= 10000;
        return new com.ieltspath.content.application.result.ProgressMetadataResult(revision, complete,
                complete ? rows : List.of());
    }

    @Override
    public List<TopicSequenceResult> topicSequence(int minPracticeQuestions) {
        return topicSequence(minPracticeQuestions, false);
    }

    @Override
    public List<TopicSequenceResult> topicSequence(int minPracticeQuestions, boolean includeErrorTraps) {
        record TopicRow(UUID id, String code, String name, int sortOrder, String requiredFeatureKey, Skill skill,
                        boolean hasTopicTest, TopicSequenceResult.CourseEntry course, List<Skill> skills,
                        int lessonCount, String description) {}
        // Course metadata is read with the topic; topics outside an active course do not join the curriculum.
        String topicDescription = hasTopicDescription ? "t.description" : "NULL::text AS description";
        String coverImageUrl = hasCourseCoverImageUrl ? "c.cover_image_url" : "NULL::varchar AS cover_image_url";
        String courseDescription = hasCourseDescription ? "c.description AS course_description"
                : "NULL::text AS course_description";
        String comboUnlockPoints = hasComboUnlockPoints ? "c.combo_unlock_points"
                : "NULL::integer AS combo_unlock_points";
        List<TopicRow> topics = jdbc.query("""
                SELECT t.id, t.code, t.name, %s, t.sort_order, t.required_feature_key, t.skill,
                       ARRAY(SELECT DISTINCT ls.skill FROM lessons l
                             CROSS JOIN LATERAL (%s) ls
                             WHERE l.topic_id = t.id AND l.status = 'PUBLISHED') AS skills,
                       EXISTS (SELECT 1 FROM content_packages p
                               WHERE p.topic_id = t.id AND p.package_type = 'TOPIC_TEST'
                                 AND p.status = 'PUBLISHED' AND p.current_published_version_id IS NOT NULL)
                           AS has_topic_test,
                       (SELECT count(*) FROM lessons l
                        WHERE l.topic_id = t.id AND l.status = 'PUBLISHED') AS lesson_count,
                       c.id AS course_id, c.code AS course_code, c.name AS course_name, c.band_level,
                       %s, %s, %s,
                       ARRAY(SELECT DISTINCT t2.skill FROM topics t2
                             WHERE t2.course_id = c.id AND t2.status = 'ACTIVE' AND t2.skill IS NOT NULL
                               AND EXISTS (SELECT 1 FROM lessons l2 WHERE l2.topic_id = t2.id AND l2.status = 'PUBLISHED')
                             ORDER BY t2.skill) AS available_skills,
                       EXISTS (SELECT 1 FROM content_packages p
                               WHERE p.course_id = c.id AND p.package_type = 'COURSE_TEST'
                                 AND p.status = 'PUBLISHED' AND p.current_published_version_id IS NOT NULL)
                           AS has_course_test
                FROM topics t
                JOIN courses c ON c.id = t.course_id
                WHERE t.status = 'ACTIVE' AND c.status = 'ACTIVE'
                  AND EXISTS (SELECT 1 FROM lessons l WHERE l.topic_id = t.id AND l.status = 'PUBLISHED')
                ORDER BY t.skill, c.band_level, t.sort_order, t.id
                """.formatted(topicDescription, LESSON_SKILLS, coverImageUrl, courseDescription, comboUnlockPoints),
                Map.of(), (rs, i) -> new TopicRow(uuid(rs, "id"), rs.getString("code"), rs.getString("name"),
                rs.getInt("sort_order"), rs.getString("required_feature_key"),
                enumOrNull(Skill.class, rs.getString("skill")), rs.getBoolean("has_topic_test"),
                new TopicSequenceResult.CourseEntry(uuid(rs, "course_id"), rs.getString("course_code"),
                        rs.getString("course_name"), rs.getBigDecimal("band_level"), rs.getBoolean("has_course_test"),
                        true, rs.getString("cover_image_url"), rs.getString("course_description"), Map.of(),
                        (Integer) rs.getObject("combo_unlock_points"), skills(rs, "available_skills")), skills(rs),
                rs.getInt("lesson_count"), rs.getString("description")));
        if (topics.isEmpty()) {
            return List.of();
        }

        Map<UUID, Map<Skill, Integer>> pricesByCourse = new LinkedHashMap<>();
        List<UUID> courseIds = topics.stream().map(row -> row.course().courseId()).distinct().toList();
        if (hasCourseSkillPrices) {
            jdbc.query("SELECT course_id, skill, unlock_points FROM course_skill_prices WHERE course_id IN (:courseIds)",
                    Map.of("courseIds", courseIds), (org.springframework.jdbc.core.RowCallbackHandler) rs ->
                            pricesByCourse.computeIfAbsent(uuid(rs, "course_id"), ignored -> new LinkedHashMap<>())
                                    .put(Skill.valueOf(rs.getString("skill")), rs.getInt("unlock_points")));
        }

        Map<UUID, List<TopicSequenceResult.KnowledgePointEntry>> pointsByTopic = new LinkedHashMap<>();
        String trapJoin = includeErrorTraps
                ? "LEFT JOIN error_types trap ON trap.trap_knowledge_point_id = kp.id" : "";
        String trapColumns = includeErrorTraps
                ? ", trap.knowledge_point_id AS parent_knowledge_point_id, trap.code AS error_code," : ",";
        String trapFilter = includeErrorTraps || !hasTrapKnowledgePointColumn() ? ""
                : "AND NOT EXISTS (SELECT 1 FROM error_types trap_filter WHERE trap_filter.trap_knowledge_point_id = kp.id)";
        String practiceSetProjection = includeErrorTraps
                ? "CASE WHEN trap.id IS NOT NULL THEN FALSE ELSE EXISTS (SELECT 1 FROM content_packages p WHERE %s) END"
                    .formatted(eligiblePracticeSet("kp.id"))
                : "EXISTS (SELECT 1 FROM content_packages p WHERE %s)".formatted(eligiblePracticeSet("kp.id"));
        jdbc.query(("""
                WITH ranked_lessons AS (%s), point_positions AS (
                    SELECT l.topic_id, lkp.knowledge_point_id, MIN(rl.skill_position) AS earliest_lesson_position
                    FROM lesson_knowledge_points lkp
                    JOIN lessons l ON l.id = lkp.lesson_id
                    JOIN ranked_lessons rl ON rl.id = l.id
                    GROUP BY l.topic_id, lkp.knowledge_point_id
                )
                SELECT kp.id, kp.topic_id, kp.code, kp.name, kp.learning_type, kp.skill, kp.description,
                       kp.cefr_level, kp.can_do, kp.scope_in, kp.scope_out %s
                       %s AS has_practice_set, point_positions.earliest_lesson_position
                FROM knowledge_points kp
                %s
                LEFT JOIN point_positions ON point_positions.topic_id = kp.topic_id
                                          AND point_positions.knowledge_point_id = kp.id
                WHERE kp.status = 'ACTIVE' AND kp.topic_id IN (:topicIds)
                  %s
                ORDER BY kp.created_at, kp.id
                """).formatted(RANKED_COURSE_LESSONS, trapColumns, practiceSetProjection, trapJoin, trapFilter),
                new MapSqlParameterSource()
                        .addValue("topicIds", topics.stream().map(TopicRow::id).toList())
                        .addValue("minQuestions", minPracticeQuestions),
                rs -> {
                    UUID topicId = uuid(rs, "topic_id");
                    pointsByTopic.computeIfAbsent(topicId, ignored -> new ArrayList<>())
                            .add(new TopicSequenceResult.KnowledgePointEntry(
                                    uuid(rs, "id"), topicId, rs.getString("code"), rs.getString("name"),
                                    enumOrNull(LearningType.class, rs.getString("learning_type")),
                                    enumOrNull(Skill.class, rs.getString("skill")),
                                    rs.getString("description"), rs.getBoolean("has_practice_set"),
                                    enumOrNull(CefrLevel.class, rs.getString("cefr_level")),
                                    rs.getString("can_do"), rs.getString("scope_in"), rs.getString("scope_out"),
                                    includeErrorTraps ? uuid(rs, "parent_knowledge_point_id") : null,
                                    includeErrorTraps ? rs.getString("error_code") : null,
                                    nullableInt(rs, "earliest_lesson_position")));
                });

        return topics.stream()
                .map(t -> new TopicSequenceResult(t.id(), t.code(), t.name(), t.sortOrder(), t.requiredFeatureKey(),
                        pointsByTopic.getOrDefault(t.id(), List.of()), t.skill(), t.hasTopicTest(),
                        new TopicSequenceResult.CourseEntry(t.course().courseId(), t.course().code(), t.course().name(),
                                t.course().bandLevel(), t.course().hasCourseTest(), t.course().active(),
                                t.course().coverImageUrl(), t.course().description(),
                                pricesByCourse.getOrDefault(t.course().courseId(), Map.of()),
                                t.course().comboUnlockPoints(), t.course().availableSkills()), t.skills(),
                        true, t.lessonCount(), t.description()))
                .toList();
    }

    private boolean hasTrapKnowledgePointColumn() {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_schema = current_schema()
                      AND table_name = 'error_types'
                      AND column_name = 'trap_knowledge_point_id'
                )
                """, Map.of(), Boolean.class));
    }

    private boolean hasColumn(String table, String column) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM information_schema.columns
                               WHERE table_schema = current_schema() AND table_name = :table AND column_name = :column)
                """, Map.of("table", table, "column", column), Boolean.class));
    }

    private boolean hasTable(String table) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT to_regclass(:table) IS NOT NULL
                """, Map.of("table", table), Boolean.class));
    }

    @Override
    public boolean activeTopicExists(UUID topicId) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM topics WHERE id = :id AND status = 'ACTIVE')",
                Map.of("id", topicId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public List<LessonSummaryResult> publishedLessons(UUID topicId) {
        record LessonRow(UUID id, String code, String title, String summary, int sortOrder, List<Skill> skills,
                         Integer estimatedMinutes, UUID courseId, Skill skill, Integer skillPosition) {}
        List<LessonRow> lessons = jdbc.query("""
                WITH ranked_lessons AS (%s)
                SELECT l.id, l.code, l.title, l.summary, l.sort_order, l.estimated_minutes, ARRAY(%s) AS skills,
                       t.course_id, t.skill AS topic_skill, ranked_lessons.skill_position
                FROM lessons l
                JOIN topics t ON t.id = l.topic_id
                LEFT JOIN ranked_lessons ON ranked_lessons.id = l.id
                WHERE l.topic_id = :topicId AND l.status = 'PUBLISHED'
                ORDER BY l.sort_order
                """.formatted(RANKED_COURSE_LESSONS, LESSON_SKILLS), Map.of("topicId", topicId), (rs, i) -> new LessonRow(uuid(rs, "id"), rs.getString("code"),
                rs.getString("title"), rs.getString("summary"), rs.getInt("sort_order"), skills(rs),
                (Integer) rs.getObject("estimated_minutes"), uuid(rs, "course_id"),
                enumOrNull(Skill.class, rs.getString("topic_skill")), nullableInt(rs, "skill_position")));
        if (lessons.isEmpty()) {
            return List.of();
        }
        List<UUID> lessonIds = lessons.stream().map(LessonRow::id).toList();
        Map<UUID, List<UUID>> knowledgePoints = lessonKnowledgePoints(lessonIds);

        Map<UUID, List<UUID>> exerciseBlocks = new LinkedHashMap<>();
        jdbc.query("""
                SELECT b.lesson_id, b.id FROM lesson_blocks b
                WHERE b.lesson_id IN (:lessonIds) AND b.block_type = 'EXERCISE'
                ORDER BY b.lesson_id, b.sort_order
                """, Map.of("lessonIds", lessonIds), rs -> {
            exerciseBlocks.computeIfAbsent(uuid(rs, "lesson_id"), ignored -> new ArrayList<>()).add(uuid(rs, "id"));
        });

        return lessons.stream()
                .map(l -> new LessonSummaryResult(l.id(), topicId, l.code(), l.title(), l.summary(), l.sortOrder(),
                        knowledgePoints.getOrDefault(l.id(), List.of()),
                        exerciseBlocks.getOrDefault(l.id(), List.of()), l.skills(), l.estimatedMinutes(), l.courseId(),
                        l.skill(), l.skillPosition()))
                .toList();
    }

    @Override
    public Optional<LessonContentResult> publishedLesson(UUID lessonId) {
        record LessonRow(UUID topicId, String code, String title, String summary, int sortOrder, List<Skill> skills,
                         UUID courseId, Skill skill, Integer skillPosition) {}
        List<LessonRow> found = jdbc.query("""
                WITH ranked_lessons AS (%s)
                SELECT l.topic_id, l.code, l.title, l.summary, l.sort_order, ARRAY(%s) AS skills,
                       t.course_id, t.skill AS topic_skill, ranked_lessons.skill_position
                FROM lessons l
                JOIN topics t ON t.id = l.topic_id
                LEFT JOIN ranked_lessons ON ranked_lessons.id = l.id
                WHERE l.id = :id AND l.status = 'PUBLISHED'
                """.formatted(RANKED_COURSE_LESSONS, LESSON_SKILLS), Map.of("id", lessonId), (rs, i) -> new LessonRow(uuid(rs, "topic_id"), rs.getString("code"),
                rs.getString("title"), rs.getString("summary"), rs.getInt("sort_order"),
                skills(rs), uuid(rs, "course_id"), enumOrNull(Skill.class, rs.getString("topic_skill")),
                nullableInt(rs, "skill_position")));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        LessonRow lesson = found.get(0);
        Map<String, Object> byLesson = Map.of("lessonId", lessonId);

        Map<UUID, List<UUID>> questionPoints = new LinkedHashMap<>();
        jdbc.query("""
                SELECT DISTINCT qkp.question_version_id, qkp.knowledge_point_id
                FROM question_knowledge_points qkp
                JOIN lesson_block_questions bq ON bq.question_version_id = qkp.question_version_id
                JOIN lesson_blocks b ON b.id = bq.block_id
                WHERE b.lesson_id = :lessonId
                ORDER BY qkp.question_version_id, qkp.knowledge_point_id
                """, byLesson, rs -> {
            questionPoints.computeIfAbsent(uuid(rs, "question_version_id"), ignored -> new ArrayList<>())
                    .add(uuid(rs, "knowledge_point_id"));
        });

        Map<UUID, List<LessonContentResult.QuestionAsset>> questionAssets = new LinkedHashMap<>();
        jdbc.query("""
                SELECT l.question_version_id, a.id, a.asset_type, a.media_reference, a.text_content, l.sort_order
                FROM content_asset_links l
                JOIN content_assets a ON a.id = l.asset_id
                JOIN lesson_block_questions bq ON bq.question_version_id = l.question_version_id
                JOIN lesson_blocks b ON b.id = bq.block_id
                WHERE b.lesson_id = :lessonId
                ORDER BY l.question_version_id, l.sort_order, a.id
                """, byLesson, rs -> {
            questionAssets.computeIfAbsent(uuid(rs, "question_version_id"), ignored -> new ArrayList<>())
                    .add(new LessonContentResult.QuestionAsset(uuid(rs, "id"),
                            AssetType.valueOf(rs.getString("asset_type")), rs.getString("media_reference"), null,
                            rs.getString("text_content"), rs.getInt("sort_order")));
        });

        Map<UUID, List<LessonContentResult.Question>> questionsByBlock = new LinkedHashMap<>();
        jdbc.query("""
                SELECT bq.block_id, bq.sort_order, qv.id, qv.stem, qv.options::text AS options,
                       qv.answer_spec::text AS answer_spec, qv.explanation, qv.hint,
                       qv.answer_spec->>'type' AS spec_type, qv.answer_spec->>'task' AS spec_task,
                       qv.answer_spec->>'passBand' AS spec_pass_band, qv.answer_spec->>'chartFacts' AS spec_chart_facts
                FROM lesson_block_questions bq
                JOIN lesson_blocks b ON b.id = bq.block_id
                JOIN question_versions qv ON qv.id = bq.question_version_id
                WHERE b.lesson_id = :lessonId AND qv.status = 'PUBLISHED'
                ORDER BY bq.block_id, bq.sort_order
                """, byLesson, rs -> {
            UUID versionId = uuid(rs, "id");
            List<LessonContentResult.QuestionAsset> assets = questionAssets.getOrDefault(versionId, List.of());
            questionsByBlock.computeIfAbsent(uuid(rs, "block_id"), ignored -> new ArrayList<>())
                    .add(new LessonContentResult.Question(versionId, rs.getInt("sort_order"), rs.getString("stem"),
                            rs.getString("options"), rs.getString("answer_spec"), rs.getString("explanation"),
                            rs.getString("hint"), questionPoints.getOrDefault(versionId, List.of()),
                            new LessonBlockKind.QuestionSpec(rs.getString("spec_type"), rs.getString("spec_task"),
                                    rs.getString("spec_pass_band"), rs.getString("spec_chart_facts"),
                                    assets.stream().map(LessonContentResult.QuestionAsset::assetType).toList()),
                            assets));
        });

        Map<UUID, List<UUID>> sensesByBlock = new LinkedHashMap<>();
        jdbc.query("""
                SELECT v.block_id, v.vocabulary_sense_id FROM lesson_block_vocabulary v
                JOIN lesson_blocks b ON b.id = v.block_id
                WHERE b.lesson_id = :lessonId
                ORDER BY v.block_id, v.sort_order
                """, byLesson, rs -> {
            sensesByBlock.computeIfAbsent(uuid(rs, "block_id"), ignored -> new ArrayList<>())
                    .add(uuid(rs, "vocabulary_sense_id"));
        });

        Map<UUID, List<UUID>> pointsByTextBlock = new LinkedHashMap<>();
        jdbc.query("""
                SELECT bk.block_id, bk.knowledge_point_id
                FROM lesson_block_knowledge_points bk
                JOIN lesson_blocks b ON b.id = bk.block_id
                JOIN knowledge_points kp ON kp.id = bk.knowledge_point_id
                WHERE b.lesson_id = :lessonId
                ORDER BY bk.block_id, kp.created_at, kp.id
                """, byLesson, rs -> {
            pointsByTextBlock.computeIfAbsent(uuid(rs, "block_id"), ignored -> new ArrayList<>())
                    .add(uuid(rs, "knowledge_point_id"));
        });

        List<LessonContentResult.Block> blocks = jdbc.query("""
                SELECT b.id, b.block_type, b.sort_order, b.text_content, a.id AS asset_id, a.asset_type,
                       a.text_content AS asset_text, a.media_reference, a.duration_seconds
                FROM lesson_blocks b
                LEFT JOIN content_assets a ON a.id = b.asset_id
                WHERE b.lesson_id = :lessonId
                ORDER BY b.sort_order
                """, byLesson, (rs, i) -> {
            UUID blockId = uuid(rs, "id");
            BlockType type = BlockType.valueOf(rs.getString("block_type"));
            LessonContentResult.Asset asset = type == BlockType.ASSET
                    ? new LessonContentResult.Asset(uuid(rs, "asset_id"),
                    AssetType.valueOf(rs.getString("asset_type")), rs.getString("asset_text"),
                    rs.getString("media_reference"), (Integer) rs.getObject("duration_seconds"), null)
                    : null;
            List<LessonContentResult.Question> questions =
                    type == BlockType.EXERCISE ? questionsByBlock.getOrDefault(blockId, List.of()) : null;
            List<UUID> blockPoints = switch (type) {
                case TEXT -> pointsByTextBlock.getOrDefault(blockId, List.of());
                case EXERCISE -> questions.stream()
                        .flatMap(question -> question.knowledgePointIds().stream()).distinct().toList();
                default -> List.of();
            };
            return new LessonContentResult.Block(blockId, type, null, rs.getInt("sort_order"),
                    type == BlockType.TEXT ? rs.getString("text_content") : null,
                    asset,
                    type == BlockType.VOCABULARY ? sensesByBlock.getOrDefault(blockId, List.of()) : null,
                    questions, blockPoints);
        });

        return Optional.of(new LessonContentResult(lessonId, lesson.topicId(), lesson.code(), lesson.title(),
                lesson.summary(), lesson.sortOrder(),
                lessonKnowledgePoints(List.of(lessonId)).getOrDefault(lessonId, List.of()), blocks, lesson.skill(),
                lesson.skills(), lesson.courseId(), lesson.skillPosition()));
    }

    @Override
    public boolean publishedLessonExists(UUID lessonId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM lessons WHERE id = :id AND status = 'PUBLISHED')",
                Map.of("id", lessonId), Boolean.class));
    }

    @Override
    public boolean lessonExists(UUID lessonId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM lessons WHERE id = :id)",
                Map.of("id", lessonId), Boolean.class));
    }

    @Override
    public boolean topicExists(UUID topicId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM topics WHERE id = :id)",
                Map.of("id", topicId), Boolean.class));
    }

    @Override
    public boolean courseExists(UUID courseId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM courses WHERE id = :id)",
                Map.of("id", courseId), Boolean.class));
    }

    @Override
    public List<LessonPracticeSetResult> lessonPracticeSets(UUID lessonId) {
        return lessonPracticeSets(lessonId, Optional.empty());
    }

    @Override
    public List<LessonPracticeSetResult> lessonPracticeSets(UUID lessonId, Optional<Skill> skill) {
        return practiceSetsOf(List.of(lessonId), skill).getOrDefault(lessonId, List.of());
    }

    @Override
    public List<LessonPracticeSetsResult> topicPracticeSets(UUID topicId) {
        List<UUID> lessonIds = jdbc.queryForList("""
                SELECT id FROM lessons WHERE topic_id = :topicId AND status = 'PUBLISHED' ORDER BY sort_order
                """, Map.of("topicId", topicId), UUID.class);
        if (lessonIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<LessonPracticeSetResult>> sets = practiceSetsOf(lessonIds, Optional.empty());
        return lessonIds.stream()
                .map(id -> new LessonPracticeSetsResult(id, sets.getOrDefault(id, List.of())))
                .toList();
    }

    /** Published practice sets of the lessons, by code, in two queries. */
    private Map<UUID, List<LessonPracticeSetResult>> practiceSetsOf(List<UUID> lessonIds, Optional<Skill> skill) {
        record SetRow(UUID lessonId, UUID packageId, UUID versionId, String code, String title, int questionCount,
                      String requiredFeatureKey, List<Skill> skills, BigDecimal band) {}
        var params = new MapSqlParameterSource("lessonIds", lessonIds);
        String filter = skill.map(s -> {
            params.addValue("skill", s.name());
            return "AND ARRAY(" + PRACTICE_SKILLS + ")::text[] = ARRAY[:skill]::text[]";
        }).orElse("");
        List<SetRow> rows = jdbc.query("""
                SELECT p.lesson_id, p.id, p.current_published_version_id, p.code, p.title, p.required_feature_key,
                       COALESCE(c.band_level, t.band_max, t.band_min) AS band,
                       ARRAY(%s) AS skills,
                       (SELECT count(*) FROM content_sections s JOIN section_questions sq ON sq.section_id = s.id
                        WHERE s.package_version_id = p.current_published_version_id) AS question_count
                FROM content_packages p
                LEFT JOIN lessons package_lesson ON package_lesson.id = p.lesson_id
                LEFT JOIN topics t ON t.id = COALESCE(p.topic_id, package_lesson.topic_id)
                LEFT JOIN courses c ON c.id = COALESCE(p.course_id, t.course_id)
                WHERE p.lesson_id IN (:lessonIds) AND p.package_type = 'PRACTICE_SET' AND p.status = 'PUBLISHED'
                  AND p.current_published_version_id IS NOT NULL
                  %s
                ORDER BY p.lesson_id, p.code
                """.formatted(PRACTICE_SKILLS, filter), params, (rs, i) -> new SetRow(uuid(rs, "lesson_id"), uuid(rs, "id"),
                uuid(rs, "current_published_version_id"), rs.getString("code"), rs.getString("title"),
                rs.getInt("question_count"), rs.getString("required_feature_key"), skills(rs), rs.getBigDecimal("band")));
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<UUID>> pointsByVersion = new LinkedHashMap<>();
        jdbc.query("""
                SELECT DISTINCT s.package_version_id, qkp.knowledge_point_id, kp.created_at
                FROM content_sections s
                JOIN section_questions sq ON sq.section_id = s.id
                JOIN question_knowledge_points qkp ON qkp.question_version_id = sq.question_version_id
                JOIN knowledge_points kp ON kp.id = qkp.knowledge_point_id
                WHERE s.package_version_id IN (:versionIds)
                ORDER BY s.package_version_id, kp.created_at, qkp.knowledge_point_id
                """, Map.of("versionIds", rows.stream().map(SetRow::versionId).toList()), rs -> {
            pointsByVersion.computeIfAbsent(uuid(rs, "package_version_id"), ignored -> new ArrayList<>())
                    .add(uuid(rs, "knowledge_point_id"));
        });
        Map<UUID, List<LessonPracticeSetResult>> result = new LinkedHashMap<>();
        for (SetRow row : rows) {
            result.computeIfAbsent(row.lessonId(), ignored -> new ArrayList<>())
                    .add(new LessonPracticeSetResult(row.lessonId(), row.packageId(), row.versionId(), row.code(),
                            row.title(), row.questionCount(), pointsByVersion.getOrDefault(row.versionId(), List.of()),
                            row.requiredFeatureKey(), row.skills(), row.band()));
        }
        return result;
    }

    @Override
    public Map<UUID, Integer> countEligiblePracticeSets(Collection<UUID> knowledgePointIds,
                                                        Collection<UUID> excludePackageIds, int minQuestions) {
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        if (knowledgePointIds.isEmpty()) {
            return counts;
        }
        knowledgePointIds.forEach(id -> counts.put(id, 0));
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("knowledgePointIds", knowledgePointIds)
                .addValue("minQuestions", minQuestions);
        String exclusion = "";
        if (!excludePackageIds.isEmpty()) {
            exclusion = "AND p.id NOT IN (:excludePackageIds)";
            params.addValue("excludePackageIds", excludePackageIds);
        }
        jdbc.query("""
                SELECT kp.id, (SELECT count(*) FROM content_packages p WHERE %s %s) AS available
                FROM knowledge_points kp
                WHERE kp.id IN (:knowledgePointIds)
                """.formatted(eligiblePracticeSet("kp.id"), exclusion), params,
                rs -> {
                    counts.put(uuid(rs, "id"), rs.getInt("available"));
                });
        return counts;
    }

    @Override
    public boolean packageVersionLeavesLessonSkills(UUID packageVersionId, UUID lessonId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM content_sections s
                    JOIN section_questions sq ON sq.section_id = s.id
                    JOIN question_versions qv ON qv.id = sq.question_version_id
                    JOIN questions q ON q.id = qv.question_id
                    JOIN lessons l ON l.id = :lessonId
                    WHERE s.package_version_id = :versionId AND (q.skill IS NULL OR q.skill NOT IN (%s)))
                """.formatted(LESSON_SKILLS), Map.of("versionId", packageVersionId, "lessonId", lessonId), Boolean.class));
    }

    private static List<Skill> skills(ResultSet rs) throws SQLException {
        return skills(rs, "skills");
    }

    private static List<Skill> skills(ResultSet rs, String column) throws SQLException {
        var array = rs.getArray(column);
        if (array == null) return List.of();
        try {
            return java.util.Arrays.stream((String[]) array.getArray()).map(Skill::valueOf).sorted().toList();
        } finally {
            array.free();
        }
    }

    @Override
    public List<QuestionUsageConflict> questionsUsedElsewhere(UUID packageVersionId) {
        return jdbc.query("""
                WITH target_questions AS (
                    SELECT DISTINCT qv.question_id, v.package_id
                    FROM content_package_versions v
                    JOIN content_sections s ON s.package_version_id = v.id
                    JOIN section_questions sq ON sq.section_id = s.id
                    JOIN question_versions qv ON qv.id = sq.question_version_id
                    WHERE v.id = :versionId
                )
                SELECT tq.question_id, 'LESSON' AS owner_type, l.code AS owner_code
                FROM target_questions tq
                JOIN question_versions qv ON qv.question_id = tq.question_id
                JOIN lesson_block_questions bq ON bq.question_version_id = qv.id
                JOIN lesson_blocks b ON b.id = bq.block_id
                JOIN lessons l ON l.id = b.lesson_id
                UNION
                SELECT tq.question_id, 'PACKAGE' AS owner_type, p.code AS owner_code
                FROM target_questions tq
                JOIN question_versions qv ON qv.question_id = tq.question_id
                JOIN section_questions sq ON sq.question_version_id = qv.id
                JOIN content_sections s ON s.id = sq.section_id
                JOIN content_package_versions v ON v.id = s.package_version_id
                JOIN content_packages p ON p.id = v.package_id
                WHERE p.id <> tq.package_id AND v.status = 'PUBLISHED'
                  AND p.package_type IN ('PRACTICE_SET', 'TOPIC_TEST', 'COURSE_TEST', 'MOCK_TEST', 'PLACEMENT_TEST', 'PRACTICE_TEST')
                ORDER BY question_id, owner_type, owner_code
                """, Map.of("versionId", packageVersionId), (rs, i) -> new QuestionUsageConflict(
                uuid(rs, "question_id"), QuestionUsageConflict.OwnerType.valueOf(rs.getString("owner_type")),
                rs.getString("owner_code")));
    }

    @Override
    public void lockQuestionsForPublishing(UUID packageVersionId) {
        jdbc.query("""
                SELECT q.id FROM questions q
                WHERE q.id IN (SELECT qv.question_id FROM content_sections s
                               JOIN section_questions sq ON sq.section_id = s.id
                               JOIN question_versions qv ON qv.id = sq.question_version_id
                               WHERE s.package_version_id = :versionId)
                ORDER BY q.id
                FOR UPDATE
                """, Map.of("versionId", packageVersionId), rs -> { });
    }

    @Override
    public List<UUID> questionsWithWrongPurpose(UUID packageVersionId, QuestionPurpose requiredPurpose) {
        return jdbc.query("""
                SELECT DISTINCT q.id FROM content_sections s
                JOIN section_questions sq ON sq.section_id = s.id
                JOIN question_versions qv ON qv.id = sq.question_version_id
                JOIN questions q ON q.id = qv.question_id
                WHERE s.package_version_id = :versionId AND q.purpose <> :purpose
                ORDER BY q.id
                """, Map.of("versionId", packageVersionId, "purpose", requiredPurpose.name()),
                (rs, i) -> uuid(rs, "id"));
    }

    @Override
    public List<TopicTestPackageResult> publishedTestPackages(UUID topicId) {
        return jdbc.query("""
                SELECT p.id, p.current_published_version_id, p.code
                FROM content_packages p
                WHERE p.topic_id = :topicId AND p.package_type = 'TOPIC_TEST' AND p.status = 'PUBLISHED'
                  AND p.current_published_version_id IS NOT NULL
                ORDER BY p.id
                """, Map.of("topicId", topicId), (rs, i) -> new TopicTestPackageResult(uuid(rs, "id"),
                uuid(rs, "current_published_version_id"), rs.getString("code")));
    }

    @Override
    public List<TopicTestPackageResult> courseTestPackages(UUID courseId) {
        return jdbc.query("""
                SELECT p.id, p.current_published_version_id, p.code
                FROM content_packages p
                JOIN content_package_versions v ON v.id = p.current_published_version_id AND v.package_id = p.id
                WHERE p.course_id = :courseId AND p.package_type = 'COURSE_TEST'
                  AND p.status = 'PUBLISHED' AND v.status = 'PUBLISHED'
                ORDER BY p.id
                """, Map.of("courseId", courseId), (rs, i) -> new TopicTestPackageResult(uuid(rs, "id"),
                uuid(rs, "current_published_version_id"), rs.getString("code")));
    }

    @Override
    public List<TopicTestPackageResult> placementTestPackages() {
        return jdbc.query("""
                SELECT p.id, p.current_published_version_id, p.code
                FROM content_packages p
                JOIN content_package_versions v ON v.id = p.current_published_version_id AND v.package_id = p.id
                WHERE p.package_type = 'PLACEMENT_TEST'
                  AND p.status = 'PUBLISHED' AND v.status = 'PUBLISHED'
                ORDER BY p.id
                """, (rs, i) -> new TopicTestPackageResult(uuid(rs, "id"),
                uuid(rs, "current_published_version_id"), rs.getString("code")));
    }

    @Override
    public List<PackageQuestionSpec> packageQuestionSpecs(UUID packageVersionId) {
        return jdbc.query("""
                SELECT DISTINCT qv.id, q.question_type, q.skill, qv.answer_spec::text AS answer_spec
                FROM content_sections s
                JOIN section_questions sq ON sq.section_id = s.id
                JOIN question_versions qv ON qv.id = sq.question_version_id
                JOIN questions q ON q.id = qv.question_id
                WHERE s.package_version_id = :versionId
                ORDER BY qv.id
                """, Map.of("versionId", packageVersionId), (rs, i) -> new PackageQuestionSpec(uuid(rs, "id"),
                QuestionType.valueOf(rs.getString("question_type")), enumOrNull(Skill.class, rs.getString("skill")),
                rs.getString("answer_spec")));
    }

    @Override
    public List<PracticeSetResult> searchPracticeSets(UUID knowledgePointId, Collection<UUID> excludePackageIds,
                                                      int minQuestions, int limit, UUID preferredLessonId) {
        return searchPracticeSets(knowledgePointId, excludePackageIds, minQuestions, limit, preferredLessonId, null);
    }

    @Override
    public List<PracticeSetResult> searchPracticeSets(UUID knowledgePointId, Collection<UUID> excludePackageIds,
                                                      int minQuestions, int limit, UUID preferredLessonId,
                                                      String trapErrorCode) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("knowledgePointId", knowledgePointId)
                .addValue("minQuestions", minQuestions)
                .addValue("limit", limit);
        String exclusion = "";
        if (!excludePackageIds.isEmpty()) {
            exclusion = "AND p.id NOT IN (:excludePackageIds)";
            params.addValue("excludePackageIds", excludePackageIds);
        }
        String preferred = "";
        if (preferredLessonId != null) {
            preferred = "COALESCE(ranked.lesson_id = :preferredLessonId, FALSE) DESC,";
            params.addValue("preferredLessonId", preferredLessonId);
        }
        String trapCount = trapErrorCode == null ? "0" : """
                (SELECT count(DISTINCT sq.question_version_id) FROM content_sections s
                 JOIN section_questions sq ON sq.section_id = s.id
                 JOIN question_versions qv ON qv.id = sq.question_version_id
                 WHERE s.package_version_id = p.current_published_version_id
                   AND jsonb_typeof(qv.answer_spec->'distractors') = 'object'
                   AND EXISTS (SELECT 1 FROM jsonb_each_text(qv.answer_spec->'distractors') d
                               WHERE d.value = :trapErrorCode))
                """;
        if (trapErrorCode != null) params.addValue("trapErrorCode", trapErrorCode);
        return jdbc.query("""
                SELECT ranked.id, ranked.current_published_version_id, ranked.code, ranked.required_feature_key,
                       ranked.band, ranked.question_count, ranked.matched_question_count, ranked.trap_question_count
                FROM (
                    SELECT p.id, p.current_published_version_id, p.code, p.required_feature_key, p.lesson_id,
                           COALESCE(c.band_level, t.band_max, t.band_min) AS band,
                           (SELECT count(*) FROM content_sections s JOIN section_questions sq ON sq.section_id = s.id
                            WHERE s.package_version_id = p.current_published_version_id) AS question_count,
                           (SELECT count(DISTINCT sq.question_version_id) FROM content_sections s
                            JOIN section_questions sq ON sq.section_id = s.id
                            JOIN question_knowledge_points qkp ON qkp.question_version_id = sq.question_version_id
                            WHERE s.package_version_id = p.current_published_version_id
                              AND qkp.knowledge_point_id = :knowledgePointId) AS matched_question_count,
                           %s AS trap_question_count
                    FROM content_packages p
                    LEFT JOIN lessons package_lesson ON package_lesson.id = p.lesson_id
                    LEFT JOIN topics t ON t.id = COALESCE(p.topic_id, package_lesson.topic_id)
                    LEFT JOIN courses c ON c.id = COALESCE(p.course_id, t.course_id)
                    WHERE %s %s
                ) ranked
                ORDER BY CASE WHEN ranked.trap_question_count >= 2 THEN ranked.trap_question_count ELSE 0 END DESC,
                         CASE WHEN ranked.trap_question_count >= 2
                              THEN ranked.trap_question_count::numeric / NULLIF(ranked.question_count, 0)
                              ELSE 0 END DESC,
                         %s ranked.matched_question_count DESC, ranked.id
                LIMIT :limit
                """.formatted(trapCount, eligiblePracticeSet(":knowledgePointId"), exclusion, preferred), params,
                (rs, i) -> new PracticeSetResult(uuid(rs, "id"), uuid(rs, "current_published_version_id"),
                        rs.getString("code"), rs.getInt("question_count"), rs.getInt("matched_question_count"),
                        rs.getBigDecimal("band"), rs.getString("required_feature_key"),
                        rs.getInt("trap_question_count")));
    }

    @Override
    public Optional<PackageVersionContentResult> publishedPackageVersion(UUID packageVersionId) {
        return packageVersionContent(packageVersionId, true);
    }

    @Override
    public Optional<PackageVersionContentResult> packageVersionContent(UUID packageVersionId) {
        return packageVersionContent(packageVersionId, false);
    }

    private Optional<PackageVersionContentResult> packageVersionContent(UUID packageVersionId, boolean publishedOnly) {
        record VersionRow(UUID packageId, PackageType packageType, UUID topicId, String rules) {}
        Map<String, Object> byVersion = Map.of("versionId", packageVersionId);
        String publicationFilter = publishedOnly ? " AND v.status = 'PUBLISHED'" : "";
        List<VersionRow> found = jdbc.query(("""
                SELECT v.package_id, p.package_type, p.topic_id, v.rules::text AS rules
                FROM content_package_versions v
                JOIN content_packages p ON p.id = v.package_id
                WHERE v.id = :versionId%s
                """).formatted(publicationFilter), byVersion, (rs, i) -> new VersionRow(uuid(rs, "package_id"),
                PackageType.valueOf(rs.getString("package_type")), uuid(rs, "topic_id"), rs.getString("rules")));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        VersionRow version = found.get(0);

        Map<UUID, List<PackageVersionContentResult.KnowledgePointMapping>> mappings = new LinkedHashMap<>();
        jdbc.query("""
                SELECT DISTINCT qkp.question_version_id, qkp.knowledge_point_id, qkp.weight
                FROM question_knowledge_points qkp
                JOIN section_questions sq ON sq.question_version_id = qkp.question_version_id
                JOIN content_sections s ON s.id = sq.section_id
                WHERE s.package_version_id = :versionId
                ORDER BY qkp.question_version_id, qkp.knowledge_point_id
                """, byVersion, rs -> {
            mappings.computeIfAbsent(uuid(rs, "question_version_id"), ignored -> new ArrayList<>())
                    .add(new PackageVersionContentResult.KnowledgePointMapping(uuid(rs, "knowledge_point_id"),
                            rs.getDouble("weight")));
        });

        Map<UUID, List<PackageVersionContentResult.Image>> imagesByQuestion = new LinkedHashMap<>();
        jdbc.query("""
                SELECT DISTINCT sq.question_version_id, a.id, a.media_reference, a.text_content, l.sort_order
                FROM section_questions sq
                JOIN content_sections s ON s.id = sq.section_id
                JOIN question_versions qv ON qv.id = sq.question_version_id
                JOIN content_asset_links l ON l.question_version_id = sq.question_version_id OR l.section_id = s.id
                JOIN content_assets a ON a.id = l.asset_id
                WHERE s.package_version_id = :versionId AND qv.answer_spec ->> 'type' = 'ESSAY'
                  AND a.asset_type = 'IMAGE' AND a.validation_status = 'VALID'
                ORDER BY sq.question_version_id, l.sort_order, a.id
                """, byVersion, rs -> {
            imagesByQuestion.computeIfAbsent(uuid(rs, "question_version_id"), ignored -> new ArrayList<>())
                    .add(new PackageVersionContentResult.Image(rs.getString("media_reference"), null,
                            rs.getString("text_content")));
        });

        Map<UUID, List<PackageVersionContentResult.Item>> itemsBySection = new LinkedHashMap<>();
        jdbc.query("""
                SELECT sq.section_id, sq.sort_order, sq.max_score, sq.group_id, qv.id, qv.stem, qv.options::text AS options,
                       qv.answer_spec::text AS answer_spec, qv.explanation, qv.hint,
                       CASE WHEN qv.answer_spec ->> 'type' = 'ESSAY' THEN qv.guide END AS guide
                FROM section_questions sq
                JOIN content_sections s ON s.id = sq.section_id
                JOIN question_versions qv ON qv.id = sq.question_version_id
                WHERE s.package_version_id = :versionId
                ORDER BY sq.section_id, sq.sort_order
                """, byVersion, rs -> {
            UUID versionId = uuid(rs, "id");
            itemsBySection.computeIfAbsent(uuid(rs, "section_id"), ignored -> new ArrayList<>())
                    .add(new PackageVersionContentResult.Item(versionId, rs.getInt("sort_order"),
                            rs.getString("stem"), rs.getString("options"), rs.getString("answer_spec"),
                            rs.getString("explanation"), rs.getString("hint"), rs.getDouble("max_score"),
                            rs.getObject("group_id", UUID.class),
                            mappings.getOrDefault(versionId, List.of()), rs.getString("guide"),
                            imagesByQuestion.getOrDefault(versionId, List.of())));
        });

        Map<UUID, List<PackageVersionContentResult.QuestionGroup>> groupsBySection = new LinkedHashMap<>();
        jdbc.query("""
                SELECT g.id, g.section_id, g.sort_order, g.format_code, g.instructions
                FROM section_question_groups g
                JOIN content_sections s ON s.id = g.section_id
                WHERE s.package_version_id = :versionId
                ORDER BY g.section_id, g.sort_order
                """, byVersion, rs -> {
            groupsBySection.computeIfAbsent(uuid(rs, "section_id"), ignored -> new ArrayList<>())
                    .add(new PackageVersionContentResult.QuestionGroup(uuid(rs, "id"), rs.getInt("sort_order"),
                            rs.getString("format_code"), rs.getString("instructions")));
        });

        List<PackageVersionContentResult.Section> sections = jdbc.query("""
                SELECT s.id, s.title, s.skill, s.instructions, s.sort_order, s.part_no, s.time_limit_seconds,
                       (SELECT a.text_content FROM content_asset_links al
                        JOIN content_assets a ON a.id = al.asset_id
                        WHERE al.section_id = s.id AND a.asset_type = 'PASSAGE'
                        ORDER BY al.sort_order, a.id LIMIT 1) AS passage,
                       audio.id AS audio_id, audio.media_reference AS audio_reference,
                       audio.duration_seconds AS audio_duration, audio.text_content AS audio_transcript
                FROM content_sections s
                LEFT JOIN LATERAL (SELECT a.id, a.media_reference, a.duration_seconds, a.text_content
                                   FROM content_asset_links al JOIN content_assets a ON a.id = al.asset_id
                                   WHERE al.section_id = s.id AND a.asset_type = 'AUDIO'
                                   ORDER BY al.sort_order, a.id LIMIT 1) audio ON TRUE
                WHERE s.package_version_id = :versionId
                ORDER BY s.sort_order
                """, byVersion, (rs, i) -> {
            UUID sectionId = uuid(rs, "id");
            return new PackageVersionContentResult.Section(sectionId, rs.getString("title"),
                    enumOrNull(Skill.class, rs.getString("skill")), rs.getString("instructions"),
                    rs.getInt("sort_order"), (Integer) rs.getObject("part_no"), (Integer) rs.getObject("time_limit_seconds"),
                    rs.getString("passage"),
                    rs.getObject("audio_id") == null ? null : new PackageVersionContentResult.SectionAudio(
                            uuid(rs, "audio_id"), rs.getString("audio_reference"), null,
                            (Integer) rs.getObject("audio_duration"), rs.getString("audio_transcript")),
                    groupsBySection.getOrDefault(sectionId, List.of()),
                    itemsBySection.getOrDefault(sectionId, List.of()));
        });

        return Optional.of(new PackageVersionContentResult(packageVersionId, version.packageId(),
                version.packageType(), version.topicId(), version.rules(), sections));
    }

    @Override
    public List<String> practiceTestPublishIssues(UUID packageVersionId) {
        record Validation(int sections, int distinctParts, int validSkills, int distinctSkills, int groupedSections,
                          int ungroupedQuestions, int multiAnswerGroups) {}
        List<Validation> rows = jdbc.query("""
                SELECT (SELECT COUNT(*)::int FROM content_sections s WHERE s.package_version_id = :versionId) sections,
                       (SELECT COUNT(DISTINCT s.part_no)::int FROM content_sections s WHERE s.package_version_id = :versionId) distinct_parts,
                       (SELECT COUNT(*)::int FROM content_sections s WHERE s.package_version_id = :versionId
                         AND s.part_no BETWEEN 1 AND 4 AND s.skill IN ('READING', 'LISTENING', 'WRITING')) valid_skills,
                       (SELECT COUNT(DISTINCT s.skill)::int FROM content_sections s WHERE s.package_version_id = :versionId) distinct_skills,
                       (SELECT COUNT(DISTINCT g.section_id)::int FROM section_question_groups g
                        JOIN content_sections s ON s.id = g.section_id WHERE s.package_version_id = :versionId) grouped_sections,
                       (SELECT COUNT(*)::int FROM section_questions sq JOIN content_sections s ON s.id = sq.section_id
                        WHERE s.package_version_id = :versionId AND sq.group_id IS NULL) ungrouped_questions,
                       (SELECT COUNT(*)::int FROM section_question_groups g JOIN content_sections s ON s.id = g.section_id
                        WHERE s.package_version_id = :versionId AND g.format_code = 'MULTIPLE_CHOICE_MANY') multi_answer_groups
                """, Map.of("versionId", packageVersionId), (rs, row) -> new Validation(rs.getInt("sections"),
                rs.getInt("distinct_parts"), rs.getInt("valid_skills"), rs.getInt("distinct_skills"), rs.getInt("grouped_sections"),
                rs.getInt("ungrouped_questions"), rs.getInt("multi_answer_groups")));
        if (rows.isEmpty()) return List.of("Practice test version has no sections");
        Validation value = rows.getFirst();
        List<String> issues = new ArrayList<>();
        if (value.sections() == 0 || value.sections() != value.distinctParts() || value.sections() != value.validSkills()
                || value.distinctSkills() != 1)
            issues.add("Practice tests require unique part numbers and one supported skill per section");
        if (value.sections() != value.groupedSections() || value.ungroupedQuestions() > 0)
            issues.add("Every practice test section and question must belong to a question group");
        if (value.multiAnswerGroups() > 0) issues.add("Multiple-answer groups are not supported for practice tests");
        return List.copyOf(issues);
    }

    @Override
    public Set<UUID> questionVersionsReservedForLearning(Collection<UUID> questionVersionIds) {
        if (questionVersionIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.query("""
                SELECT bq.question_version_id AS id FROM lesson_block_questions bq
                WHERE bq.question_version_id IN (:ids)
                UNION
                SELECT sq.question_version_id FROM section_questions sq
                JOIN content_sections s ON s.id = sq.section_id
                JOIN content_package_versions v ON v.id = s.package_version_id
                JOIN content_packages p ON p.id = v.package_id
                WHERE sq.question_version_id IN (:ids)
                  AND p.package_type IN ('TOPIC_TEST', 'COURSE_TEST', 'MOCK_TEST', 'PLACEMENT_TEST', 'PRACTICE_SET', 'PRACTICE_TEST')
                """, Map.of("ids", questionVersionIds), (rs, i) -> uuid(rs, "id")));
    }

    @Override
    public List<KnowledgeTreeNodeResult> knowledgeTreeNodes(UUID courseId, Skill skill, boolean includeInactive) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
        if (!includeInactive) {
            where.append(" AND kp.status = 'ACTIVE' ");
        }
        if (courseId != null) {
            where.append(" AND t.course_id = :courseId ");
            parameters.addValue("courseId", courseId);
        }
        if (skill != null) {
            where.append(" AND kp.skill = :skill ");
            parameters.addValue("skill", skill.name());
        }
        return jdbc.query("""
                SELECT kp.id, kp.code, kp.name, kp.skill, kp.kind, kp.learning_type,
                       kp.cefr_level, kp.can_do, kp.scope_in, kp.scope_out,
                       c.band_level AS band, c.id AS course_id, kp.topic_id, kp.status,
                       ARRAY(SELECT lkp.lesson_id FROM lesson_knowledge_points lkp
                             WHERE lkp.knowledge_point_id = kp.id ORDER BY lkp.lesson_id) AS lesson_ids
                FROM knowledge_points kp
                LEFT JOIN topics t ON t.id = kp.topic_id
                LEFT JOIN courses c ON c.id = t.course_id
                """ + where + " ORDER BY c.band_level NULLS FIRST, t.course_id NULLS FIRST, kp.skill, kp.code, kp.id",
                parameters, (rs, row) -> new KnowledgeTreeNodeResult(
                        uuid(rs, "id"), rs.getString("code"), rs.getString("name"),
                        enumOrNull(Skill.class, rs.getString("skill")),
                        KnowledgePointKind.valueOf(rs.getString("kind")),
                        enumOrNull(LearningType.class, rs.getString("learning_type")),
                        rs.getBigDecimal("band"), nullableUuid(rs, "course_id"), uuid(rs, "topic_id"),
                        uuids(rs.getArray("lesson_ids")), ContentStatus.valueOf(rs.getString("status")),
                        enumOrNull(CefrLevel.class, rs.getString("cefr_level")), rs.getString("can_do"),
                        rs.getString("scope_in"), rs.getString("scope_out")));
    }

    @Override
    public Map<UUID, List<UUID>> activeKnowledgePrerequisites(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<UUID> distinctIds = ids.stream().distinct().toList();
        Map<UUID, List<UUID>> prerequisitesByKnowledgePoint = new LinkedHashMap<>();
        jdbc.query("""
                SELECT id FROM knowledge_points WHERE status = 'ACTIVE' AND id IN (:ids) ORDER BY id
                """, Map.of("ids", distinctIds),
                rs -> {
                    prerequisitesByKnowledgePoint.put(uuid(rs, "id"), new ArrayList<>());
                });
        if (prerequisitesByKnowledgePoint.isEmpty()) {
            return Map.of();
        }
        jdbc.query("""
                SELECT kp.id AS knowledge_point_id, prereq.prerequisite_id
                FROM knowledge_points kp
                JOIN knowledge_point_prerequisites prereq ON prereq.knowledge_point_id = kp.id
                JOIN knowledge_points prerequisite ON prerequisite.id = prereq.prerequisite_id
                WHERE kp.status = 'ACTIVE' AND prerequisite.status = 'ACTIVE' AND kp.id IN (:ids)
                ORDER BY kp.id, prereq.prerequisite_id
                """, Map.of("ids", prerequisitesByKnowledgePoint.keySet()), rs -> {
                    prerequisitesByKnowledgePoint.get(uuid(rs, "knowledge_point_id"))
                            .add(uuid(rs, "prerequisite_id"));
                });
        return prerequisitesByKnowledgePoint;
    }

    @Override
    public Map<UUID, QuestionDifficulty> questionDifficulties(Collection<UUID> questionVersionIds) {
        if (questionVersionIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> distinctIds = questionVersionIds.stream().distinct().toList();
        Map<UUID, QuestionDifficulty> difficulties = new LinkedHashMap<>();
        jdbc.query("""
                SELECT id, difficulty
                FROM question_versions
                WHERE id IN (:ids) AND difficulty IS NOT NULL
                ORDER BY id
                """, Map.of("ids", distinctIds), rs -> {
            difficulties.put(uuid(rs, "id"), QuestionDifficulty.valueOf(rs.getString("difficulty")));
        });
        return difficulties;
    }

    @Override
    public com.ieltspath.content.application.result.WorkItemSubjectsResult workItemSubjects(
            Collection<UUID> knowledgePointIds, Collection<UUID> questionVersionIds) {
        Map<UUID, com.ieltspath.content.application.result.WorkItemSubjectsResult.KnowledgePointLabel> points =
                new LinkedHashMap<>();
        if (!knowledgePointIds.isEmpty()) {
            jdbc.query("SELECT id, code, name FROM knowledge_points WHERE id IN (:ids) ORDER BY id",
                    Map.of("ids", knowledgePointIds.stream().distinct().toList()), rs -> {
                        points.put(uuid(rs, "id"),
                                new com.ieltspath.content.application.result.WorkItemSubjectsResult
                                        .KnowledgePointLabel(rs.getString("code"), rs.getString("name")));
                    });
        }
        Map<UUID, com.ieltspath.content.application.result.WorkItemSubjectsResult.QuestionLabel> questions =
                new LinkedHashMap<>();
        if (!questionVersionIds.isEmpty()) {
            jdbc.query("SELECT id, question_id, left(stem, 120) AS stem_preview FROM question_versions "
                            + "WHERE id IN (:ids) ORDER BY id",
                    Map.of("ids", questionVersionIds.stream().distinct().toList()), rs -> {
                        questions.put(uuid(rs, "id"),
                                new com.ieltspath.content.application.result.WorkItemSubjectsResult
                                        .QuestionLabel(uuid(rs, "question_id"), rs.getString("stem_preview")));
                    });
        }
        return new com.ieltspath.content.application.result.WorkItemSubjectsResult(points, questions);
    }

    private Map<UUID, List<UUID>> lessonKnowledgePoints(List<UUID> lessonIds) {
        Map<UUID, List<UUID>> result = new LinkedHashMap<>();
        jdbc.query("""
                SELECT lkp.lesson_id, lkp.knowledge_point_id
                FROM lesson_knowledge_points lkp
                JOIN knowledge_points kp ON kp.id = lkp.knowledge_point_id
                WHERE lkp.lesson_id IN (:lessonIds)
                ORDER BY lkp.lesson_id, kp.created_at, kp.id
                """, Map.of("lessonIds", lessonIds), rs -> {
            result.computeIfAbsent(uuid(rs, "lesson_id"), ignored -> new ArrayList<>())
                    .add(uuid(rs, "knowledge_point_id"));
        });
        return result;
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static UUID nullableUuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static List<UUID> uuids(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        Object[] values = (Object[]) array.getArray();
        List<UUID> result = new ArrayList<>(values.length);
        for (Object value : values) {
            result.add((UUID) value);
        }
        return List.copyOf(result);
    }

    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }
}
