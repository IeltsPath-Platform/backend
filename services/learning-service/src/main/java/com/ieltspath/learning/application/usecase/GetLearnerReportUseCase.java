package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearnerGoalClient;
import com.ieltspath.learning.application.result.LearnerReportResult.EstimatedBand;
import com.ieltspath.learning.application.result.LearnerReportResult.FocusPoint;
import com.ieltspath.learning.application.result.LearnerReportResult.NextBandHint;
import com.ieltspath.learning.application.result.LearnerReportResult.SkillSummary;
import com.ieltspath.learning.application.result.LearnerReportResult.Target;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.service.BandEstimator;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Builds the Score / Errors / Evidence / Action report from the stored mastery profiles. */
@Service
public class GetLearnerReportUseCase {
    private static final int RECENT_EVIDENCE = 5;
    private static final int FOCUS_POINTS = 3;
    /** The six blocks of the skill view: the four skills, then grammar and vocabulary. */
    private static final List<String> SUMMARY_ORDER =
            List.of("LISTENING", "READING", "WRITING", "SPEAKING", "GRAMMAR", "VOCABULARY");

    private final MasteryProfileRepository profiles;
    private final QuestionEventLog events;
    private final ReviewItemRepository reviews;
    private final KnowledgePointDirectory directory;
    private final DiagnosticRepository diagnostics;
    private final LearnerPlacementRepository placements;
    private final LearnerGoalClient goals;
    private final Clock clock = Clock.systemUTC();

    public GetLearnerReportUseCase(MasteryProfileRepository profiles, QuestionEventLog events,
                                   ReviewItemRepository reviews, KnowledgePointDirectory directory,
                                   DiagnosticRepository diagnostics, LearnerPlacementRepository placements,
                                   LearnerGoalClient goals) {
        this.diagnostics = diagnostics;
        this.placements = placements;
        this.goals = goals;
        this.profiles = profiles;
        this.events = events;
        this.reviews = reviews;
        this.directory = directory;
    }

    @Transactional(readOnly = true)
    public LearnerReportResult execute(UUID userId) {
        Instant now = clock.instant();
        List<MasteryProfile> stored = profiles.findAll(userId);
        if (stored.isEmpty()) return withSummary(userId, now, List.of());

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
        return withSummary(userId, now, report);
    }

    /** Adds the per-skill view, the points to focus on, the estimated band and the learner's target to the report. */
    private LearnerReportResult withSummary(UUID userId, Instant generatedAt, List<KnowledgePointReport> report) {
        BigDecimal placement = placements.find(userId).map(found -> found.band().value()).orElse(null);
        List<BandEstimator.Point> points = report.stream().filter(point -> point.skill() != null)
                .map(point -> new BandEstimator.Point(point.knowledgeId(), point.skill().toUpperCase(Locale.ROOT),
                        point.band(), MasteryTier.valueOf(point.score().tier()), point.score().mastery()))
                .toList();
        BandEstimator.Estimate estimate = BandEstimator.estimate(points, placement);
        EstimatedBand band = new EstimatedBand(estimate.overall(), true, estimate.speakingIncluded(),
                estimate.hints().stream()
                        .map(hint -> new NextBandHint(hint.skill(), hint.nextBand(), hint.pointIds())).toList());
        // One call to the User Service per report, outside any loop; no goal simply leaves the target out.
        Target target = goals.goal(userId).filter(goal -> goal.targetBand() != null || goal.examDate() != null)
                .map(goal -> new Target(goal.targetBand(),
                        goal.targetBand() == null || estimate.overall() == null ? null
                                : goal.targetBand().subtract(estimate.overall()), goal.examDate()))
                .orElse(null);
        return new LearnerReportResult(userId, generatedAt, report, skillSummaries(report, estimate),
                focus(report), band, target);
    }

    private static List<SkillSummary> skillSummaries(List<KnowledgePointReport> report,
                                                     BandEstimator.Estimate estimate) {
        Map<String, List<KnowledgePointReport>> bySkill = new HashMap<>();
        for (KnowledgePointReport point : report) {
            if (point.skill() != null) {
                bySkill.computeIfAbsent(point.skill().toUpperCase(Locale.ROOT), key -> new ArrayList<>()).add(point);
            }
        }
        List<SkillSummary> summaries = new ArrayList<>();
        for (String skill : SUMMARY_ORDER) {
            List<KnowledgePointReport> own = bySkill.getOrDefault(skill, List.of());
            Map<String, Integer> tierCounts = new LinkedHashMap<>();
            for (MasteryTier tier : MasteryTier.values()) tierCounts.put(tier.name(), 0);
            double weighted = 0;
            long weight = 0;
            for (KnowledgePointReport point : own) {
                tierCounts.merge(point.score().tier(), 1, Integer::sum);
                if (!MasteryTier.INSUFFICIENT.name().equals(point.score().tier())) {
                    weighted += point.score().mastery() * point.score().evidenceCount();
                    weight += point.score().evidenceCount();
                }
            }
            summaries.add(new SkillSummary(skill, weight == 0 ? null : weighted / weight, tierCounts, own.size(),
                    estimate.skillBands().get(skill)));
        }
        return summaries;
    }

    /** The weakest points still being built, those that block a review first. */
    private static List<FocusPoint> focus(List<KnowledgePointReport> report) {
        return report.stream()
                .filter(point -> point.score().tier().equals(MasteryTier.PRIORITY.name())
                        || point.score().tier().equals(MasteryTier.CONSOLIDATE.name()))
                .sorted(Comparator.<KnowledgePointReport>comparingInt(point -> blocked(point) ? 0 : 1)
                        .thenComparingDouble(point -> point.score().mastery()))
                .limit(FOCUS_POINTS)
                .map(point -> new FocusPoint(point.knowledgeId(), point.code(), point.name(), point.skill(),
                        point.score().mastery(), point.score().tier(), blocked(point)))
                .toList();
    }

    private static boolean blocked(KnowledgePointReport point) {
        return point.action().type().equals("REVIEW_REQUIRED") || point.action().type().equals("DIAGNOSING");
    }

    private static Action action(MasteryProfile profile, UUID pendingReviewId, boolean diagnosing, Instant now) {
        if (pendingReviewId != null) {
            return new Action(diagnosing ? "DIAGNOSING" : "REVIEW_REQUIRED", pendingReviewId, null);
        }
        return switch (profile.tier()) {
            case PRIORITY, INSUFFICIENT -> new Action("NONE", null, profile.nextReviewAt());
            case CONSOLIDATE -> new Action("CONSOLIDATE", null, profile.nextReviewAt());
            case MAINTAIN -> profile.nextReviewAt() != null && !profile.nextReviewAt().isAfter(now)
                    ? new Action("MAINTAIN_DUE", null, profile.nextReviewAt())
                    : new Action("NONE", null, profile.nextReviewAt());
        };
    }
}
