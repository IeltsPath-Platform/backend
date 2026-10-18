package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.aggregate.*;
import com.ieltspath.user.domain.repository.*;
import com.ieltspath.user.domain.vo.GoalStatus;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;

class ScheduleStudyGoalUseCaseTest {
    @Test void changedTimezoneCannotRewriteRecordedDayAndPauseSchedulesNull() {
        var sessions=mock(StudySessionRepository.class);
        var profiles=mock(LearnerProfileRepository.class);
        var user=UUID.randomUUID(); var id=UUID.randomUUID();
        Instant now=Instant.parse("2026-10-15T01:00:00Z");
        when(profiles.findByUserId(user)).thenReturn(Optional.of(LearnerProfile.builder().userId(user).timezone("America/Los_Angeles").build()));
        when(sessions.latestRecordedDate(user,now)).thenReturn(LocalDate.parse("2026-10-15"));
        var useCase=new ScheduleStudyGoalUseCase(sessions,profiles,Clock.fixed(now,ZoneOffset.UTC));
        var goal=LearningGoal.builder().id(id).userId(user).availableMinutesPerDay(60).status(GoalStatus.PAUSED).build();
        useCase.execute(goal);
        verify(sessions).supersedePendingGoals(user,now);
        verify(sessions).scheduleGoal(user,id,null,LocalDate.parse("2026-10-16"),"America/Los_Angeles",Instant.parse("2026-10-16T07:00:00Z"));
    }
}
