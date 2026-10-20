package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.LessonResources;
import com.ieltspath.learning.application.service.LessonAccess;
import com.ieltspath.learning.domain.exception.LearningGateException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GetLessonResourcesUseCaseTest {
    private final LearnerLock lock = mock(LearnerLock.class);
    private final LessonAccess access = mock(LessonAccess.class);
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final GetLessonResourcesUseCase useCase = new GetLessonResourcesUseCase(lock, access, content);
    private final UUID user = UUID.randomUUID();
    private final UUID lesson = UUID.randomUUID();

    @Test
    void anOpenLessonReturnsItsResourcesAfterTheGateWithoutTouchingProgress() {
        var resources = new LessonResources(lesson, "## Outcomes", List.of(), List.of());
        when(content.getLessonResources(lesson)).thenReturn(resources);

        assertSame(resources, useCase.execute(user, lesson));

        InOrder order = inOrder(lock, access, content);
        order.verify(lock).lock(user);
        order.verify(access).authorizeToRead(user, lesson);
        order.verify(content).getLessonResources(lesson);
        verify(access, never()).refresh(any(), any(), any());
    }

    @Test
    void aLockedLessonNeverFetchesItsResources() {
        var gate = new LearningGateException("LESSON_LOCKED", List.of());
        when(access.authorizeToRead(user, lesson)).thenThrow(gate);

        assertSame(gate, assertThrows(LearningGateException.class, () -> useCase.execute(user, lesson)));
        verify(content, never()).getLessonResources(any());
    }
}
