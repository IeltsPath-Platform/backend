package com.ieltspath.community.domain.repository;

import com.ieltspath.community.domain.vo.CommunityPage;

import java.util.Set;
import java.util.UUID;

public interface UserBlockRepository {
    boolean block(UUID blockerId, UUID blockedId);

    void unblock(UUID blockerId, UUID blockedId);

    boolean isBlockedEitherWay(UUID userA, UUID userB);

    Set<UUID> findBlockedEitherWayUserIds(UUID viewerId);

    CommunityPage<UUID> findBlockedUsers(UUID blockerId, int page, int size);
}
