package com.ieltspath.notification.application;

import java.util.List;
import java.util.Set;

public final class NotificationEventTypes {
    public static final String POST_COMMENTED = "community.post-commented.v1";
    public static final String COMMENT_REPLIED = "community.comment-replied.v1";
    public static final String COMMENT_MENTIONED = "community.comment-mentioned.v1";
    public static final String POST_REACTED = "community.post-reacted.v1";
    public static final String USER_FOLLOWED = "community.user-followed.v1";
    public static final List<String> ORDERED = List.of(POST_COMMENTED, COMMENT_REPLIED, COMMENT_MENTIONED,
            POST_REACTED, USER_FOLLOWED);
    public static final Set<String> ALL = Set.copyOf(ORDERED);

    private NotificationEventTypes() { }
}
