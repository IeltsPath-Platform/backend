package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.ImportCatalogReader;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class JdbcImportCatalogReader implements ImportCatalogReader {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcImportCatalogReader(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, UUID> activeKnowledgePoints(Collection<String> codes) {
        Map<String, UUID> points = new HashMap<>();
        if (codes.isEmpty()) return points;
        jdbc.query("SELECT id, code FROM knowledge_points WHERE status = 'ACTIVE' AND code IN (:codes)",
                Map.of("codes", List.copyOf(codes)), rs -> {
                    points.put(rs.getString("code"), rs.getObject("id", UUID.class));
                });
        return points;
    }

    @Override
    public Set<String> existingImportRefs(Collection<String> refs) {
        return existing("SELECT import_ref FROM questions WHERE import_ref IN (:values)", refs);
    }

    @Override
    public Set<String> existingPackageCodes(Collection<String> codes) {
        return existing("SELECT code FROM content_packages WHERE code IN (:values)", codes);
    }

    private Set<String> existing(String sql, Collection<String> values) {
        if (values.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.query(sql, Map.of("values", List.copyOf(values)), (rs, i) -> rs.getString(1)));
    }
}
