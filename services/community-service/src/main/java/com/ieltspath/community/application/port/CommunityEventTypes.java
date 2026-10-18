package com.ieltspath.community.application.port;

import java.util.Set;

public final class CommunityEventTypes {
    public static final String POST_COMMENTED = "community.post-commented.v1";
    public static final String COMMENT_REPLIED = "community.comment-replied.v1";
    public static final String COMMENT_MENTIONED = "community.comment-mentioned.v1";
    public static final String POST_REACTED = "community.post-reacted.v1";
    public static final String USER_FOLLOWED = "community.user-followed.v1";
    public static final Set<String> ALL = Set.of(POST_COMMENTED, COMMENT_REPLIED, COMMENT_MENTIONED,
            POST_REACTED, USER_FOLLOWED);

    private CommunityEventTypes() { }
}
