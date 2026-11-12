package com.ieltspath.learning.domain.aggregate;

import com.ieltspath.learning.domain.vo.GoalStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LearningGoalTest {

    @Test
    void stateTransitionsWorkCorrectly() {
        LearningGoal goal = LearningGoal.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .targetBand(BigDecimal.valueOf(7.0))
                .availableMinutesPerDay(60)
                .status(GoalStatus.ACTIVE)
                .build();

        goal.pause();
        assertThat(goal.getStatus()).isEqualTo(GoalStatus.PAUSED);

        goal.resume();
        assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACTIVE);

        goal.complete();
        assertThat(goal.getStatus()).isEqualTo(GoalStatus.ACHIEVED);
        assertThat(goal.getEndedAt()).isNotNull();
    }

    @Test
    void updateGoal_validatesTargetBandAndMinutes() {
        LearningGoal goal = LearningGoal.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .targetBand(BigDecimal.valueOf(6.0))
                .availableMinutesPerDay(45)
                .status(GoalStatus.ACTIVE)
                .build();

        goal.updateGoal(BigDecimal.valueOf(8.0), LocalDate.now().plusMonths(6), 90);

        assertThat(goal.getTargetBand()).isEqualByComparingTo("8.0");
        assertThat(goal.getAvailableMinutesPerDay()).isEqualTo(90);

        assertThatThrownBy(() -> goal.updateGoal(BigDecimal.valueOf(10.0), null, 60))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> goal.updateGoal(BigDecimal.valueOf(7.0), null, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> goal.updateGoal(new BigDecimal("0.5"), null, 60))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> goal.updateGoal(BigDecimal.valueOf(7.0), null, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateGoal_acceptsSrsBoundaries() {
        LearningGoal goal = LearningGoal.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .targetBand(BigDecimal.valueOf(6.0))
                .availableMinutesPerDay(45)
                .status(GoalStatus.ACTIVE)
                .build();

        goal.updateGoal(new BigDecimal("1.0"), null, 5);
        assertThat(goal.getTargetBand()).isEqualByComparingTo("1.0");
        assertThat(goal.getAvailableMinutesPerDay()).isEqualTo(5);

        goal.updateGoal(new BigDecimal("9.0"), null, 5);
        assertThat(goal.getTargetBand()).isEqualByComparingTo("9.0");
    }
}

