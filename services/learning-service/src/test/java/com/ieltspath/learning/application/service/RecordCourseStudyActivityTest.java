package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.aggregate.CourseStudyActivity;
import com.ieltspath.learning.domain.aggregate.LearnerCurriculum;
import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.repository.CourseStudyActivityRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecordCourseStudyActivityTest {
    final UUID user = UUID.randomUUID(), course = UUID.randomUUID(), topic = UUID.randomUUID();
    final CourseStudyActivityRepository activities = mock(CourseStudyActivityRepository.class);
    final LearnerCurriculumRepository curricula = mock(LearnerCurriculumRepository.class);
    final RecordCourseStudyActivity recorder = new RecordCourseStudyActivity(activities, curricula);

    @Test void firstAuthorizedStudyCreatesRealTimestampAndLaterStudyPreservesIt() {
        curriculum();
        recorder.record(user, topic);
        var capture = ArgumentCaptor.forClass(CourseStudyActivity.class);
        verify(activities).save(capture.capture());
        var first = capture.getValue();
        assertThat(first.startedAt()).isNotNull().isEqualTo(first.lastStudiedAt());
        reset(activities);
        when(activities.find(user, course)).thenReturn(Optional.of(first));
        recorder.record(user, topic);
        verify(activities).save(capture.capture());
        assertThat(capture.getValue().startedAt()).isEqualTo(first.startedAt());
        verify(activities, never()).hasLegacyStudy(any(), any());
    }

    @Test void historicalProgressDoesNotAcquireAnInventedFirstStudyDate() {
        curriculum();
        when(activities.hasLegacyStudy(user, course)).thenReturn(true);
        recorder.record(user, topic);
        var capture = ArgumentCaptor.forClass(CourseStudyActivity.class);
        verify(activities).save(capture.capture());
        assertThat(capture.getValue().startedAt()).isNull();
        assertThat(capture.getValue().lastStudiedAt()).isNotNull();
    }

    @Test void activityCannotMoveBackwardsOrHaveAnInvalidTimeRange() {
        Instant now = Instant.now();
        var activity = CourseStudyActivity.start(user, course, now, false);
        assertThat(activity.studiedAt(now.minusSeconds(10)).lastStudiedAt()).isEqualTo(now);
        assertThatThrownBy(() -> new CourseStudyActivity(user, course, now, now.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void curriculum() {
        when(curricula.find(user)).thenReturn(LearnerCurriculum.restore(user,
                List.of(new TopicProgress(topic, course, 1, null, LearningSkill.READING, true))));
    }
}
