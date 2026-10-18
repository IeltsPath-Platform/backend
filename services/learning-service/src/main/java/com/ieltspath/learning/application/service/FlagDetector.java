package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.StaffInsightRepository;
import com.ieltspath.learning.domain.service.AnomalyRules;
import com.ieltspath.learning.domain.vo.StaffRecords.FlagKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Checks a learner against the anomaly rules after their question events committed, and only that learner. It runs in its
 * own transaction, so a failure here never undoes or delays a submission. A kind already flagged for the learner
 * recently is not raised again.
 */
@Component
public class FlagDetector {
    private static final Logger log = LoggerFactory.getLogger(FlagDetector.class);

    private final StaffInsightRepository insight;
    private final FlagSettings settings;
    private final AnomalyRules rules = new AnomalyRules();
    private final Clock clock = Clock.systemUTC();

    public FlagDetector(StaffInsightRepository insight, FlagSettings settings) {
        this.insight = insight;
        this.settings = settings;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEvents(QuestionEventsAppended appended) {
        try {
            check(appended.userId());
        } catch (RuntimeException exception) {
            log.warn("Anomaly check failed: userId={}", appended.userId(), exception);
        }
    }

    /** Raises the flags that apply to the learner now; returns the kinds newly raised. */
    public List<FlagKind> check(UUID userId) {
        Instant now = clock.instant();
        Instant silentSince = now.minus(Duration.ofHours(settings.repeatSilenceHours()));
        var limits = new AnomalyRules.Thresholds(settings.minQuestions(), settings.medianBelowMs(),
                settings.minAccuracy(), settings.burstCount());
        List<FlagKind> raised = new java.util.ArrayList<>();

        var day = insight.samplesSince(userId, now.minus(Duration.ofDays(settings.dayWindowDays())));
        if (rules.tooFastHighScore(day, limits) && insight.insertFlag(userId, FlagKind.TOO_FAST_HIGH_SCORE,
                "{\"answers\":" + day.size() + ",\"windowDays\":" + settings.dayWindowDays() + "}", silentSince)) {
            raised.add(FlagKind.TOO_FAST_HIGH_SCORE);
        }
        long burst = insight.eventsSince(userId, now.minus(Duration.ofMinutes(settings.burstWindowMinutes())));
        if (rules.burst(burst, limits) && insight.insertFlag(userId, FlagKind.BURST_SUBMISSIONS,
                "{\"submissions\":" + burst + ",\"windowMinutes\":" + settings.burstWindowMinutes() + "}",
                silentSince)) {
            raised.add(FlagKind.BURST_SUBMISSIONS);
        }
        for (String hash : insight.recentEssayHashes(userId, now.minus(Duration.ofDays(settings.dayWindowDays())))) {
            long others = insight.otherLearnersWithEssayHash(userId, hash);
            if (others > 0 && insight.insertFlag(userId, FlagKind.DUPLICATE_ESSAY,
                    "{\"essayHash\":\"" + hash + "\",\"otherLearners\":" + others + "}", silentSince)) {
                raised.add(FlagKind.DUPLICATE_ESSAY);
                break;
            }
        }
        return raised;
    }
}
