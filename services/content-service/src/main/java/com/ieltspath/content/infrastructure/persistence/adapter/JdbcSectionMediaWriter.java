package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.SectionMediaWriter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JdbcSectionMediaWriter implements SectionMediaWriter {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcSectionMediaWriter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void setPassage(UUID sectionId, String text) {
        unlink(sectionId, "PASSAGE");
        UUID assetId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO content_assets (id, asset_type, text_content, validation_status)
                VALUES (:id, 'PASSAGE', :text, 'VALID')
                """, new MapSqlParameterSource("id", assetId).addValue("text", text));
        link(sectionId, assetId);
    }

    @Override
    public void clearPassage(UUID sectionId) {
        unlink(sectionId, "PASSAGE");
    }

    @Override
    public UUID setAudio(UUID sectionId, String mediaReference, int durationSeconds, String transcript) {
        unlink(sectionId, "AUDIO");
        UUID assetId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds,
                                            validation_status)
                VALUES (:id, 'AUDIO', :transcript, :reference, :duration, 'VALID')
                """, new MapSqlParameterSource("id", assetId).addValue("transcript", transcript)
                .addValue("reference", mediaReference).addValue("duration", durationSeconds));
        link(sectionId, assetId);
        return assetId;
    }

    @Override
    public void clearAudio(UUID sectionId) {
        unlink(sectionId, "AUDIO");
    }

    private void unlink(UUID sectionId, String assetType) {
        jdbc.update("""
                DELETE FROM content_asset_links l
                USING content_assets a
                WHERE a.id = l.asset_id AND l.section_id = :section AND a.asset_type = :type
                """, new MapSqlParameterSource("section", sectionId).addValue("type", assetType));
    }

    private void link(UUID sectionId, UUID assetId) {
        jdbc.update("""
                INSERT INTO content_asset_links (id, asset_id, section_id, sort_order)
                VALUES (:id, :asset, :section, 0)
                """, new MapSqlParameterSource("id", UUID.randomUUID()).addValue("asset", assetId)
                .addValue("section", sectionId));
    }
}
