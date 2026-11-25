package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.RewardGrantRepository;
import com.ieltspath.learning.domain.repository.StreakRepository;
import com.ieltspath.learning.domain.service.StreakRules;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import com.ieltspath.learning.domain.vo.StreakState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** The streak counts the day a learner answered, in their timezone, once per day and once per milestone. */
class StreakRecorderTest {
    private static final ZoneId HANOI = ZoneId.of("Asia/Ho_Chi_Minh");
    private final UUID user = UUID.randomUUID();
    private final StreakRepository streaks = mock(StreakRepository.class);
    private final GamificationRecorder gamification = mock(GamificationRecorder.class);
    private final RewardGrantRepository rewards = mock(RewardGrantRepository.class);
    private final LearnerTimezones timezones = mock(LearnerTimezones.class);
    private final BadgeService badges = mock(BadgeService.class);
    private final StreakRecorder recorder = new StreakRecorder(streaks, gamification, rewards, timezones, badges);

    StreakRecorderTest() {
        when(timezones.zoneOf(user)).thenReturn(HANOI);
    }

    private QuestionAttemptEvent answeredAt(Instant at) {
        return new QuestionAttemptEvent(user, UUID.randomUUID(), List.of(), true, null, null, null, null,
                QuestionEventSource.LESSON_EXERCISE, UUID.randomUUID(), null, true, at);
    }

    private static StreakState streak(int days, LocalDate lastDay) {
        StreakState state = StreakState.empty();
        for (int i = days - 1; i >= 0; i--) state = StreakRules.recordAnswer(state, lastDay.minusDays(i)).state();
        return state;
    }

    private StreakState saved() {
        ArgumentCaptor<StreakState> state = ArgumentCaptor.forClass(StreakState.class);
        verify(streaks).save(org.mockito.ArgumentMatchers.eq(user), state.capture());
        return state.getValue();
    }

    @Test
    void aReplayedSubmissionWithNoNewAnswerChangesNothing() {
        recorder.onAnswers(new QuestionEventsAppended(user));
        verifyNoInteractions(streaks, gamification, rewards, badges);
    }

    @Test
    void assessmentResultsDoNotCountAsAStudyDay() {
        var graded = new QuestionAttemptEvent(user, UUID.randomUUID(), List.of(), true, null, null, null, null,
                QuestionEventSource.ASSESSMENT, UUID.randomUUID(), UUID.randomUUID(), true, null);
        recorder.onAnswers(new QuestionEventsAppended(user, List.of(graded)));
        verifyNoInteractions(streaks, gamification, rewards, badges);
    }

    @Test
    void theDayIsTheOneTheLearnerAnsweredInTheirTimezone() {
        when(streaks.find(user)).thenReturn(Optional.empty());
        // 17:30 UTC is 00:30 the next day in Hanoi.
        recorder.onAnswers(new QuestionEventsAppended(user, List.of(answeredAt(Instant.parse("2026-10-14T17:30:00Z")))));
        assertThat(saved().lastActive()).isEqualTo(LocalDate.parse("2026-10-15"));
    }

    @Test
    void aMinuteBeforeLocalMidnightStillCountsForThatDay() {
        when(streaks.find(user)).thenReturn(Optional.empty());
        recorder.onAnswers(new QuestionEventsAppended(user, List.of(answeredAt(Instant.parse("2026-10-14T16:59:00Z")))));
        assertThat(saved().lastActive()).isEqualTo(LocalDate.parse("2026-10-14"));
    }

    @Test
    void theSeventhDayPaysItsMilestoneOnceWithXpAndPoints() {
        LocalDate seventh = LocalDate.parse("2026-10-15");
        StreakState sixDays = streak(6, seventh.minusDays(1));
        StreakState sevenDays = StreakRules.recordAnswer(sixDays, seventh).state();
        when(streaks.find(user)).thenReturn(Optional.of(sixDays), Optional.of(sevenDays));
        Instant morning = Instant.parse("2026-10-15T02:00:00Z");

        recorder.onAnswers(new QuestionEventsAppended(user, List.of(answeredAt(morning))));
        recorder.onAnswers(new QuestionEventsAppended(user, List.of(answeredAt(morning.plusSeconds(3600)))));

        String reference = sixDays.streakStart() + ":7";
        verify(gamification, times(1)).recordStreakMilestone(user, reference, 100);
        verify(rewards, times(1)).enqueue(user, "STREAK_MILESTONE", reference, 10);
    }

    @Test
    void theThirtiethDayPaysXpButNoPoints() {
        LocalDate thirtieth = LocalDate.parse("2026-10-30");
        StreakState days29 = streak(29, thirtieth.minusDays(1));
        when(streaks.find(user)).thenReturn(Optional.of(days29));

        recorder.onAnswers(new QuestionEventsAppended(user, List.of(answeredAt(Instant.parse("2026-10-30T02:00:00Z")))));

        verify(gamification).recordStreakMilestone(user, days29.streakStart() + ":30", 500);
        verify(rewards, never()).enqueue(any(), anyString(), anyString(), anyInt());
    }

    @Test
    void badgesAreCheckedOnlyOnTheFirstAnswerOfTheDay() {
        LocalDate day = LocalDate.parse("2026-10-15");
        StreakState yesterday = streak(2, day.minusDays(1));
        StreakState afterFirst = StreakRules.recordAnswer(yesterday, day).state();
        when(streaks.find(user)).thenReturn(Optional.of(yesterday), Optional.of(afterFirst));

        recorder.onAnswers(new QuestionEventsAppended(user, List.of(answeredAt(Instant.parse("2026-10-15T02:00:00Z")))));
        recorder.onAnswers(new QuestionEventsAppended(user, List.of(answeredAt(Instant.parse("2026-10-15T05:00:00Z")))));

        verify(badges, times(1)).onStreak(user, 3);
    }
}
