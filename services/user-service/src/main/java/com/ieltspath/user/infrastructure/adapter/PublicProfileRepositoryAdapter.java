package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.user.domain.repository.PublicProfileRepository;
import com.ieltspath.user.domain.vo.ProfileVisibility;
import com.ieltspath.user.domain.vo.PublicProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PublicProfileRepositoryAdapter implements PublicProfileRepository {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<PublicProfile> findByUserIds(Set<UUID> userIds) {
        if (userIds.isEmpty()) return List.of();
        if (userIds.size() > 100) throw new IllegalArgumentException("At most 100 user IDs are allowed");
        // The profile primary key keeps this one row per user.
        return jdbc.query("""
                SELECT u.id, u.full_name, p.display_name, p.avatar_reference, p.bio, p.profile_visibility
                FROM users u
                LEFT JOIN learner_profiles p ON p.user_id = u.id
                WHERE u.status = 'ACTIVE'
                  AND u.id IN (:userIds)
                """, Map.of("userIds", userIds), (row, index) -> new PublicProfile(
                row.getObject("id", UUID.class), row.getString("display_name"), row.getString("full_name"),
                row.getString("avatar_reference"), row.getString("bio"),
                row.getString("profile_visibility") == null ? null
                        : ProfileVisibility.valueOf(row.getString("profile_visibility"))));
    }

    @Override
    public List<PublicProfile> searchVisibleByName(UUID viewerId, String query, int limit) {
        String escapedQuery = "%" + escapeLike(query.toLowerCase()) + "%";
        return jdbc.query("""
                SELECT u.id, u.full_name, p.display_name, p.avatar_reference, p.bio, p.profile_visibility
                FROM users u
                LEFT JOIN learner_profiles p ON p.user_id = u.id
                WHERE u.status = 'ACTIVE'
                  AND (p.profile_visibility IN ('PUBLIC','COMMUNITY') OR u.id = :viewerId)
                  AND lower(coalesce(nullif(trim(p.display_name), ''), u.full_name)) LIKE :query ESCAPE '\\'
                ORDER BY lower(coalesce(nullif(trim(p.display_name), ''), u.full_name)), u.id
                LIMIT :limit
                """, Map.of("viewerId", viewerId, "query", escapedQuery, "limit", limit),
                (row, index) -> new PublicProfile(row.getObject("id", UUID.class), row.getString("display_name"),
                        row.getString("full_name"), row.getString("avatar_reference"), row.getString("bio"),
                        row.getString("profile_visibility") == null ? null
                                : ProfileVisibility.valueOf(row.getString("profile_visibility"))));
    }

    private String escapeLike(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
