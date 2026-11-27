package com.ieltspath.community.infrastructure.adapter;

import com.ieltspath.community.domain.repository.UserBlockRepository;
import com.ieltspath.community.domain.vo.CommunityPage;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserBlockRepositoryAdapter implements UserBlockRepository {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public boolean block(UUID blockerId, UUID blockedId) {
        return jdbc.update(
                "INSERT INTO user_blocks (blocker_id, blocked_id) VALUES (:blocker, :blocked) ON CONFLICT DO NOTHING",
                new MapSqlParameterSource().addValue("blocker", blockerId).addValue("blocked", blockedId)
        ) > 0;
    }

    @Override
    public void unblock(UUID blockerId, UUID blockedId) {
        jdbc.update(
                "DELETE FROM user_blocks WHERE blocker_id = :blocker AND blocked_id = :blocked",
                new MapSqlParameterSource().addValue("blocker", blockerId).addValue("blocked", blockedId)
        );
    }

    @Override
    public boolean isBlockedEitherWay(UUID userA, UUID userB) {
        if (userA == null || userB == null || userA.equals(userB)) {
            return false;
        }
        Boolean exists = jdbc.queryForObject(
                """
                SELECT EXISTS(
                    SELECT 1 FROM user_blocks
                    WHERE (blocker_id = :userA AND blocked_id = :userB)
                       OR (blocker_id = :userB AND blocked_id = :userA)
                )
                """,
                new MapSqlParameterSource().addValue("userA", userA).addValue("userB", userB),
                Boolean.class
        );
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Set<UUID> findBlockedEitherWayUserIds(UUID viewerId) {
        var ids = jdbc.query(
                """
                SELECT blocked_id AS other_id FROM user_blocks WHERE blocker_id = :viewer
                UNION
                SELECT blocker_id AS other_id FROM user_blocks WHERE blocked_id = :viewer
                """,
                new MapSqlParameterSource().addValue("viewer", viewerId),
                (rs, rowNum) -> rs.getObject("other_id", UUID.class)
        );
        return new HashSet<>(ids);
    }

    @Override
    public CommunityPage<UUID> findBlockedUsers(UUID blockerId, int page, int size) {
        var args = new MapSqlParameterSource()
                .addValue("blocker", blockerId)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);
        var items = jdbc.query(
                """
                SELECT blocked_id
                FROM user_blocks
                WHERE blocker_id = :blocker
                ORDER BY created_at DESC, blocked_id DESC
                LIMIT :limit OFFSET :offset
                """,
                args,
                (rs, rowNum) -> rs.getObject("blocked_id", UUID.class)
        );
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM user_blocks WHERE blocker_id = :blocker",
                args,
                Long.class
        );
        return new CommunityPage<>(items, total == null ? 0L : total);
    }
}
