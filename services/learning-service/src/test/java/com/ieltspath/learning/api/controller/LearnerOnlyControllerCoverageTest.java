package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.authorization.LearnerOnly;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LearnerOnlyControllerCoverageTest {
    @Test
    void everyPlannedLearnerControllerIsRestricted() {
        assertThat(List.of(FreePracticeController.class, FreeWritingController.class, GamificationController.class,
                HeartsController.class, LearnerTreeController.class, LearningActivityController.class,
                LearningGoalController.class, LessonLearningController.class, LessonResourceController.class,
                PracticeController.class, ReviewController.class, StreakController.class, StudySessionController.class,
                WritingController.class))
                .allSatisfy(controller -> assertThat(controller.isAnnotationPresent(LearnerOnly.class))
                        .as("%s should require CUSTOMER", controller.getSimpleName()).isTrue());
    }
}
