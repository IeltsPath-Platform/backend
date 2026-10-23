package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.AuthorPackageReader;
import com.ieltspath.content.application.result.AuthorPackageExtras;
import com.ieltspath.content.application.result.AuthorPackageSummary;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PublicationStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JdbcAuthorPackageReader implements AuthorPackageReader {

    private static final int STEM_PREVIEW_LENGTH = 200;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAuthorPackageReader(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AuthorPackageSummary> list(Filter filter) {
        MapSqlParameterSource params = new MapSqlParameterSource("limit", LIST_LIMIT);
        StringBuilder where = new StringBuilder(" WHERE TRUE");
        if (filter.type() != null) {
            where.append(" AND p.package_type = :type");
            params.addValue("type", filter.type().name());
        }
        if (filter.status() != null) {
            where.append(" AND p.status = :status");
            params.addValue("status", filter.status().name());
        }
        if (filter.lessonId() != null) {
            where.append(" AND p.lesson_id = :lessonId");
            params.addValue("lessonId", filter.lessonId());
        }
        if (filter.topicId() != null) {
            where.append(" AND p.topic_id = :topicId");
            params.addValue("topicId", filter.topicId());
        }
        if (filter.courseId() != null) {
            where.append(" AND p.course_id = :courseId");
            params.addValue("courseId", filter.courseId());
        }
        if (filter.q() != null && !filter.q().isBlank()) {
            where.append(" AND (p.code ILIKE :q ESCAPE '\\' OR p.title ILIKE :q ESCAPE '\\')");
            params.addValue("q", "%" + escapeLike(filter.q().strip()) + "%");
        }
        return jdbc.query("""
                SELECT p.id, p.code, p.title, p.package_type, p.required_feature_key, p.status,
                       p.current_published_version_id, p.lesson_id, p.topic_id, p.course_id, p.source_code,
                       p.created_at, p.updated_at,
                       (SELECT MAX(v.version_number) FROM content_package_versions v
                        WHERE v.package_id = p.id) AS latest_version_number,
                       EXISTS (SELECT 1 FROM content_package_versions v
                               WHERE v.package_id = p.id AND v.status = 'DRAFT') AS has_draft
                FROM content_packages p
                """ + where + """

                ORDER BY p.updated_at DESC, p.id
                LIMIT :limit
                """, params, (rs, i) -> new AuthorPackageSummary(uuid(rs, "id"), rs.getString("code"),
                rs.getString("title"), PackageType.valueOf(rs.getString("package_type")),
                rs.getString("required_feature_key"), PublicationStatus.valueOf(rs.getString("status")),
                uuid(rs, "current_published_version_id"), uuid(rs, "lesson_id"), uuid(rs, "topic_id"),
                uuid(rs, "course_id"), rs.getString("source_code"), instant(rs, "created_at"),
                instant(rs, "updated_at"), (Integer) rs.getObject("latest_version_number"),
                rs.getBoolean("has_draft")));
    }

    @Override
    public AuthorPackageExtras extras(UUID packageId) {
        Map<String, Object> byPackage = Map.of("packageId", packageId);

        Map<UUID, AuthorPackageExtras.SectionMedia> sections = new LinkedHashMap<>();
        jdbc.query("""
                SELECT s.id AS section_id,
                       (SELECT a.text_content FROM content_asset_links al
                        JOIN content_assets a ON a.id = al.asset_id
                        WHERE al.section_id = s.id AND a.asset_type = 'PASSAGE'
                        ORDER BY al.sort_order, a.id LIMIT 1) AS passage,
                       audio.id AS audio_id, audio.media_reference, audio.duration_seconds,
                       audio.text_content AS transcript
                FROM content_sections s
                JOIN content_package_versions v ON v.id = s.package_version_id
                LEFT JOIN LATERAL (SELECT a.id, a.media_reference, a.duration_seconds, a.text_content
                                   FROM content_asset_links al JOIN content_assets a ON a.id = al.asset_id
                                   WHERE al.section_id = s.id AND a.asset_type = 'AUDIO'
                                   ORDER BY al.sort_order, a.id LIMIT 1) audio ON TRUE
                WHERE v.package_id = :packageId
                """, byPackage, rs -> {
            UUID audioId = uuid(rs, "audio_id");
            AuthorPackageExtras.Audio audio = audioId == null ? null : new AuthorPackageExtras.Audio(audioId,
                    rs.getString("media_reference"), null, (Integer) rs.getObject("duration_seconds"),
                    rs.getString("transcript"));
            sections.put(uuid(rs, "section_id"), new AuthorPackageExtras.SectionMedia(rs.getString("passage"), audio));
        });

        Map<UUID, AuthorPackageExtras.QuestionInfo> questions = new LinkedHashMap<>();
        jdbc.query("""
                SELECT sq.id AS section_question_id, q.id AS question_id, left(qv.stem, %d) AS stem,
                       qv.status, q.purpose
                FROM section_questions sq
                JOIN content_sections s ON s.id = sq.section_id
                JOIN content_package_versions v ON v.id = s.package_version_id
                JOIN question_versions qv ON qv.id = sq.question_version_id
                JOIN questions q ON q.id = qv.question_id
                WHERE v.package_id = :packageId
                """.formatted(STEM_PREVIEW_LENGTH), byPackage, rs -> {
            questions.put(uuid(rs, "section_question_id"), new AuthorPackageExtras.QuestionInfo(
                    uuid(rs, "question_id"), rs.getString("stem"), rs.getString("status"), rs.getString("purpose")));
        });
        return new AuthorPackageExtras(sections, questions);
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
