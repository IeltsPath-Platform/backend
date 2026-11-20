package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.CodeSettingsStore;
import com.ieltspath.content.application.result.CodeSetting;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcCodeSettingsStore implements CodeSettingsStore {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcCodeSettingsStore(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<CodeSetting> find(String kind) {
        return jdbc.query("SELECT code_kind, applied, pattern, start_number, editable FROM code_settings WHERE code_kind = :kind",
                new MapSqlParameterSource("kind", kind), rs -> rs.next()
                        ? Optional.of(new CodeSetting(rs.getString("code_kind"), rs.getBoolean("applied"),
                        rs.getString("pattern"), rs.getLong("start_number"), rs.getBoolean("editable")))
                        : Optional.empty());
    }

    @Override
    public void save(CodeSetting setting, UUID updatedBy) {
        jdbc.update("""
                INSERT INTO code_settings(code_kind, applied, pattern, start_number, editable, updated_by, updated_at)
                VALUES (:kind, :applied, :pattern, :start, :editable, :updatedBy, NOW())
                ON CONFLICT (code_kind) DO UPDATE SET applied = EXCLUDED.applied, pattern = EXCLUDED.pattern,
                start_number = EXCLUDED.start_number, editable = EXCLUDED.editable, updated_by = EXCLUDED.updated_by,
                updated_at = NOW()
                """, new MapSqlParameterSource().addValue("kind", setting.kind()).addValue("applied", setting.applied())
                .addValue("pattern", setting.pattern()).addValue("start", setting.startNumber())
                .addValue("editable", setting.editable()).addValue("updatedBy", updatedBy));
    }

    @Override
    public long allocate(String kind, String scopeKey, long startNumber) {
        Long allocated = jdbc.queryForObject("""
                INSERT INTO code_counters(code_kind, scope_key, next_number)
                VALUES (:kind, :scope, :start + 1)
                ON CONFLICT (code_kind, scope_key) DO UPDATE SET next_number = code_counters.next_number + 1
                RETURNING next_number - 1
                """, new MapSqlParameterSource().addValue("kind", kind).addValue("scope", scopeKey)
                .addValue("start", startNumber), Long.class);
        return allocated == null ? startNumber : allocated;
    }

    @Override
    public long peek(String kind, String scopeKey, long startNumber) {
        Long next = jdbc.queryForObject("SELECT COALESCE((SELECT next_number FROM code_counters WHERE code_kind = :kind AND scope_key = :scope), :start)",
                new MapSqlParameterSource().addValue("kind", kind).addValue("scope", scopeKey).addValue("start", startNumber), Long.class);
        return next == null ? startNumber : next;
    }

    @Override
    public void reset(String kind, String scopeKey, long nextNumber) {
        jdbc.update("""
                INSERT INTO code_counters(code_kind, scope_key, next_number) VALUES (:kind, :scope, :next)
                ON CONFLICT (code_kind, scope_key) DO UPDATE SET next_number = EXCLUDED.next_number
                """, new MapSqlParameterSource().addValue("kind", kind).addValue("scope", scopeKey)
                .addValue("next", nextNumber));
    }
}
