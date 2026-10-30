package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.result.TopicResult;
import com.ieltspath.learning.domain.aggregate.LearnerCurriculum;
import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;
import com.ieltspath.learning.domain.repository.CourseProgressRepository;
import com.ieltspath.learning.domain.repository.LearnerCourseUnlockRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCoursesUnlockFieldsTest {
    @Mock LearnerLock lock;
    @Mock RefreshLearningTopicsUseCase refresh;
    @Mock LearnerCurriculumRepository curricula;
    @Mock CourseProgressRepository courseProgress;
    @Mock LearnerPlacementRepository placements;
    @Mock LessonProgressRepository lessonProgress;
    @Mock LearnerCourseUnlockRepository unlocks;
    @Mock ReconcilePendingCourseUnlocks reconciler;
    @Mock AccessClient access;
    @InjectMocks ListCoursesUseCase useCase;

    @Test
    void courseListReportsOwnedSkillsComboPriceAndPremiumState() {
        UUID user = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        var course = new TopicResult.Course(courseId, "IELTS_6_0", "IELTS 6.0", new BigDecimal("6.0"),
                "https://cdn.example/cover.jpg", "Description",
                Map.of(LearningSkill.READING, 100, LearningSkill.LISTENING, 120), 350,
                Set.of(LearningSkill.READING, LearningSkill.LISTENING));
        when(refresh.execute(user)).thenReturn(List.of(new TopicResult(topicId, "R1", "Reading", 1,
                com.ieltspath.learning.domain.vo.TopicStatus.IN_PROGRESS, 0, null, LearningSkill.READING,
                true, course, false)));
        when(curricula.find(user)).thenReturn(LearnerCurriculum.restore(user, List.of()));
        when(courseProgress.findAll(user)).thenReturn(Map.of());
        when(placements.find(user)).thenReturn(java.util.Optional.empty());
        when(lessonProgress.lastStudiedByTopic(user)).thenReturn(Map.of());
        when(unlocks.findActive(user, courseId)).thenReturn(List.of(new LearnerCourseUnlock(UUID.randomUUID(), user,
                courseId, LearningSkill.READING, 80, UUID.randomUUID(), LearnerCourseUnlock.Status.ACTIVE,
                UUID.randomUUID())));
        when(access.premium()).thenReturn(true);

        var result = useCase.execute(user).getFirst();

        assertThat(result.skillPrices()).containsEntry(LearningSkill.READING, 100);
        assertThat(result.availableSkills()).containsExactlyInAnyOrder(LearningSkill.READING, LearningSkill.LISTENING);
        assertThat(result.unlockedSkills()).containsExactly(LearningSkill.READING);
        assertThat(result.comboUnlocked()).isFalse();
        assertThat(result.comboPayablePoints()).isEqualTo(270);
        assertThat(result.premium()).isTrue();
    }
}
