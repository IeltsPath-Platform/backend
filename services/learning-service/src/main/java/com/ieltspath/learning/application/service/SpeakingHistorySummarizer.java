package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePoint;
import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.port.QuestionEventLog.RecentEvent;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.repository.MasteryProfileRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.vo.MasteryTier;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the grader may be told about the learner's own Speaking standing, so the advice fits them. It is built from the
 * learner's mastery profiles, recent answers and pending reviews, filtered as plan 5b section 5 says, and it is
 * deliberately poor in numbers: a tier in words, a trend in words and the error codes seen lately. A mastery figure or
 * an earlier band would make the model anchor its band on the past, so none of them ever leave this class.
 */
@Component
public class SpeakingHistorySummarizer {
    private static final Logger log = LoggerFactory.getLogger(SpeakingHistorySummarizer.class);
    private static final int MAX_ERRORS_PER_POINT = 3;
    /** Share of right answers the newer half must be ahead (or behind) the older half by to call the trend. Interim. */
    private static final double TREND_SHIFT = 0.34;

    /** A Speaking knowledge point the grader may name, with what the learner can do once they have it. */
    public record CatalogPoint(UUID id, String code, String canDo) { }

    public record ErrorCount(String code, int count) { }

    /** {@code source} is {@code speaking} or {@code from writing}; {@code trend} is null when too few answers to tell. */
    public record HistoryLine(UUID id, String code, String source, MasteryTier tier, String trend,
                              List<ErrorCount> recentErrors, boolean reviewPending) {
        public HistoryLine {
            recentErrors = List.copyOf(recentErrors);
        }

        public String level() {
            return tier == MasteryTier.PRIORITY ? "priority" : "needs consolidation";
        }
    }

    /** The Speaking points the grader may name and the weak ones among them for this learner. */
    public record LearnerContext(List<CatalogPoint> catalog, List<HistoryLine> history) {
        public LearnerContext {
            catalog = List.copyOf(catalog);
            history = List.copyOf(history);
        }

        public static LearnerContext empty() {
            return new LearnerContext(List.of(), List.of());
        }

        /** The id of every point the grader may name, by its code. */
        public java.util.Map<String, UUID> idsByCode() {
            java.util.Map<String, UUID> ids = new java.util.HashMap<>();
            catalog.forEach(point -> ids.put(point.code(), point.id()));
            history.forEach(line -> ids.putIfAbsent(line.code(), line.id()));
            return ids;
        }

        /** Every code the grader is allowed to name: the catalog and the history. */
        public Set<String> allowedKpCodes() {
            Set<String> codes = new HashSet<>();
            catalog.forEach(point -> codes.add(point.code()));
            history.forEach(line -> codes.add(line.code()));
            return codes;
        }
    }

    private final LearningContentClient content;
    private final MasteryProfileRepository profiles;
    private final QuestionEventLog events;
    private final ReviewItemRepository reviews;
    private final SpeakingPracticeSettings settings;
    private final Clock clock;

    @Autowired
    public SpeakingHistorySummarizer(LearningContentClient content, MasteryProfileRepository profiles,
                                     QuestionEventLog events, ReviewItemRepository reviews,
                                     SpeakingPracticeSettings settings) {
        this(content, profiles, events, reviews, settings, Clock.systemUTC());
    }

    SpeakingHistorySummarizer(LearningContentClient content, MasteryProfileRepository profiles, QuestionEventLog events,
                              ReviewItemRepository reviews, SpeakingPracticeSettings settings, Clock clock) {
        this.content = content;
        this.profiles = profiles;
        this.events = events;
        this.reviews = reviews;
        this.settings = settings;
        this.clock = clock;
    }

    /**
     * The catalog and the learner's history. A failure to read the knowledge points from Content only costs the
     * personalised part of the feedback, so it is logged and an empty context is returned; the grade does not wait on it.
     *
     * @param errorCodes the Speaking error codes the grader may name; only these are ever shown for a point
     */
    public LearnerContext summarize(UUID userId, Set<String> errorCodes) {
        Map<UUID, KnowledgePoint> points;
        try {
            points = knowledgePoints();
        } catch (LearningRequestException unavailable) {
            log.warn("Speaking knowledge points unavailable: code={}", unavailable.getCode());
            return LearnerContext.empty();
        }
        Map<UUID, KnowledgePoint> speaking = new LinkedHashMap<>();
        Map<UUID, KnowledgePoint> fromWriting = new LinkedHashMap<>();
        Set<String> extra = extraCodes();
        for (Map.Entry<UUID, KnowledgePoint> entry : points.entrySet()) {
            KnowledgePoint point = entry.getValue();
            if (point.parentKnowledgePointId() != null || point.code() == null) continue;
            if ("SPEAKING".equalsIgnoreCase(point.skill())) speaking.put(entry.getKey(), point);
            else if (extra.contains(point.code())) fromWriting.put(entry.getKey(), point);
        }
        List<CatalogPoint> catalog = speaking.values().stream()
                .map(point -> new CatalogPoint(point.id(), point.code(), point.canDo() == null ? "" : point.canDo()))
                .sorted(Comparator.comparing(CatalogPoint::code)).toList();
        return new LearnerContext(catalog, history(userId, speaking, fromWriting, errorCodes));
    }

