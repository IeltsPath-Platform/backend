package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.service.KnowledgePointDirectory;
import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.port.QuestionEventLog.EventStats;
import com.ieltspath.learning.application.port.QuestionEventLog.RecentEvent;
import com.ieltspath.learning.application.result.LearnerReportResult;
import com.ieltspath.learning.application.result.LearnerReportResult.Action;
import com.ieltspath.learning.application.result.LearnerReportResult.ErrorEntry;
import com.ieltspath.learning.application.result.LearnerReportResult.EvidenceEntry;
import com.ieltspath.learning.application.result.LearnerReportResult.KnowledgePointReport;
import com.ieltspath.learning.application.result.LearnerReportResult.Score;
import com.ieltspath.learning.application.service.MasteryProfileUpdater;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.DiagnosticRepository;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.MasteryTier;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Builds the Score / Errors / Evidence / Action report from the stored mastery profiles. */
@Service
public class GetLearnerReportUseCase {
    private static final int RECENT_EVIDENCE = 5;

    private final MasteryProfileRepository profiles;
    private final QuestionEventLog events;
    private final ReviewItemRepository reviews;
    private final KnowledgePointDirectory directory;
    private final DiagnosticRepository diagnostics;
    private final Clock clock = Clock.systemUTC();

    public GetLearnerReportUseCase(MasteryProfileRepository profiles, QuestionEventLog events,
                                   ReviewItemRepository reviews, KnowledgePointDirectory directory,
                                   DiagnosticRepository diagnostics) {
        this.diagnostics = diagnostics;
        this.profiles = profiles;
        this.events = events;
        this.reviews = reviews;
        this.directory = directory;
    }

    @Transactional(readOnly = true)
    public LearnerReportResult execute(UUID userId) {
        Instant now = clock.instant();
        List<MasteryProfile> stored = profiles.findAll(userId);
        if (stored.isEmpty()) return new LearnerReportResult(userId, now, List.of());

        List<UUID> pointIds = stored.stream().map(MasteryProfile::knowledgePointId).toList();
        Map<UUID, EventStats> stats = events.statsByKnowledgePoint(userId, pointIds,
                now.minus(MasteryProfileUpdater.STATS_WINDOW));
        Map<UUID, List<RecentEvent>> recent = events.recentByKnowledgePoint(userId, pointIds, RECENT_EVIDENCE);
        Map<UUID, UUID> pendingReviews = new HashMap<>();
        for (PendingReview review : reviews.findPending(userId)) {
            if (!review.blocking()) continue;
            pendingReviews.putIfAbsent(review.knowledgePointId(), review.reviewId());
        }
        Set<UUID> diagnosing = diagnostics.probingRootKnowledgePoints(userId);
        Map<UUID, KnowledgePointDirectory.Entry> described = directory.describe();

        List<KnowledgePointReport> report = stored.stream().map(profile -> {
            var meta = described.getOrDefault(profile.knowledgePointId(), KnowledgePointDirectory.Entry.UNKNOWN);
            EventStats pointStats = stats.get(profile.knowledgePointId());
            return new KnowledgePointReport(profile.knowledgePointId(), meta.code(), meta.name(), meta.skill(),
                    meta.band(), new Score(profile.mastery(), profile.tier().name(), profile.evidenceCount()),
                    pointStats == null ? List.of() : pointStats.errors().stream()
                            .map(error -> new ErrorEntry(error.code(), error.count())).toList(),
                    recent.getOrDefault(profile.knowledgePointId(), List.of()).stream()
                            .map(event -> new EvidenceEntry(event.questionVersionId(), event.correct(),
                                    event.durationMs(), event.source().value(), event.errorCode(), event.at(),
                                    event.revisionNo(), event.revisionDelta()))
                            .toList(),
                    action(profile, pendingReviews.get(profile.knowledgePointId()),
                            diagnosing.contains(profile.knowledgePointId()), now));
        }).sorted(Comparator.<KnowledgePointReport>comparingInt(point -> MasteryTier.valueOf(point.score().tier()).ordinal())
                .thenComparingDouble(point -> point.score().mastery())
                .thenComparing(point -> point.code() == null ? "￿" : point.code())).toList();
        return new LearnerReportResult(userId, now, report);
    }

    private static Action action(MasteryProfile profile, UUID pendingReviewId, boolean diagnosing, Instant now) {
        if (pendingReviewId != null) {
            return new Action(diagnosing ? "DIAGNOSING" : "REVIEW_REQUIRED", pendingReviewId, null);
        }
        return switch (profile.tier()) {
            case PRIORITY -> new Action("NONE", null, profile.nextReviewAt());
            case CONSOLIDATE -> new Action("CONSOLIDATE", null, profile.nextReviewAt());
            case MAINTAIN -> profile.nextReviewAt() != null && !profile.nextReviewAt().isAfter(now)
                    ? new Action("MAINTAIN_DUE", null, profile.nextReviewAt())
                    : new Action("NONE", null, profile.nextReviewAt());
        };
    }
}
