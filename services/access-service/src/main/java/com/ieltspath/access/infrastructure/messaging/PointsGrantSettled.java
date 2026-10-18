package com.ieltspath.access.infrastructure.messaging;

import java.util.UUID;

public record PointsGrantSettled(UUID grantId, String status, String error) {
    public static PointsGrantSettled granted(UUID grantId) {
        return new PointsGrantSettled(grantId, "GRANTED", null);
    }

    public static PointsGrantSettled capped(UUID grantId) {
        return new PointsGrantSettled(grantId, "CAPPED", null);
    }

    public static PointsGrantSettled failed(UUID grantId, String error) {
        return new PointsGrantSettled(grantId, "FAILED", error);
    }
}
