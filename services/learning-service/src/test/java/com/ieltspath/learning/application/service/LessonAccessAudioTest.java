package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.Lesson;
import com.ieltspath.learning.application.port.LearningContentClient.LessonSummary;
import com.ieltspath.learning.application.service.LessonAccess.ReadContext;
import com.ieltspath.learning.application.usecase.RefreshLearningTopicsUseCase;
import com.ieltspath.learning.domain.aggregate.LearnerCurriculum;
import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.LearnerCourseUnlockRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.service.TopicStatusDeriver;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.TopicStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LessonAccessAudioTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID topicId = UUID.randomUUID();
    private final UUID lessonId = UUID.randomUUID();
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final LessonProgressRepository lessonProgress = mock(LessonProgressRepository.class);
    private final LearnerCurriculumRepository curricula = mock(LearnerCurriculumRepository.class);
    private final ReviewItemRepository reviews = mock(ReviewItemRepository.class);
    private final KnowledgeEvidenceRepository evidence = mock(KnowledgeEvidenceRepository.class);
    private final RefreshLearningTopicsUseCase refreshTopics = mock(RefreshLearningTopicsUseCase.class);
    private final PracticeProgress practice = mock(PracticeProgress.class);
    private final RecordCourseStudyActivity courseActivity = mock(RecordCourseStudyActivity.class);
    private final PaywallGate paywall = mock(PaywallGate.class);
    private final PaywallGate.Request request = mock(PaywallGate.Request.class);
    private final LessonAccess access = new LessonAccess(content, lessonProgress, curricula, reviews, evidence,
            refreshTopics, practice, courseActivity, paywall);

    private LessonSummary summary() {
        return new LessonSummary(lessonId, topicId, "L4", "Lesson", null, 4, List.of(), List.of(),
                Set.of(LearningSkill.LISTENING), null, courseId, LearningSkill.LISTENING, 4);
    }

    private void initializeAccessibleCurriculum() {
        when(paywall.forLearner(userId)).thenReturn(request);
        when(content.getLessonAccessSummary(lessonId)).thenReturn(summary());
        when(content.getTopicLessons(topicId)).thenReturn(List.of(summary()));
        when(lessonProgress.findByTopic(userId, topicId)).thenReturn(Map.of());
        when(lessonProgress.find(userId, lessonId)).thenReturn(java.util.Optional.empty());
        when(curricula.find(userId)).thenReturn(LearnerCurriculum.restore(userId,
                List.of(TopicProgress.withSkills(topicId, courseId, 1, null, Set.of(LearningSkill.LISTENING), true))));
    }

    @Test
    void blockedLessonDoesNotRequestItsFullContentOrAudioUrl() {
        initializeAccessibleCurriculum();
        doThrow(new LearningGateException("PAYWALL", List.of())).when(request)
                .requireLessonOpen(any(Lesson.class), isNull(), anySet());

        assertThrows(LearningGateException.class, () -> access.authorizeAndLoadForReading(userId, lessonId));

        verify(content).getLessonAccessSummary(lessonId);
        verify(content, never()).getLesson(lessonId, true);
    }

    @Test
    void openLessonFetchesAudioOnlyAfterItsAccessCheck() {
        initializeAccessibleCurriculum();
        Lesson full = new Lesson(lessonId, topicId, "L4", "Lesson", null, 4, List.of(), List.of(),
                LearningSkill.LISTENING, Set.of(LearningSkill.LISTENING), courseId, 4);
        when(content.getLesson(lessonId, true)).thenReturn(full);

        ReadContext result = access.authorizeAndLoadForReading(userId, lessonId);

        assertEquals(full, result.lesson());
        verify(content).getLesson(lessonId, true);
    }
}
