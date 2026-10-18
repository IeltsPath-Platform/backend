package com.ieltspath.community.application.port;
import com.ieltspath.community.application.result.PublicProfile;
import java.util.*;
public interface PublicProfileClient {
 Map<UUID,PublicProfile> findProfiles(Set<UUID> ids);
 default java.util.List<PublicProfile> searchProfiles(UUID viewerId, String query, int limit) { return java.util.List.of(); }
}