    private Map<UUID, KnowledgePoint> knowledgePoints() {
        Map<UUID, KnowledgePoint> all = new HashMap<>();
        var topics = content.getTopicSequence();
        if (topics == null) return all;
        for (var topic : topics) {
            if (topic.knowledgePoints() == null) continue;
            for (KnowledgePoint point : topic.knowledgePoints()) all.putIfAbsent(point.id(), point);
        }
        return all;
    }

    private Set<String> extraCodes() {
        Set<String> codes = new HashSet<>();
        if (settings.historyExtraKpCodes() == null) return codes;
        Arrays.stream(settings.historyExtraKpCodes().split(",")).map(String::strip).filter(code -> !code.isEmpty())
                .forEach(codes::add);
        return codes;
    }

    private List<HistoryLine> history(UUID userId, Map<UUID, KnowledgePoint> speaking,
                                      Map<UUID, KnowledgePoint> fromWriting, Set<String> errorCodes) {
        Set<UUID> eligible = new HashSet<>(speaking.keySet());
        eligible.addAll(fromWriting.keySet());
        if (eligible.isEmpty()) return List.of();

        Instant now = clock.instant();
        Instant oldest = now.minus(Duration.ofDays(settings.historyMaxAgeDays()));
        Set<UUID> reviewPending = new HashSet<>();
        for (PendingReview review : reviews.findPending(userId)) {
            if (review.blocking()) reviewPending.add(review.knowledgePointId());
        }
        List<MasteryProfile> weak = profiles.findAll(userId).stream()
                .filter(profile -> eligible.contains(profile.knowledgePointId()))
                .filter(profile -> profile.tier() == MasteryTier.PRIORITY || profile.tier() == MasteryTier.CONSOLIDATE)
                .filter(profile -> profile.evidenceCount() >= settings.historyMinEvidence())
                .filter(profile -> !profile.lastEvidenceAt().isBefore(oldest))
                .sorted(Comparator.<MasteryProfile>comparingInt(profile -> reviewPending.contains(profile.knowledgePointId()) ? 0 : 1)
                        .thenComparingDouble(MasteryProfile::mastery)
                        .thenComparing(profile -> profile.knowledgePointId().toString()))
                .limit(settings.historyMaxKps()).toList();
        if (weak.isEmpty()) return List.of();

        List<UUID> ids = weak.stream().map(MasteryProfile::knowledgePointId).toList();
        Map<UUID, QuestionEventLog.EventStats> stats = events.statsByKnowledgePoint(userId, ids, oldest);
        Map<UUID, List<RecentEvent>> recent = events.recentByKnowledgePoint(userId, ids, settings.historyTrendWindow());
        List<HistoryLine> lines = new ArrayList<>();
        for (MasteryProfile profile : weak) {
            UUID id = profile.knowledgePointId();
            boolean speakingPoint = speaking.containsKey(id);
            KnowledgePoint point = speakingPoint ? speaking.get(id) : fromWriting.get(id);
            lines.add(new HistoryLine(id, point.code(), speakingPoint ? "speaking" : "from writing", profile.tier(),
                    trend(recent.getOrDefault(id, List.of())), allowedErrors(stats.get(id), errorCodes),
                    reviewPending.contains(id)));
        }
        return lines;
    }

    /** The error codes the grader may name, most frequent first; any other code (a Writing one, say) is left out. */
    private static List<ErrorCount> allowedErrors(QuestionEventLog.EventStats stats, Set<String> allowed) {
        if (stats == null) return List.of();
        return stats.errors().stream().filter(error -> allowed.contains(error.code()))
                .limit(MAX_ERRORS_PER_POINT).map(error -> new ErrorCount(error.code(), error.count())).toList();
    }

    /**
     * Compares the right answers in the newer half of the latest answers with the older half (newest first in the
     * list). Fewer than four judged answers, or no clear shift, gives {@code steady} or no trend at all.
     */
    static String trend(Collection<RecentEvent> newestFirst) {
        List<Boolean> judged = newestFirst.stream().map(RecentEvent::correct).filter(correct -> correct != null).toList();
        if (judged.size() < 4) return null;
        int newer = judged.size() - judged.size() / 2;
        double newerRate = rate(judged.subList(0, newer));
        double olderRate = rate(judged.subList(newer, judged.size()));
        double shift = newerRate - olderRate;
        if (shift >= TREND_SHIFT) return "improving";
        if (shift <= -TREND_SHIFT) return "declining";
        return "steady";
    }

    private static double rate(List<Boolean> answers) {
        return answers.isEmpty() ? 0 : (double) answers.stream().filter(correct -> correct).count() / answers.size();
    }

    /** One history line as the grader reads it: words only, no number that stands for mastery. */
    public static String render(HistoryLine line) {
        StringBuilder text = new StringBuilder(line.code()).append(" [").append(line.source()).append("] ")
                .append(line.level());
        if (line.trend() != null) text.append("; trend: ").append(line.trend());
        if (!line.recentErrors().isEmpty()) {
            text.append("; recent errors: ");
            for (int index = 0; index < line.recentErrors().size(); index++) {
                ErrorCount error = line.recentErrors().get(index);
                text.append(index > 0 ? ", " : "").append(error.code()).append(" x").append(error.count());
            }
        }
        if (line.reviewPending()) text.append("; review pending");
        return text.toString();
    }
}
