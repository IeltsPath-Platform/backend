package com.ieltspath.library.application.port;

import com.ieltspath.library.application.result.RecentResult;
import com.ieltspath.library.application.result.SetProgressResult;
import com.ieltspath.library.application.result.TopicProgressResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VocabularyProgressReader {
    int weeklyTarget(UUID userId, int defaultTarget);
    void saveWeeklyTarget(UUID userId, int target);
    long weeklyProgress(UUID userId, Instant start, Instant endExclusive);
    List<LocalDate> reviewDays(UUID userId, LocalDate start, LocalDate endExclusive, ZoneId timezone);
    List<BandCounts> vocabularyMap(UUID userId, Instant now);
    List<TopicCounts> topicsProgress(UUID userId, UUID setId, Instant now);
    List<TopicProgressResult.NextWord> nextWords(UUID userId, UUID topicId, UUID setId);
    Optional<SetProgressResult> setProgress(UUID userId, UUID setId, Instant now);
    Optional<RecentResult> recentResult(UUID userId);
    AchievementCounts achievementCounts(UUID userId, Instant now);

    record BandCounts(String cefr, long total, long learned, long learning, long needsReview) {}
    record TopicCounts(UUID topicId, String name, long total, long learned, Instant lastReviewedAt) {}
    record AchievementCounts(long completedTopics, long masteredWords) {}
}
