package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.StudyTimezones;
import com.ieltspath.learning.domain.aggregate.LearningGoal;
import com.ieltspath.learning.domain.repository.LearningGoalRepository;
import com.ieltspath.learning.domain.repository.StudySessionRepository;
import com.ieltspath.learning.domain.vo.GoalStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleStudyGoalUseCaseTest {
    private final UUID user = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-15T01:00:00Z");
    private final StudySessionRepository sessions = mock(StudySessionRepository.class);
    private final LearningGoalRepository goals = mock(LearningGoalRepository.class);
    private StudyTimezones timezones = u -> ZoneId.of("America/Los_Angeles");

    private ScheduleStudyGoalUseCase useCase() {
        return new ScheduleStudyGoalUseCase(sessions, timezones, goals, Clock.fixed(now, ZoneOffset.UTC));
    }

    private LearningGoal goal(GoalStatus status) {
        return LearningGoal.builder().id(id).userId(user).availableMinutesPerDay(60).status(status).build();
    }

    @Test
    void changedTimezoneCannotRewriteRecordedDayAndPauseSchedulesNull() {
        when(sessions.latestRecordedDate(user, now)).thenReturn(LocalDate.parse("2026-10-15"));

        useCase().execute(goal(GoalStatus.PAUSED));

        verify(sessions).supersedePendingGoals(user, now);
        verify(sessions).scheduleGoal(user, id, null, LocalDate.parse("2026-10-16"), "America/Los_Angeles",
                Instant.parse("2026-10-16T07:00:00Z"));
    }

    @Test
    void anActiveGoalIsRescheduledWhenTheProfileTimezoneDiffersFromTheOneItWasScheduledIn() {
        when(goals.findActiveByUserId(user)).thenReturn(Optional.of(goal(GoalStatus.ACTIVE)));
        when(sessions.latestGoalTimezone(user)).thenReturn(Optional.of("Asia/Ho_Chi_Minh"));

        useCase().rescheduleIfTimezoneChanged(user);

        verify(sessions).scheduleGoal(user, id, 60, LocalDate.parse("2026-10-15"), "America/Los_Angeles",
                Instant.parse("2026-10-15T07:00:00Z"));
    }

    @Test
    void nothingChangesWhenTheTimezoneIsTheSame() {
        when(goals.findActiveByUserId(user)).thenReturn(Optional.of(goal(GoalStatus.ACTIVE)));
        when(sessions.latestGoalTimezone(user)).thenReturn(Optional.of("America/Los_Angeles"));

        useCase().rescheduleIfTimezoneChanged(user);

        verify(sessions, never()).scheduleGoal(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aLearnerWithoutAnActiveGoalOrWithoutAScheduleIsLeftAlone() {
        when(goals.findActiveByUserId(user)).thenReturn(Optional.empty());
        useCase().rescheduleIfTimezoneChanged(user);

        when(goals.findActiveByUserId(user)).thenReturn(Optional.of(goal(GoalStatus.ACTIVE)));
        when(sessions.latestGoalTimezone(user)).thenReturn(Optional.empty());
        useCase().rescheduleIfTimezoneChanged(user);

        verify(sessions, never()).scheduleGoal(any(), any(), any(), any(), any(), any());
    }
}
