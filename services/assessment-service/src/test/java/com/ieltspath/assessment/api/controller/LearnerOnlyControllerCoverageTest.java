package com.ieltspath.assessment.api.controller;

import com.ieltspath.assessment.api.dto.request.CreateGradingJobRequest;
import com.ieltspath.commonsecurity.authorization.LearnerOnly;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LearnerOnlyControllerCoverageTest {
    @Test
    void everyPlannedLearnerControllerAndGradingJobMethodIsRestricted() throws Exception {
        assertThat(List.of(AssessmentAttemptController.class, AssessmentResultController.class,
                BandStatisticsController.class, LearnerSubmissionController.class, PlacementResultController.class,
                ProvisionalFeedbackController.class, VideoPracticeController.class))
                .allSatisfy(controller -> assertThat(controller.isAnnotationPresent(LearnerOnly.class))
                        .as("%s should require CUSTOMER", controller.getSimpleName()).isTrue());

        assertThat(GradingJobController.class.getMethod("create", CreateGradingJobRequest.class)
                .isAnnotationPresent(LearnerOnly.class)).isTrue();
        assertThat(GradingJobController.class.getMethod("get", java.util.UUID.class)
                .isAnnotationPresent(LearnerOnly.class)).isTrue();
    }
}
