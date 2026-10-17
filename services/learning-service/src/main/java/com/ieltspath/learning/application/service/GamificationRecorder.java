package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.domain.repository.GamificationRepository;
import com.ieltspath.learning.domain.repository.GamificationRepository.LedgerEntry;
import com.ieltspath.learning.domain.service.XpRules;
import com.ieltspath.learning.domain.service.XpRules.Award;
import com.ieltspath.learning.domain.service.XpRules.Candidate;
import com.ieltspath.learning.domain.service.XpRules.Params;
import com.ieltspath.learning.domain.vo.GamificationSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns submissions and knowledge-point milestones into XP. Callers hold the learner lock and run inside the submission
 * transaction, so an award and the answer that earned it commit together. Every award goes through the ledger, whose
 * unique key means a repeated request or milestone adds nothing.
 */
@Component
public class GamificationRecorder {
    static final String QUESTION = "QUESTION";
    static final String KP_CONSOLIDATE = "KP_CONSOLIDATE";
    static final String KP_MAINTAIN = "KP_MAINTAIN";
    static final String STREAK = "STREAK";
    static final String QUEST = "QUEST";

    /** A knowledge point that reached consolidate; {@code maintained} also means it reached maintain. */
    public record Rise(UUID knowledgePointId, boolean maintained) {}

    /** The learner's progress: counted XP, the level, the XP needed for the next one, and today's answer XP. */
    public record Progress(int totalXp, int level, int nextLevelAt, int xpToNextLevel, long xpToday, int dailyCap) {}

    private final GamificationRepository repository;
    private final LearningContentClient content;
    private final LearnerTimezones timezones;
    private final HeartsService hearts;
    private final LeagueService league;
    private final BadgeService badges;
    private final Params params;
    private final Clock clock = Clock.systemUTC();

    public GamificationRecorder(GamificationRepository repository, LearningContentClient content,
                                LearnerTimezones timezones, HeartsService hearts, LeagueService league,
                                BadgeService badges,
                                @Value("${learning.gamification.xp.easy:10}") int easy,
                                @Value("${learning.gamification.xp.medium:15}") int medium,
                                @Value("${learning.gamification.xp.hard:20}") int hard,
                                @Value("${learning.gamification.xp.unknown:10}") int unknown,
                                @Value("${learning.gamification.xp.daily-cap:100}") int dailyCap,
                                @Value("${learning.gamification.xp.level-xp:1000}") int levelXp,
                                @Value("${learning.gamification.xp.kp-consolidate:25}") int consolidate,
                                @Value("${learning.gamification.xp.kp-maintain:50}") int maintain) {
        this.repository = repository;
        this.content = content;
        this.timezones = timezones;
        this.hearts = hearts;
        this.league = league;
        this.badges = badges;
        this.params = new Params(easy, medium, hard, unknown, dailyCap, levelXp, consolidate, maintain);
    }

    /**
     * XP for the correct answers of one first submission. Answers past the learner's daily cap are kept as shadow XP and
     * flag {@code xpCapped}.
     */
    public GamificationSummary recordCorrectAnswers(UUID userId, UUID requestId, List<UUID> correctQuestionVersionIds) {
        int before = repository.totalXp(userId);
        if (correctQuestionVersionIds.isEmpty()) return apply(userId, before, 0, false);
        LocalDate today = today(userId);
        Map<UUID, String> difficulties = content.questionDifficulties(correctQuestionVersionIds);
        List<Candidate> candidates = correctQuestionVersionIds.stream()
                .map(id -> new Candidate(id, difficulties.get(id))).toList();
        List<Award> awards = XpRules.award(params, candidates, repository.countedAnswerXpOn(userId, today));
        List<LedgerEntry> entries = awards.stream().map(award -> new LedgerEntry(QUESTION,
                requestId + ":" + award.questionVersionId(), award.amount(), award.counted(), today)).toList();
        boolean[] fresh = repository.insertLedger(userId, entries);
        int earned = 0;
        boolean capped = false;
        for (int index = 0; index < awards.size(); index++) {
            if (!fresh[index]) continue;
            if (awards.get(index).counted()) earned += awards.get(index).amount();
            else capped = true;
        }
        return apply(userId, before, earned, capped);
    }

    /**
     * Milestone XP for knowledge points that reached consolidate, and again for those that reached maintain. Each point
     * pays once per milestone, ever, and the milestone XP is not limited by the daily cap.
     */
    public void recordRises(UUID userId, List<Rise> rises) {
        if (rises.isEmpty()) return;
        LocalDate today = today(userId);
        List<LedgerEntry> entries = new ArrayList<>();
        for (Rise rise : rises) {
            String ref = rise.knowledgePointId().toString();
            entries.add(new LedgerEntry(KP_CONSOLIDATE, ref, params.consolidateXp(), true, today));
            if (rise.maintained()) entries.add(new LedgerEntry(KP_MAINTAIN, ref, params.maintainXp(), true, today));
        }
        boolean[] fresh = repository.insertLedger(userId, entries);
        int earned = 0;
        for (int index = 0; index < entries.size(); index++) {
            if (fresh[index]) earned += entries.get(index).amount();
        }
        if (earned > 0) apply(userId, repository.totalXp(userId), earned, false);
    }

    /**
     * XP for a streak milestone. {@code milestoneRef} names the streak run and the day, so the same milestone of the same
     * run pays once. Like knowledge-point milestones, it is not limited by the daily cap.
     */
    public void recordStreakMilestone(UUID userId, String milestoneRef, int xp) {
        payOnce(userId, STREAK, milestoneRef, xp);
    }

    /** XP for a completed daily quest, paid once per quest and, like milestones, outside the daily cap. */
    public void recordQuestReward(UUID userId, String questId, int xp) {
        payOnce(userId, QUEST, questId, xp);
    }

    private void payOnce(UUID userId, String source, String reference, int xp) {
        if (xp <= 0) return;
        LedgerEntry entry = new LedgerEntry(source, reference, xp, true, today(userId));
        boolean[] fresh = repository.insertLedger(userId, List.of(entry));
        if (fresh[0]) apply(userId, repository.totalXp(userId), xp, false);
    }

    /** The learner's XP, level and today's counted answer XP against the cap. */
    public Progress progress(UUID userId) {
        int total = repository.totalXp(userId);
        int level = params.levelOf(total);
        int nextLevelAt = level * params.levelXp();
        return new Progress(total, level, nextLevelAt, nextLevelAt - total,
                repository.countedAnswerXpOn(userId, today(userId)), params.dailyCap());
    }

    /** The learner's standing without any award, for a submission that earned nothing new. */
    public GamificationSummary current(UUID userId) {
        return apply(userId, repository.totalXp(userId), 0, false);
    }

    private GamificationSummary apply(UUID userId, int before, int earned, boolean capped) {
        int total = before + earned;
        int level = params.levelOf(total);
        if (earned > 0) {
            repository.saveTotals(userId, total, level);
            // Only XP that counted reaches the league; capped answers were never part of `earned`.
            league.addWeeklyXp(userId, earned);
            badges.onLevel(userId, level);
        }
        boolean leveledUp = params.levelOf(before) < level;
        if (leveledUp) hearts.refillOnLevelUp(userId);
        return new GamificationSummary(earned, capped, total, level, leveledUp);
    }

    private LocalDate today(UUID userId) {
        return LocalDate.now(clock.withZone(timezones.zoneOf(userId)));
    }
}
