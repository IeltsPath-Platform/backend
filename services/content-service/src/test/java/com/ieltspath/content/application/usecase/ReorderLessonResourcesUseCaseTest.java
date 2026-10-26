package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.LessonResource;
import com.ieltspath.content.domain.repository.LessonResourceRepository;
import com.ieltspath.content.domain.vo.LessonResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReorderLessonResourcesUseCaseTest {

    private final LessonResourceRepository repository = mock(LessonResourceRepository.class);
    private final ReorderLessonResourcesUseCase useCase = new ReorderLessonResourcesUseCase(repository);
    private final UUID lessonId = UUID.randomUUID();

    @Test
    void reorderGivesZeroBasedOrdersOfTheRequestedTypeOnly() {
        LessonResource s1 = resource(LessonResourceType.SLIDE, 0);
        LessonResource s2 = resource(LessonResourceType.SLIDE, 1);
        LessonResource document = resource(LessonResourceType.DOCUMENT, 0);
        when(repository.findByLessonId(lessonId)).thenReturn(List.of(s1, s2, document));

        useCase.execute(lessonId, LessonResourceType.SLIDE, List.of(s2.getId(), s1.getId()));

        assertEquals(0, s2.getSortOrder());
        assertEquals(1, s1.getSortOrder());
        assertEquals(0, document.getSortOrder());
        verify(repository, never()).save(document);
    }

    @Test
    void orderCheckIsDeferredBeforeAnyResourceIsWritten() {
        LessonResource a = resource(LessonResourceType.SLIDE, 0);
        LessonResource b = resource(LessonResourceType.SLIDE, 1);
        when(repository.findByLessonId(lessonId)).thenReturn(List.of(a, b));

        useCase.execute(lessonId, LessonResourceType.SLIDE, List.of(b.getId(), a.getId()));

        var order = inOrder(repository);
        order.verify(repository).deferSortOrderCheck();
        order.verify(repository, atLeastOnce()).save(any(LessonResource.class));
    }

    @Test
    void incompleteOrDuplicatedListIsRefusedBeforeWriting() {
        LessonResource a = resource(LessonResourceType.SLIDE, 0);
        LessonResource b = resource(LessonResourceType.SLIDE, 1);
        when(repository.findByLessonId(lessonId)).thenReturn(List.of(a, b));

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(lessonId, LessonResourceType.SLIDE, List.of(a.getId())));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(lessonId, LessonResourceType.SLIDE, List.of(a.getId(), a.getId())));
        verify(repository, never()).deferSortOrderCheck();
        verify(repository, never()).save(any());
    }

    private LessonResource resource(LessonResourceType type, int sortOrder) {
        return LessonResource.create(lessonId, type, "Resource", "https://example.com/" + UUID.randomUUID(), sortOrder);
    }
}
