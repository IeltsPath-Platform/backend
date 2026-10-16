package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.aggregate.LearningGoal;
import com.ieltspath.user.domain.repository.*;
import com.ieltspath.user.domain.vo.GoalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleStudyGoalUseCase {
    private final StudySessionRepository sessions;
    private final LearnerProfileRepository profiles;
    private final Clock clock;
    public void lock(UUID userId) { sessions.lockUser(userId); }
    public void lockGoalOwner(UUID goalId) { sessions.lockGoalOwner(goalId); }
    public void execute(LearningGoal goal) {
        var zone=StudyTimezone.forUser(profiles,goal.getUserId());
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
