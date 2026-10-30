package com.ieltspath.learning.application.service;

import com.ieltspath.learning.application.exception.AccessUnavailableException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearningContentClient.KnowledgePoint;
import com.ieltspath.learning.application.port.LearningContentClient.Lesson;
import com.ieltspath.learning.application.port.LearningContentClient.Topic;
import com.ieltspath.learning.domain.entity.LearnerCourseUnlock;
import com.ieltspath.learning.domain.exception.LearningGateException;
import com.ieltspath.learning.domain.repository.LearnerCourseUnlockRepository;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaywallGateTest {
    private final AccessClient access = mock(AccessClient.class);
    private final LearnerCourseUnlockRepository unlocks = mock(LearnerCourseUnlockRepository.class);
    private final LessonProgressRepository progress = mock(LessonProgressRepository.class);
    private final PaywallGate gate = new PaywallGate(access, unlocks, progress);
    private final UUID user = UUID.randomUUID();
    private final UUID course = UUID.randomUUID();
    private final UUID topicId = UUID.randomUUID();

    @Test
    void freeLessonsDoNotNeedAccess() {
        when(unlocks.findActive(user, course)).thenReturn(List.of());
        assertTrue(gate.forLearner(user).lessonOpen(lesson(3), null));
        verify(access, never()).premium();
    }

    @Test
    void activeSkillUnlockOpensOnlyThatSkillAndDoesNotNeedPremium() {
        when(unlocks.findActive(user, course)).thenReturn(List.of(unlock(LearningSkill.READING)));
        var request = gate.forLearner(user);
        assertTrue(request.lessonOpen(lesson(4), null));
        assertFalse(request.lessonOpen(lesson(4, LearningSkill.WRITING), null));
        verify(access).premium();
    }

    @Test
    void premiumIsMemoizedPerRequestAndOutagePropagatesForLockedLesson() {
        when(unlocks.findActive(user, course)).thenReturn(List.of());
        when(access.premium()).thenReturn(true);
        var request = gate.forLearner(user);
        assertTrue(request.lessonOpen(lesson(4), null));
        assertTrue(request.lessonOpen(lesson(5), null));
        verify(access).premium();

        when(access.premium()).thenThrow(new AccessUnavailableException(503));
        assertThrows(AccessUnavailableException.class,
                () -> gate.forLearner(user).requireLessonOpen(lesson(4), null, Set.of(LearningSkill.READING)));
    }

    @Test
    void knowledgePointWithoutLessonOrCourseDoesNotAskAccess() {
        KnowledgePoint point = new KnowledgePoint(UUID.randomUUID(), "KP", "Point", "PROCEDURE", "READING",
                null, true, null, null, null, null, null);
        Topic topic = new Topic(topicId, "TOPIC", "Topic", 1, List.of(point));
        assertTrue(gate.forLearner(user).knowledgePointOpen(topic, point));
        verify(access, never()).premium();
    }

    @Test
    void grandfatheredKnowledgePointOpensDuringAccessOutage() {
        KnowledgePoint point = new KnowledgePoint(UUID.randomUUID(), "KP", "Point", "PROCEDURE", "READING",
                null, true, null, null, null, null, 4);
        var courseInfo = new com.ieltspath.learning.application.port.LearningContentClient.Course(course, "C",
                "Course", BigDecimal.valueOf(6.5), false);
        Topic topic = new Topic(topicId, "TOPIC", "Topic", 1, null, List.of(point), LearningSkill.READING,
                false, courseInfo);
        when(progress.hasGrandfatheredLessonForKnowledgePoint(user, point.id())).thenReturn(true);
        assertTrue(gate.forLearner(user).knowledgePointOpen(topic, point));
        verify(access, never()).premium();
    }

    @Test
    void pendingAndCancelledRowsNeverUnlock() {
        var pending = new LearnerCourseUnlock(UUID.randomUUID(), user, course, LearningSkill.READING, 50,
                UUID.randomUUID(), LearnerCourseUnlock.Status.PENDING, null);
        when(unlocks.findActive(user, course)).thenReturn(List.of(pending));
        when(access.premium()).thenReturn(false);
        assertEquals("PAYWALL", assertThrows(LearningGateException.class, () -> gate.forLearner(user)
                .requireLessonOpen(lesson(4), null, Set.of(LearningSkill.READING))).getCode());
    }

    private Lesson lesson(int position) { return lesson(position, LearningSkill.READING); }

    private Lesson lesson(int position, LearningSkill skill) {
        return new Lesson(UUID.randomUUID(), topicId, "L", "Lesson", null, position, List.of(), List.of(), skill,
                Set.of(skill), course, position);
    }

    private LearnerCourseUnlock unlock(LearningSkill skill) {
        return new LearnerCourseUnlock(UUID.randomUUID(), user, course, skill, 50, UUID.randomUUID(),
                LearnerCourseUnlock.Status.ACTIVE, UUID.randomUUID());
    }
}
