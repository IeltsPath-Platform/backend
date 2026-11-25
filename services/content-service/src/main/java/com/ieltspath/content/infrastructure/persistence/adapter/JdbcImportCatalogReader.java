package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.ImportCatalogReader;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

    @Override
    public Set<String> existingCourseCodes(Collection<String> codes) {
        return existing("SELECT code FROM courses WHERE code IN (:values)", codes);
    }

    @Override
    public Set<String> existingTopicCodes(Collection<String> codes) {
        return existing("SELECT code FROM topics WHERE code IN (:values)", codes);
    }

    @Override
    public Set<String> existingLessonCodes(Collection<String> codes) {
        return existing("SELECT code FROM lessons WHERE code IN (:values)", codes);
    }

    @Override
    public Set<String> existingBandLevels(Collection<BigDecimal> levels) {
        if (levels.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.query("SELECT band_level FROM courses WHERE band_level IN (:values)",
                Map.of("values", List.copyOf(levels)), (rs, i) -> rs.getBigDecimal(1).stripTrailingZeros().toPlainString()));
    }

    @Override
    public Set<Integer> existingTopicSortOrders(Collection<Integer> orders) {
        if (orders.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.query("SELECT sort_order FROM topics WHERE sort_order IN (:values)",
                Map.of("values", List.copyOf(orders)), (rs, i) -> rs.getInt(1)));
    }

    @Override
    public Map<String, String> existingQuestionSkills(Collection<String> refs) {
        if (refs.isEmpty()) return Map.of();
        Map<String, String> skills = new HashMap<>();
        jdbc.query("SELECT import_ref, skill FROM questions WHERE import_ref IN (:refs)", Map.of("refs", List.copyOf(refs)),
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> skills.put(rs.getString("import_ref"), rs.getString("skill")));
        return skills;
    }

    private Set<String> existing(String sql, Collection<String> values) {
        if (values.isEmpty()) return Set.of();
        return new HashSet<>(jdbc.query(sql, Map.of("values", List.copyOf(values)), (rs, i) -> rs.getString(1)));
    }
}
