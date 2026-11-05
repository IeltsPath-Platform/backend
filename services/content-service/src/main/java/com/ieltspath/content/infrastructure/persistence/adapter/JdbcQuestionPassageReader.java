package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.QuestionPassageReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JdbcQuestionPassageReader implements QuestionPassageReader {

    /** More than this many passages for one question is not a passage but a mix-up; the rest are left out. */
    private static final int LIMIT = 5;

    private final JdbcTemplate jdbc;

    public JdbcQuestionPassageReader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Passage> passagesOf(UUID questionVersionId) {
        // The same version id feeds the three branches: a direct link, the nearest ASSET block above the exercise
        // block that holds the version, and the passages linked to a section that holds the version.
        List<Passage> found = jdbc.query("""
                SELECT asset_id, text_content, source, source_name FROM (
                  SELECT a.id AS asset_id, a.text_content, 'DIRECT' AS source, NULL AS source_name, 0 AS rank, 0 AS position
                  FROM content_asset_links link
                  JOIN content_assets a ON a.id = link.asset_id
                  WHERE link.question_version_id = ? AND a.asset_type = 'PASSAGE' AND a.text_content IS NOT NULL
                  UNION ALL
                  SELECT a.id, a.text_content, 'LESSON', lesson.title, 1, 0
                  FROM lesson_block_questions held
                  JOIN lesson_blocks exercise ON exercise.id = held.block_id
                  JOIN lessons lesson ON lesson.id = exercise.lesson_id
                  JOIN LATERAL (
                    SELECT above.asset_id FROM lesson_blocks above
                    WHERE above.lesson_id = exercise.lesson_id AND above.block_type = 'ASSET' AND above.sort_order < exercise.sort_order
                    ORDER BY above.sort_order DESC LIMIT 1) nearest ON TRUE
                  JOIN content_assets a ON a.id = nearest.asset_id
                  WHERE held.question_version_id = ? AND a.asset_type = 'PASSAGE' AND a.text_content IS NOT NULL
                  UNION ALL
                  SELECT a.id, a.text_content, 'SECTION', section.title, 2, link.sort_order
                  FROM section_questions placed
                  JOIN content_sections section ON section.id = placed.section_id
                  JOIN content_asset_links link ON link.section_id = section.id
                  JOIN content_assets a ON a.id = link.asset_id
                  WHERE placed.question_version_id = ? AND a.asset_type = 'PASSAGE' AND a.text_content IS NOT NULL
                ) found ORDER BY rank, position
                """, (rs, row) -> new Passage(rs.getObject("asset_id", UUID.class), rs.getString("text_content"),
                Source.valueOf(rs.getString("source")), rs.getString("source_name")),
                questionVersionId, questionVersionId, questionVersionId);
        Map<UUID, Passage> once = new LinkedHashMap<>();
        found.forEach(passage -> once.putIfAbsent(passage.assetId(), passage));
        return once.values().stream().limit(LIMIT).toList();
    }
}
