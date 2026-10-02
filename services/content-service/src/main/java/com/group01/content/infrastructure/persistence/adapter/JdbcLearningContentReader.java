package com.group01.content.infrastructure.persistence.adapter;

import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.LessonContentResult;
import com.group01.content.application.result.LessonSummaryResult;
import com.group01.content.application.result.PackageVersionContentResult;
import com.group01.content.application.result.PracticeSetResult;
import com.group01.content.application.result.TopicSequenceResult;
import com.group01.content.application.result.TopicTestPackageResult;
import com.group01.content.domain.vo.AssetType;
import com.group01.content.domain.vo.BlockType;
import com.group01.content.domain.vo.LessonBlockKind;
import com.group01.content.domain.vo.LearningType;
import com.group01.content.domain.vo.PackageType;
import com.group01.content.domain.vo.Skill;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
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
 * Plain SQL projections of the published curriculum. Collections are loaded per request with {@code IN} lists and
 * joined in memory by id, so no method issues one query per row.
 */
@Component
public class JdbcLearningContentReader implements LearningContentReader {

    /** Question versions that a lesson or any final test uses; review material must not repeat them. */
    private static final String LESSON_OR_TEST_QUESTION_VERSIONS = """
            SELECT bq.question_version_id FROM lesson_block_questions bq
            UNION
            SELECT tsq.question_version_id
            FROM section_questions tsq
            JOIN content_sections ts ON ts.id = tsq.section_id
            JOIN content_package_versions tv ON tv.id = ts.package_version_id
            JOIN content_packages tp ON tp.id = tv.package_id
            WHERE tp.package_type = 'TOPIC_TEST'
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

    public JdbcLearningContentReader(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String eligiblePracticeSet(String knowledgePointExpression) {
        return ELIGIBLE_PRACTICE_SET.formatted(knowledgePointExpression, LESSON_OR_TEST_QUESTION_VERSIONS);
    }

    @Override
    public List<TopicSequenceResult> topicSequence(int minPracticeQuestions) {
        record TopicRow(UUID id, String code, String name, int sortOrder, String requiredFeatureKey) {}
        List<TopicRow> topics = jdbc.query("""
                SELECT t.id, t.code, t.name, t.sort_order, t.required_feature_key
                FROM topics t
                WHERE t.status = 'ACTIVE'
                  AND EXISTS (SELECT 1 FROM lessons l WHERE l.topic_id = t.id AND l.status = 'PUBLISHED')
                  AND EXISTS (SELECT 1 FROM content_packages p
                              WHERE p.topic_id = t.id AND p.package_type = 'TOPIC_TEST'
                                AND p.status = 'PUBLISHED' AND p.current_published_version_id IS NOT NULL)
                ORDER BY t.sort_order, t.id
                """, Map.of(), (rs, i) -> new TopicRow(uuid(rs, "id"), rs.getString("code"), rs.getString("name"),
                rs.getInt("sort_order"), rs.getString("required_feature_key")));
        if (topics.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<TopicSequenceResult.KnowledgePointEntry>> pointsByTopic = new LinkedHashMap<>();
        jdbc.query("""
                SELECT kp.id, kp.topic_id, kp.code, kp.name, kp.learning_type, kp.skill, kp.description,
                       EXISTS (SELECT 1 FROM content_packages p WHERE %s) AS has_practice_set
                FROM knowledge_points kp
                WHERE kp.status = 'ACTIVE' AND kp.topic_id IN (:topicIds)
                ORDER BY kp.created_at, kp.id
                """.formatted(eligiblePracticeSet("kp.id")),
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
                                    rs.getString("description"), rs.getBoolean("has_practice_set")));
                });

        return topics.stream()
                .map(t -> new TopicSequenceResult(t.id(), t.code(), t.name(), t.sortOrder(), t.requiredFeatureKey(),
                        pointsByTopic.getOrDefault(t.id(), List.of())))
                .toList();
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
        record LessonRow(UUID id, String code, String title, String summary, int sortOrder) {}
        List<LessonRow> lessons = jdbc.query("""
                SELECT l.id, l.code, l.title, l.summary, l.sort_order
                FROM lessons l
                WHERE l.topic_id = :topicId AND l.status = 'PUBLISHED'
                ORDER BY l.sort_order
                """, Map.of("topicId", topicId), (rs, i) -> new LessonRow(uuid(rs, "id"), rs.getString("code"),
                rs.getString("title"), rs.getString("summary"), rs.getInt("sort_order")));
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
                        exerciseBlocks.getOrDefault(l.id(), List.of())))
                .toList();
    }

    @Override
    public Optional<LessonContentResult> publishedLesson(UUID lessonId) {
        record LessonRow(UUID topicId, String code, String title, String summary, int sortOrder) {}
        List<LessonRow> found = jdbc.query("""
                SELECT l.topic_id, l.code, l.title, l.summary, l.sort_order FROM lessons l
                WHERE l.id = :id AND l.status = 'PUBLISHED'
                """, Map.of("id", lessonId), (rs, i) -> new LessonRow(uuid(rs, "topic_id"), rs.getString("code"),
                rs.getString("title"), rs.getString("summary"), rs.getInt("sort_order")));
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
            return new LessonContentResult.Block(blockId, type, null, rs.getInt("sort_order"),
                    type == BlockType.TEXT ? rs.getString("text_content") : null,
                    asset,
                    type == BlockType.VOCABULARY ? sensesByBlock.getOrDefault(blockId, List.of()) : null,
                    type == BlockType.EXERCISE ? questionsByBlock.getOrDefault(blockId, List.of()) : null);
        });

        return Optional.of(new LessonContentResult(lessonId, lesson.topicId(), lesson.code(), lesson.title(),
                lesson.summary(), lesson.sortOrder(),
                lessonKnowledgePoints(List.of(lessonId)).getOrDefault(lessonId, List.of()), blocks));
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
    public List<PracticeSetResult> searchPracticeSets(UUID knowledgePointId, Collection<UUID> excludePackageIds,
                                                      int minQuestions, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("knowledgePointId", knowledgePointId)
                .addValue("minQuestions", minQuestions)
                .addValue("limit", limit);
        String exclusion = "";
        if (!excludePackageIds.isEmpty()) {
            exclusion = "AND p.id NOT IN (:excludePackageIds)";
            params.addValue("excludePackageIds", excludePackageIds);
        }
        return jdbc.query("""
                SELECT p.id, p.current_published_version_id, p.code,
                       (SELECT count(*) FROM content_sections s JOIN section_questions sq ON sq.section_id = s.id
                        WHERE s.package_version_id = p.current_published_version_id) AS question_count,
                       (SELECT count(DISTINCT sq.question_version_id) FROM content_sections s
                        JOIN section_questions sq ON sq.section_id = s.id
                        JOIN question_knowledge_points qkp ON qkp.question_version_id = sq.question_version_id
                        WHERE s.package_version_id = p.current_published_version_id
                          AND qkp.knowledge_point_id = :knowledgePointId) AS matched_question_count
                FROM content_packages p
                WHERE %s %s
                ORDER BY matched_question_count DESC, p.id
                LIMIT :limit
                """.formatted(eligiblePracticeSet(":knowledgePointId"), exclusion), params,
                (rs, i) -> new PracticeSetResult(uuid(rs, "id"), uuid(rs, "current_published_version_id"),
                        rs.getString("code"), rs.getInt("question_count"), rs.getInt("matched_question_count")));
    }

    @Override
    public Optional<PackageVersionContentResult> publishedPackageVersion(UUID packageVersionId) {
        record VersionRow(UUID packageId, PackageType packageType, UUID topicId, String rules) {}
        Map<String, Object> byVersion = Map.of("versionId", packageVersionId);
        List<VersionRow> found = jdbc.query("""
                SELECT v.package_id, p.package_type, p.topic_id, v.rules::text AS rules
                FROM content_package_versions v
                JOIN content_packages p ON p.id = v.package_id
                WHERE v.id = :versionId AND v.status = 'PUBLISHED'
                """, byVersion, (rs, i) -> new VersionRow(uuid(rs, "package_id"),
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

        Map<UUID, List<PackageVersionContentResult.Item>> itemsBySection = new LinkedHashMap<>();
        jdbc.query("""
                SELECT sq.section_id, sq.sort_order, sq.max_score, qv.id, qv.stem, qv.options::text AS options,
                       qv.answer_spec::text AS answer_spec, qv.explanation
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
                            rs.getString("explanation"), rs.getDouble("max_score"),
                            mappings.getOrDefault(versionId, List.of())));
        });

        List<PackageVersionContentResult.Section> sections = jdbc.query("""
                SELECT s.id, s.title, s.skill, s.instructions, s.sort_order,
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
                    rs.getInt("sort_order"), rs.getString("passage"),
                    rs.getObject("audio_id") == null ? null : new PackageVersionContentResult.SectionAudio(
                            uuid(rs, "audio_id"), rs.getString("audio_reference"), null,
                            (Integer) rs.getObject("audio_duration"), rs.getString("audio_transcript")),
                    itemsBySection.getOrDefault(sectionId, List.of()));
        });

        return Optional.of(new PackageVersionContentResult(packageVersionId, version.packageId(),
                version.packageType(), version.topicId(), version.rules(), sections));
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
                WHERE sq.question_version_id IN (:ids) AND p.package_type IN ('TOPIC_TEST', 'PRACTICE_SET')
                """, Map.of("ids", questionVersionIds), (rs, i) -> uuid(rs, "id")));
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

    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }
}
