package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.SpeakingTaskReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcSpeakingTaskReader implements SpeakingTaskReader {
    private final JdbcTemplate jdbc;

    public JdbcSpeakingTaskReader(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<SpeakingTask> find(UUID packageVersionId, UUID sectionId) {
        List<Row> rows = jdbc.query("""
                SELECT p.id package_id, p.title package_title, s.title, s.part_no, s.time_limit_seconds, s.instructions,
                       qv.id question_version_id, sq.sort_order, qv.stem,
                       (qv.answer_spec ->> 'durationSeconds')::int duration_seconds
                FROM content_packages p
                JOIN content_package_versions v ON v.package_id = p.id
                JOIN content_sections s ON s.package_version_id = v.id
                JOIN section_questions sq ON sq.section_id = s.id
                JOIN question_versions qv ON qv.id = sq.question_version_id
                WHERE v.id = ? AND s.id = ? AND p.package_type = 'PRACTICE_TEST' AND p.status = 'PUBLISHED'
                  AND v.status = 'PUBLISHED' AND p.current_published_version_id = v.id AND s.skill = 'SPEAKING'
                  AND qv.answer_spec ->> 'type' = 'SPEAKING'
                ORDER BY sq.sort_order
                """, (row, index) -> new Row(row.getObject("package_id", UUID.class), row.getString("package_title"),
                row.getString("title"), (Integer) row.getObject("part_no"), (Integer) row.getObject("time_limit_seconds"),
                row.getString("instructions"), new Question(row.getObject("question_version_id", UUID.class),
                row.getInt("sort_order"), row.getString("stem"), (Integer) row.getObject("duration_seconds"))),
                packageVersionId, sectionId);
        if (rows.isEmpty()) return Optional.empty();
        Row first = rows.getFirst();
        return Optional.of(new SpeakingTask(first.packageId(), packageVersionId, sectionId, first.packageTitle(),
                first.title(), first.partNo(), first.timeLimitSeconds(), first.instructions(),
                rows.stream().map(Row::question).toList()));
    }

    private record Row(UUID packageId, String packageTitle, String title, Integer partNo, Integer timeLimitSeconds,
                       String instructions, Question question) {}
}
