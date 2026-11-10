package com.ieltspath.notification.application;

import com.ieltspath.events.community.CommunityEventTypes;

import java.util.List;
import java.util.Set;

/** Event types that Notification stores and exposes as user preferences. */
public final class NotificationEventTypes {
    public static final String GRADING_REQUESTED = "assessment.grading-requested.v1";
    public static final String GRADING_DUE_SOON = "assessment.grading-due-soon.v1";
    public static final String GRADING_OVERDUE = "assessment.grading-overdue.v1";
    public static final String GRADING_COMPLETED = "assessment.grading-completed.v1";
    public static final String SUBSCRIPTION_GRANTED = "access.subscription-granted.v1";

    public static final List<String> ORDERED = List.of(
            CommunityEventTypes.POST_COMMENTED,
            CommunityEventTypes.COMMENT_REPLIED,
            CommunityEventTypes.COMMENT_MENTIONED,
            CommunityEventTypes.POST_REACTED,
            CommunityEventTypes.USER_FOLLOWED,
            GRADING_REQUESTED,
            GRADING_DUE_SOON,
            GRADING_OVERDUE,
            GRADING_COMPLETED,
            SUBSCRIPTION_GRANTED);
    public static final Set<String> ALL = Set.copyOf(ORDERED);

    private NotificationEventTypes() { }
}
