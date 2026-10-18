package com.ieltspath.events.points;

/** Learning publishes requests, Access publishes settlements; see docs/contracts/access-points-v1.md. */
public final class PointsGrantMessaging {
    public static final String EXCHANGE = "learning.points";
    public static final String REQUESTED_ROUTING_KEY = "points.grant.requested";
    public static final String SETTLED_ROUTING_KEY = "points.grant.settled";
    /** Both consumers park failures for this long and give up after MAX_RETRIES. */
    public static final long RETRY_DELAY_MS = 30_000;
    public static final int MAX_RETRIES = 5;

    private PointsGrantMessaging() { }
}
