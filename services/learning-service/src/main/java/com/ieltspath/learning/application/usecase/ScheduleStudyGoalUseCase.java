package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.StudyTimezones;
import com.ieltspath.learning.domain.aggregate.LearningGoal;
import com.ieltspath.learning.domain.repository.*;
import com.ieltspath.learning.domain.vo.GoalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleStudyGoalUseCase {
    private final StudySessionRepository sessions;
    private final StudyTimezones timezones;
    private final LearningGoalRepository goals;
    private final Clock clock;
    public void lock(UUID userId) { sessions.lockUser(userId); }
    public void lockGoalOwner(UUID goalId) { sessions.lockGoalOwner(goalId); }
    /**
     * The learner may have changed the timezone on their profile, which the User Service keeps. When the goal was last
     * scheduled in another timezone it is scheduled again, from the next local day. Callers hold the learner's lock.
     */
    public void rescheduleIfTimezoneChanged(UUID userId) {
        goals.findActiveByUserId(userId).ifPresent(goal -> sessions.latestGoalTimezone(userId)
                .filter(scheduled -> !scheduled.equals(timezones.zoneOf(userId).getId()))
                .ifPresent(scheduled -> execute(goal)));
    }
    public void execute(LearningGoal goal) {
        var zone=timezones.zoneOf(goal.getUserId());
        var now=clock.instant();
        var date=now.atZone(zone).toLocalDate().plusDays(1);
        // Moving west must not replace a target for a previously recorded local day.
        var latest=sessions.latestRecordedDate(goal.getUserId(),now);
        if(latest!=null && !date.isAfter(latest)) date=latest.plusDays(1);
        // A timezone change may move the pending effective date. Retain its audit row but
        // prevent the older pending target from taking effect after this newer choice.
        sessions.supersedePendingGoals(goal.getUserId(),now);
        sessions.scheduleGoal(goal.getUserId(),goal.getId(),goal.getStatus()==GoalStatus.ACTIVE ? goal.getAvailableMinutesPerDay() : null,
                date,zone.getId(),date.atStartOfDay(zone).toInstant());
    }
}
