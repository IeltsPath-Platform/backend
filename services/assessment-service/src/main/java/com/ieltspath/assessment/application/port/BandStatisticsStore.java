package com.ieltspath.assessment.application.port;

import com.ieltspath.assessment.domain.vo.BandMeasurement;
import com.ieltspath.assessment.domain.vo.Skill;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface BandStatisticsStore {
    void freeze(UUID resultId, List<BandMeasurement> snapshot);
    void markGradedSkills(UUID resultId, Collection<Skill> skills, boolean verified);
    Set<Skill> verifiedSkills(UUID resultId);
    Page history(UUID user, Instant from, Instant until, String kind, String skill, Cursor cursor, int limit);
    record Cursor(Instant submittedAt, UUID attemptId) {}
    record Point(UUID attemptId, int resultVersion, Instant submittedAt, double band, boolean pendingRegrade,
                 String gradingSource, String measurementQuality, String scoringVersion) {}
    record Page(List<Point> points, Double baseline, Double latest, Instant dataFrom) {}
}
