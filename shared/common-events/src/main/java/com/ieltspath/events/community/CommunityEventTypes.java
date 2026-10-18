package com.ieltspath.events.community;

/** Community publishes events consumed by Notification; see docs/contracts/community-events-v1.md. */
public final class CommunityEventTypes {
    public static final String POST_COMMENTED = "community.post-commented.v1";
    public static final String COMMENT_REPLIED = "community.comment-replied.v1";
    public static final String COMMENT_MENTIONED = "community.comment-mentioned.v1";
    public static final String POST_REACTED = "community.post-reacted.v1";
    public static final String USER_FOLLOWED = "community.user-followed.v1";
    /** In the order notification preferences list them. */
    public static final java.util.List<String> ORDERED = java.util.List.of(
            POST_COMMENTED, COMMENT_REPLIED, COMMENT_MENTIONED, POST_REACTED, USER_FOLLOWED);
    public static final java.util.Set<String> ALL = java.util.Set.copyOf(ORDERED);

    private CommunityEventTypes() { }
}
