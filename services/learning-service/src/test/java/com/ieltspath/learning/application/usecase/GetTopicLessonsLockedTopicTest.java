package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.LessonSummary;
import com.ieltspath.learning.application.service.LessonAccess;
import com.ieltspath.learning.application.service.PaywallGate;
import com.ieltspath.learning.application.service.PracticeProgress;
import com.ieltspath.learning.domain.entity.TopicProgress;
import com.ieltspath.learning.domain.repository.LessonProgressRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticeStatus;
import com.ieltspath.learning.domain.vo.TopicStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A chặng the learner has not reached yet still lists its lessons, all closed, so the course can be read ahead. */
class GetTopicLessonsLockedTopicTest {
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final LessonAccess access = mock(LessonAccess.class);
    private final LessonProgressRepository progress = mock(LessonProgressRepository.class);
    private final PracticeProgress practice = mock(PracticeProgress.class);
    private final PaywallGate paywall = mock(PaywallGate.class);
    private final PaywallGate.Request request = mock(PaywallGate.Request.class);
    private final GetTopicLessonsUseCase useCase =
            new GetTopicLessonsUseCase(content, access, progress, practice, paywall);
    private final UUID user = UUID.randomUUID();
    private final UUID topic = UUID.randomUUID();
    private final UUID course = UUID.randomUUID();

    private LessonSummary lesson(String code, int sortOrder, int skillPosition) {
        return new LessonSummary(UUID.randomUUID(), topic, code, "Bài " + code, null, sortOrder, List.of(), List.of(),
                Set.of(LearningSkill.READING), 10, course, LearningSkill.READING, skillPosition);
    }

    private void lockedTopic(boolean hasTest) {
        var progressRow = new TopicProgress(topic, 2, null, LearningSkill.READING, hasTest);
        when(access.topic(user, topic)).thenReturn(progressRow);
        when(access.status(user, topic)).thenReturn(TopicStatus.LOCKED);
        when(paywall.forLearner(user)).thenReturn(request);
        when(content.getTopicLessons(topic)).thenReturn(List.of(lesson("L2", 2, 5), lesson("L1", 1, 4)));
        when(request.freePreview(any(), any())).thenReturn(false);
    }

    @Test
    void aLockedTopicListsItsLessonsInOrderAllClosedWithoutTouchingProgress() {
        lockedTopic(false);
        when(request.lessonOpen(any(LessonSummary.class), any())).thenReturn(true);

        var result = useCase.execute(user, topic);

        assertEquals(List.of("L1", "L2"), result.lessons().stream().map(row -> row.code()).toList());
        assertTrue(result.lessons().stream().allMatch(row -> row.status().equals("LOCKED")));
        assertTrue(result.lessons().stream().allMatch(row -> row.practiceStatus() == PracticeStatus.LOCKED));
        assertTrue(result.lessons().stream().allMatch(row -> row.blockingReviewId() == null));
        verify(progress, never()).findByTopic(any(), any());
        verify(practice, never()).forTopic(any(), any(), any());
    }

    @Test
    void aPaywalledLessonOfALockedTopicStaysMarkedSoThePageCanOfferTheUnlock() {
        lockedTopic(false);
        when(request.lessonOpen(any(LessonSummary.class), any())).thenReturn(false);

        var result = useCase.execute(user, topic);

        assertTrue(result.lessons().stream().allMatch(row -> row.paywall()));
    }

    @Test
    void theTestOfALockedTopicIsLockedAndATopicWithoutOneHasNone() {
        lockedTopic(false);
        when(request.lessonOpen(any(LessonSummary.class), any())).thenReturn(true);
        assertEquals("NONE", useCase.execute(user, topic).testStatus());

        lockedTopic(true);
        assertEquals("LOCKED", useCase.execute(user, topic).testStatus());
    }
}
