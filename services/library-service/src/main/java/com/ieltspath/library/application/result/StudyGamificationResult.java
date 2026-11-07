package com.ieltspath.library.application.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.*;

public final class StudyGamificationResult {
    private StudyGamificationResult() {}
    public record XpView(int totalXp,int level,int nextLevelAt,int xpToNextLevel) {}
    public record HeartsView(int hearts,int maxHearts,Instant nextHeartAt,int refillCost) {}
    public record StreakView(int currentDays,int longestDays,LocalDate lastQualifiedDate,boolean qualifiedToday,
        boolean freezeAvailable,Integer freezeCost,String freezeBlockedReason,int lostStreakDays,String timezone) {}
    public record Summary(XpView xp,StreakView streak,HeartsView hearts) {}
    public record XpBreakdown(int firstCompletion,int perfectBonus) {}
    public record Reward(int xpEarned,XpBreakdown xpBreakdown,boolean leveledUp,boolean streakIncreased,Summary summary) {}
    public record Settings(String timezone) {}
    public record PurchaseResult(@JsonInclude(JsonInclude.Include.NON_NULL) HeartsView hearts,
        @JsonInclude(JsonInclude.Include.NON_NULL) StreakView streak,int pointsSpent,long pointsBalanceAfter) {}
}
