package com.ieltspath.community.domain.repository;
import com.ieltspath.community.domain.aggregate.*;
import com.ieltspath.community.domain.vo.*;
import java.util.*;
public interface CommunityReadRepository {
 List<Post> feed(UUID viewer, UUID author, boolean bookmarked, boolean followingOnly, PostCategory category, FeedCursor cursor,int limit);
 List<Post> search(UUID viewer, boolean followingOnly, PostCategory category, String query, FeedCursor cursor, int limit);
 Map<UUID,PostInteractions> interactions(Set<UUID> postIds,UUID viewer);
 List<FollowEntry> follows(UUID user,boolean followers,FeedCursor cursor,int limit);
 Relationship relationship(UUID viewer,UUID user);
 CommunityPage<Comment> thread(UUID post,UUID parent,int page,int size);
 Map<UUID,Long> replyCounts(Set<UUID> commentIds);
}
