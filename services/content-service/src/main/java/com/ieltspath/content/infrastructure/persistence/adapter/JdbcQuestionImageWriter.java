package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.QuestionImageWriter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JdbcQuestionImageWriter implements QuestionImageWriter {

    private final JdbcTemplate jdbc;

    public JdbcQuestionImageWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void replace(UUID questionVersionId, String mediaReference, String altText) {
        clear(questionVersionId);
        UUID assetId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_assets (id, asset_type, text_content, media_reference, validation_status) "
                + "VALUES (?, 'IMAGE', ?, ?, 'VALID')", assetId, altText, mediaReference);
        jdbc.update("INSERT INTO content_asset_links (id, asset_id, question_version_id, sort_order) VALUES (?, ?, ?, 0)",
                UUID.randomUUID(), assetId, questionVersionId);
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void copyFromPreviousVersion(UUID newQuestionVersionId) {
        // The version was just saved through JPA; its row must exist before a link can point at it.
        if (entityManager != null) entityManager.flush();
        jdbc.update("""
                INSERT INTO content_asset_links (id, asset_id, question_version_id, sort_order)
                SELECT gen_random_uuid(), l.asset_id, ?, 0
                FROM question_versions fresh
                JOIN question_versions previous ON previous.question_id = fresh.question_id AND previous.id <> fresh.id
                JOIN content_asset_links l ON l.question_version_id = previous.id
                JOIN content_assets a ON a.id = l.asset_id AND a.asset_type = 'IMAGE'
                WHERE fresh.id = ?
                ORDER BY previous.version_number DESC
                LIMIT 1
                """, newQuestionVersionId, newQuestionVersionId);
    }

    @Override
    public void clear(UUID questionVersionId) {
        jdbc.update("""
                DELETE FROM content_asset_links l
                USING content_assets a
                WHERE a.id = l.asset_id AND l.question_version_id = ? AND a.asset_type = 'IMAGE'
                """, questionVersionId);
    }
}
