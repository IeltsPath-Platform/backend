package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.VocabularyProgressReader;
import com.ieltspath.library.application.result.*;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.progress.VocabularyProgressPolicy;
import com.ieltspath.library.domain.vo.CefrLevel;
import com.ieltspath.library.domain.vo.IeltsBandRange;
import com.ieltspath.library.config.VocabularyWeeklyGoalProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VocabularyProgressUseCase {
    private final VocabularyProgressReader reader;
    private final VocabularyWeeklyGoalProperties weeklyGoalProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public WeeklyGoalResult weeklyGoal(UUID userId, ZoneId timezone) {
        LocalDate today = LocalDate.now(clock.withZone(timezone));
        var week = VocabularyProgressPolicy.weekWindow(today, timezone);
        int target = reader.weeklyTarget(userId, weeklyGoalProperties.getWeeklyGoalDefault());
        long progress = reader.weeklyProgress(userId, week.start().atStartOfDay(timezone).toInstant(),
                week.endExclusive().atStartOfDay(timezone).toInstant());
        var reviewedDays = reader.reviewDays(userId, week.start(), week.endExclusive(), timezone).stream().collect(java.util.stream.Collectors.toSet());
        List<WeeklyGoalResult.Day> days = week.start().datesUntil(week.endExclusive())
                .map(date -> new WeeklyGoalResult.Day(date, date.getDayOfWeek(), reviewedDays.contains(date))).toList();
        return new WeeklyGoalResult(target, progress, Math.max(0L, target - progress), days);
    }

    @Transactional
    public WeeklyGoalResult setWeeklyGoal(UUID userId, int target, ZoneId timezone) {
        VocabularyProgressPolicy.validateWeeklyTarget(target);
        reader.saveWeeklyTarget(userId, target);
        return weeklyGoal(userId, timezone);
    }

    @Transactional(readOnly = true)
    public VocabularyMapResult vocabularyMap(UUID userId, CefrLevel requestedCurrent) {
        var counts = reader.vocabularyMap(userId, clock.instant());
        var bands = counts.stream().map(count -> {
            CefrLevel cefr = CefrLevel.valueOf(count.cefr());
            IeltsBandRange range = cefr.bandRange();
            return new VocabularyMapResult.Band(range.min() + "–" + range.max(), cefr.name(), count.total(), count.learned(),
                    count.learning(), count.needsReview(), count.total() - count.learned());
        }).toList();
        List<VocabularyProgressPolicy.BandCount> policyCounts = counts.stream()
                .map(count -> new VocabularyProgressPolicy.BandCount(count.cefr(), count.learning(), count.needsReview())).toList();
        CefrLevel current = requestedCurrent == null ? VocabularyProgressPolicy.currentBand(policyCounts) : requestedCurrent;
        CefrLevel suggested = VocabularyProgressPolicy.suggestedBand(policyCounts);
        return new VocabularyMapResult(bands, current == null ? null : current.name(), suggested == null ? null : suggested.name());
    }

    @Transactional(readOnly = true)
    public TopicProgressResult topicsProgress(UUID userId, UUID setId) {
        List<VocabularyProgressReader.TopicCounts> counts = reader.topicsProgress(userId, setId, clock.instant());
        List<TopicProgressResult.Topic> topics = counts.stream().map(count -> {
            int percent = count.total() == 0 ? 0 : (int) Math.round(count.learned() * 100.0 / count.total());
            return new TopicProgressResult.Topic(count.topicId(), count.name(), count.total(), count.learned(), percent,
                    VocabularyProgressPolicy.topicStatus(count.total(), count.learned()));
        }).toList();
        return counts.stream().filter(count -> count.lastReviewedAt() != null)
                .max(Comparator.comparing(VocabularyProgressReader.TopicCounts::lastReviewedAt))
                .map(current -> new TopicProgressResult(topics, new TopicProgressResult.Current(current.topicId(), current.name(),
                        current.lastReviewedAt(), reader.nextWords(userId, current.topicId(), setId))))
                .orElseGet(() -> new TopicProgressResult(topics, null));
    }

    @Transactional(readOnly = true)
    public SetProgressResult setProgress(UUID userId, UUID setId) {
        return reader.setProgress(userId, setId, clock.instant()).orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public RecentResult recentResult(UUID userId) {
        return reader.recentResult(userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public VocabularyAchievementsResult achievements(UUID userId, ZoneId timezone) {
        LocalDate today = LocalDate.now(clock.withZone(timezone));
        var stats = reader.achievementCounts(userId, clock.instant());
        var reviewDays = reader.reviewDays(userId, today.minusDays(7), today.plusDays(1), timezone).stream().collect(java.util.stream.Collectors.toSet());
        int streak = VocabularyProgressPolicy.reviewStreak(reviewDays, today);
        return new VocabularyAchievementsResult(VocabularyProgressPolicy.achievements(streak, stats.completedTopics(), stats.masteredWords()));
    }
}
