package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.WritingTaskReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcWritingTaskReader implements WritingTaskReader {
    private final JdbcTemplate jdbc;

    public JdbcWritingTaskReader(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<WritingTask> find(UUID packageVersionId, UUID sectionId) {
        List<Row> rows = jdbc.query("""
                SELECT p.id package_id, p.title package_title, s.title, s.part_no, s.time_limit_seconds, s.instructions,
                       qv.id question_version_id, qv.stem, qv.guide, qv.guide_steps::text guide_steps,
                       qv.answer_spec ->> 'task' task, (qv.answer_spec ->> 'minWords')::int min_words
                FROM content_packages p
                JOIN content_package_versions v ON v.package_id = p.id
                JOIN content_sections s ON s.package_version_id = v.id
                JOIN section_questions sq ON sq.section_id = s.id
                JOIN question_versions qv ON qv.id = sq.question_version_id
                WHERE v.id = ? AND s.id = ? AND p.package_type = 'PRACTICE_TEST' AND p.status = 'PUBLISHED'
                  AND v.status = 'PUBLISHED' AND p.current_published_version_id = v.id AND s.skill = 'WRITING'
                  AND qv.answer_spec ->> 'type' = 'ESSAY'
                ORDER BY sq.sort_order LIMIT 1
                """, (row, index) -> new Row(row.getObject("package_id", UUID.class), row.getString("package_title"),
                row.getString("title"), (Integer) row.getObject("part_no"), (Integer) row.getObject("time_limit_seconds"),
                row.getString("instructions"), row.getObject("question_version_id", UUID.class), row.getString("stem"),
                row.getString("task"), (Integer) row.getObject("min_words"), row.getString("guide"),
                row.getString("guide_steps")), packageVersionId, sectionId);
        if (rows.isEmpty()) return Optional.empty();
        Row row = rows.getFirst();
        List<Image> images = jdbc.query("""
                SELECT a.media_reference, a.text_content
                FROM content_asset_links l JOIN content_assets a ON a.id = l.asset_id
                WHERE l.question_version_id = ? AND a.asset_type = 'IMAGE' AND a.validation_status = 'VALID'
                ORDER BY l.sort_order, l.created_at
                """, (image, index) -> new Image(image.getString("media_reference"), image.getString("text_content")),
                row.questionVersionId());
        return Optional.of(new WritingTask(row.packageId(), packageVersionId, sectionId, row.packageTitle(),
                row.title(), row.partNo(), row.timeLimitSeconds(), row.instructions(), row.questionVersionId(),
                row.stem(), row.task(), row.minWords(), row.guide(), row.guideSteps(), images));
    }

    private record Row(UUID packageId, String packageTitle, String title, Integer partNo, Integer timeLimitSeconds,
                       String instructions, UUID questionVersionId, String stem, String task, Integer minWords,
                       String guide, String guideSteps) {}
}
