package com.ieltspath.events.community;

/** Community publishes events consumed by Notification; see docs/contracts/community-events-v1.md. */
public final class CommunityMessaging {
    public static final String EXCHANGE = "community.events";
    /** The queue Notification consumes community events from. */
    public static final String NOTIFICATIONS_QUEUE = "community.notifications.v1";
    /** Where Notification parks community events it cannot use; a replay sends them back to {@link #NOTIFICATIONS_QUEUE}. */
    public static final String NOTIFICATIONS_DLQ = "community.notifications.v1.dlq";

    private CommunityMessaging() { }
}
