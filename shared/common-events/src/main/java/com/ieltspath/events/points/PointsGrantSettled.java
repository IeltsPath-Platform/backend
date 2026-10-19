package com.ieltspath.events.points;

import java.util.Set;
import java.util.UUID;

/** Access's answer to a grant from Learning. */
public record PointsGrantSettled(UUID grantId, String status, String error) {
    public static final String GRANTED = "GRANTED";
    public static final String CAPPED = "CAPPED";
    public static final String FAILED = "FAILED";
    /** The statuses a consumer must accept. */
    public static final Set<String> STATUSES = Set.of(GRANTED, CAPPED, FAILED);

    public static PointsGrantSettled granted(UUID grantId) {
        return new PointsGrantSettled(grantId, GRANTED, null);
    }

    public static PointsGrantSettled capped(UUID grantId) {
        return new PointsGrantSettled(grantId, CAPPED, null);
    }

    public static PointsGrantSettled failed(UUID grantId, String error) {
        return new PointsGrantSettled(grantId, FAILED, error);
    }
}
