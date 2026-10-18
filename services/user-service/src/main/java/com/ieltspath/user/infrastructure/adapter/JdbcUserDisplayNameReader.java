package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.application.port.UserDisplayNameReader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcUserDisplayNameReader implements UserDisplayNameReader {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Map<UUID, String> findDisplayNamesByIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        var params = new MapSqlParameterSource("ids", userIds.stream().distinct().toList());
        Map<UUID, String> names = new LinkedHashMap<>();
        jdbc.query("""
                SELECT u.id, COALESCE(NULLIF(BTRIM(profile.display_name), ''), u.full_name) AS display_name
                FROM users u
                LEFT JOIN learner_profiles profile ON profile.user_id = u.id
                WHERE u.id IN (:ids)
                """, params, rs -> {
            names.put(rs.getObject("id", UUID.class), rs.getString("display_name"));
        });
        return Map.copyOf(names);
    }
}
