package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.aggregate.CourseStudyActivity;
import com.ieltspath.learning.domain.repository.CourseStudyActivityRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

@Component
public class RecordCourseStudyActivity {
    private final CourseStudyActivityRepository activities;
    private final LearnerCurriculumRepository curricula;
    public RecordCourseStudyActivity(CourseStudyActivityRepository activities, LearnerCurriculumRepository curricula) {
        this.activities = activities;
        this.curricula = curricula;
    }

    /** Caller holds the learner lock, after authorization and before the first progress row is inserted. */
    public void record(UUID userId, UUID topicId) {
        UUID courseId = curricula.find(userId).topic(topicId).map(topic -> topic.courseId()).orElse(null);
        if (courseId == null) return;
        Instant now = Instant.now();
        var activity = activities.find(userId, courseId)
                .map(current -> current.studiedAt(now))
                .orElseGet(() -> CourseStudyActivity.start(userId, courseId, now, activities.hasLegacyStudy(userId, courseId)));
        activities.save(activity);
    }
}
