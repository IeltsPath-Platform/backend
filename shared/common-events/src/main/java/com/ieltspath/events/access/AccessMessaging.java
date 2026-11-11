package com.ieltspath.events.access;

/** Access publishes subscription changes consumed by Notification; see docs/contracts/subscription-granted-v1.md. */
public final class AccessMessaging {
    public static final String EXCHANGE = "access.events";
    /** A learner received a granted Premium subscription; consumed by Notification. */
    public static final String SUBSCRIPTION_GRANTED_ROUTING_KEY = SubscriptionGrantedV1.EVENT_TYPE;
    /** A Premium subscription is about to end or has ended; consumed by Notification. */
    public static final String SUBSCRIPTION_EXPIRY_NOTICE_ROUTING_KEY = SubscriptionExpiryNoticeV1.EVENT_TYPE;

    private AccessMessaging() { }
}
