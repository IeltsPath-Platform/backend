package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.AiDraftOriginReader;
import com.ieltspath.content.application.result.AiDraftOrigin;
import com.ieltspath.content.domain.aggregate.AiDraft;
import com.ieltspath.content.domain.vo.DraftKind;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class JdbcAiDraftOriginReader implements AiDraftOriginReader {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAiDraftOriginReader(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Map<UUID, AiDraftOrigin> findOrigins(List<AiDraft> drafts) {
        if (drafts.isEmpty()) return Map.of();
        Map<UUID, AiDraftOrigin> result = new LinkedHashMap<>();
        jdbc.query("""
                SELECT d.id AS draft_id, d.kind,
                       NULLIF(d.request ->> 'lessonId', '')::uuid AS requested_lesson_id,
                       NULLIF(d.request ->> 'sectionId', '')::uuid AS requested_section_id,
                       NULLIF(d.request ->> 'versionId', '')::uuid AS requested_version_id,
                       NULLIF(d.request ->> 'packageId', '')::uuid AS requested_package_id,
                       l.id AS lesson_id, l.title AS lesson_title, t.id AS topic_id,
                       s.id AS section_id, s.title AS section_title,
                       v.id AS version_id, p.id AS package_id, p.title AS package_title,
                       kp.name AS knowledge_point_name
                FROM ai_drafts d
                LEFT JOIN lessons l ON d.kind = 'QUESTIONS'
                    AND l.id = NULLIF(d.request ->> 'lessonId', '')::uuid
                LEFT JOIN topics t ON t.id = l.topic_id
                LEFT JOIN knowledge_points kp ON kp.id = NULLIF(d.request ->> 'knowledgePointId', '')::uuid
                LEFT JOIN content_sections s ON d.kind = 'SECTION_QUESTIONS'
                    AND s.id = NULLIF(d.request ->> 'sectionId', '')::uuid
                LEFT JOIN content_package_versions v ON v.id = s.package_version_id
                    AND v.id = NULLIF(d.request ->> 'versionId', '')::uuid
                LEFT JOIN content_packages p ON p.id = v.package_id
                    AND p.id = NULLIF(d.request ->> 'packageId', '')::uuid
                WHERE d.id IN (:ids)
                """, Map.of("ids", drafts.stream().map(AiDraft::getId).toList()), rs -> {
            AiDraftOrigin origin = map(rs);
            if (origin != null) result.put(rs.getObject("draft_id", UUID.class), origin);
        });
        return Map.copyOf(result);
    }

    private static AiDraftOrigin map(ResultSet rs) throws SQLException {
        DraftKind kind = DraftKind.valueOf(rs.getString("kind"));
        if (kind != DraftKind.QUESTIONS && kind != DraftKind.SECTION_QUESTIONS) return null;
        UUID lessonId = rs.getObject("requested_lesson_id", UUID.class);
        UUID sectionId = rs.getObject("requested_section_id", UUID.class);
        UUID packageId = rs.getObject("requested_package_id", UUID.class);
        UUID versionId = rs.getObject("requested_version_id", UUID.class);
        String knowledgePointName = rs.getString("knowledge_point_name");
        if (kind == DraftKind.SECTION_QUESTIONS) {
            boolean exists = rs.getObject("section_id", UUID.class) != null && rs.getObject("package_id", UUID.class) != null && rs.getObject("version_id", UUID.class) != null;
            String title = exists ? "Phần " + rs.getString("section_title") + " · " + rs.getString("package_title") : "Phần của đề không còn tồn tại";
            return new AiDraftOrigin("SECTION", title, packageId, versionId, sectionId, null, null, exists, null);
        }
        if (kind == DraftKind.QUESTIONS && rs.getString("lesson_title") != null) {
            String lessonTitle = rs.getString("lesson_title");
            String title = "Bài " + lessonTitle + (knowledgePointName == null ? "" : " · " + knowledgePointName);
            return new AiDraftOrigin("LESSON", title, null, null, null, rs.getObject("topic_id", UUID.class), lessonId, true, knowledgePointName);
        }
        boolean lessonRequest = lessonId != null;
        return new AiDraftOrigin(lessonRequest ? "LESSON" : "BANK",
                lessonRequest ? "Bài học không còn tồn tại" : "Ngân hàng câu hỏi", null, null, null,
                null, lessonRequest ? lessonId : null, !lessonRequest, knowledgePointName);
    }
}
