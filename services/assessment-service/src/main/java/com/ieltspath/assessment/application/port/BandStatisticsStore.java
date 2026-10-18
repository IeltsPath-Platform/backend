package com.ieltspath.assessment.application.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read-only history of the latest completed result for each owned attempt. */
public interface BandStatisticsStore {
    Page history(UUID user, Instant from, Instant until, String kind, String skill, Cursor cursor, int limit);

    record Cursor(Instant submittedAt, UUID attemptId) {}

    record Point(UUID attemptId, UUID resultId, int resultVersion, Instant submittedAt, Instant gradedAt,
                 double band, boolean regradePending, String gradingSource, String measurementQuality) {}

    record Page(List<Point> points, Double baseline, Double latest, Instant dataFrom, long eligibleCount) {}
}
