package com.ieltspath.user.domain.repository;

import com.ieltspath.user.domain.vo.PublicProfile;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PublicProfileRepository {
    List<PublicProfile> findByUserIds(Set<UUID> userIds);
    List<PublicProfile> searchVisibleByName(UUID viewerId, String query, int limit);
}
