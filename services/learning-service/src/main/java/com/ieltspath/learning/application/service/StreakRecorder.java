package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.RewardGrantRepository;
import com.ieltspath.learning.domain.repository.StreakRepository;
import com.ieltspath.learning.domain.service.StreakRules;
import com.ieltspath.learning.domain.vo.StreakState;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Counts a learner's answered day when their question events are written. It runs before the submission commits, so the
 * streak, its milestone XP and the queued points commit together with the answer. The day is the one the learner
 * submitted the answer, judged in their timezone. A write with no new answer (a replayed submission) and assessment
 * results (finalised when grading ends, not when the learner answered) do not count.
 */
@Component
public class StreakRecorder {
    static final String MILESTONE_REWARD = "STREAK_MILESTONE";

    private final StreakRepository streaks;
    private final GamificationRecorder gamification;
    private final RewardGrantRepository rewards;
    private final LearnerTimezones timezones;
    private final BadgeService badges;

    public StreakRecorder(StreakRepository streaks, GamificationRecorder gamification, RewardGrantRepository rewards,
                          LearnerTimezones timezones, BadgeService badges) {
        this.badges = badges;
        this.streaks = streaks;
        this.gamification = gamification;
        this.rewards = rewards;
        this.timezones = timezones;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onAnswers(QuestionEventsAppended appended) {
        UUID userId = appended.userId();
        Instant answeredAt = appended.submittedAnswers().stream().map(event -> event.answeredAt())
                .max(Instant::compareTo).orElse(null);
        if (answeredAt == null) return;
        LocalDate today = LocalDate.ofInstant(answeredAt, timezones.zoneOf(userId));
        StreakState before = streaks.find(userId).orElse(StreakState.empty());
        StreakRules.Recorded recorded = StreakRules.recordAnswer(before, today);
        streaks.save(userId, recorded.state());
        // The streak only grows on the first answer of a day, which is the only time its badges need checking.
        if (!today.equals(before.lastActive())) badges.onStreak(userId, recorded.state().currentDays());
        for (int days : recorded.milestones()) {
            payMilestone(userId, recorded.state().streakStart(), days);
        }
    }

    /** One milestone of one streak run: XP always, points for the weekly ones. The reference holds the run's start. */
    private void payMilestone(UUID userId, LocalDate streakStart, int days) {
        String reference = streakStart + ":" + days;
        gamification.recordStreakMilestone(userId, reference, StreakRules.milestoneXp(days));
        int points = StreakRules.milestonePoints(days);
        if (points > 0) rewards.enqueue(userId, MILESTONE_REWARD, reference, points);
    }
}
