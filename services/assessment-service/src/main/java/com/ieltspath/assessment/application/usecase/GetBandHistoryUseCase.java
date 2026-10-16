package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.BandStatisticsStore;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class GetBandHistoryUseCase {
    private static final Set<String> SERIES_KINDS = Set.of("PLACEMENT", "MOCK", "OFFICIAL_PRACTICE");
    private static final Set<String> SKILLS = Set.of("LISTENING", "READING", "WRITING", "SPEAKING", "OVERALL");
    private final BandStatisticsStore store;
    private final AssessmentAttemptRepository attempts;

    public GetBandHistoryUseCase(BandStatisticsStore store, AssessmentAttemptRepository attempts) {
        this.store = store;
        this.attempts = attempts;
    }

    public record Result(String availability, Instant generatedAt, LocalDate dataFrom, String seriesKind, String skill,
                         String eligibilityReason, List<BandStatisticsStore.Point> points, String nextCursor,
                         Double baseline, Double latest, Double delta) {}

    public record StudyContext(UUID sourceId, UUID courseId) {}

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Result execute(UUID user, LocalDate from, LocalDate to, String kind, String skill,
                          String cursor, int limit, String timezone) {
        if (from == null || to == null || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 365
                || limit < 1 || limit > 100 || kind == null || !SERIES_KINDS.contains(kind)
                || skill == null || !SKILLS.contains(skill)) {
            throw new IllegalArgumentException("Invalid band range, series, skill or page size");
        }
        if (timezone == null || !ZoneId.getAvailableZoneIds().contains(timezone)) {
            throw new IllegalArgumentException("Invalid IANA timezone");
        }
        ZoneId zone = ZoneId.of(timezone);
        var position = decode(cursor);
        if ("PLACEMENT".equals(kind) && Set.of("SPEAKING", "OVERALL").contains(skill)) {
            return new Result("UNAVAILABLE", Instant.now(), null, kind, skill,
                    "PLACEMENT_" + skill + "_UNAVAILABLE", List.of(), null, null, null, null);
        }
        var page = store.history(user, from.atStartOfDay(zone).toInstant(),
                to.plusDays(1).atStartOfDay(zone).toInstant(), kind, skill, position, limit + 1);
        var points = page.points().size() > limit ? page.points().subList(0, limit) : page.points();
        String next = page.points().size() > limit ? encode(points.getLast()) : null;
        String reason = page.latest() != null ? null
                : "OVERALL".equals(skill) ? "FOUR_GRADED_SKILLS_REQUIRED" : "NO_ELIGIBLE_RESULTS";
        return new Result(page.latest() == null ? "UNAVAILABLE" : "AVAILABLE", Instant.now(),
                page.dataFrom() == null ? null : page.dataFrom().atZone(zone).toLocalDate(),
                kind, skill, reason, points, next, page.baseline(), page.latest(),
                page.eligibleCount() < 2 || page.baseline() == null || page.latest() == null
                        ? null : page.latest() - page.baseline());
    }

    private BandStatisticsStore.Cursor decode(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank() || value.length() > 200) {
            throw new IllegalArgumentException("Invalid cursor");
        }
        try {
            var parts = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8).split("\\|", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }
            return new BandStatisticsStore.Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

    private String encode(BandStatisticsStore.Point point) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (point.submittedAt() + "|" + point.attemptId()).getBytes(StandardCharsets.UTF_8));
    }

    @Transactional(readOnly = true)
    public StudyContext context(UUID user, String sourceType, UUID sourceId) {
        if (!"ASSESSMENT".equals(sourceType)) {
            throw new IllegalArgumentException("Invalid source type");
        }
        var attempt = attempts.findByIdAndUserId(sourceId, user)
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment not found"));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS
                || attempt.getExpiresAt() != null && !Instant.now().isBefore(attempt.getExpiresAt())) {
            throw new AssessmentNotFoundException("Active assessment not found");
        }
        return new StudyContext(sourceId, null);
    }
}
