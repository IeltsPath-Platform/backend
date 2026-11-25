package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.service.KnowledgePointDirectory;
import com.ieltspath.learning.application.usecase.ListSubmissionHistoryUseCase.Scope;
import com.ieltspath.learning.domain.repository.SubmissionHistoryRepository;
import com.ieltspath.learning.domain.repository.SubmissionHistoryRepository.Entry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListSubmissionHistoryUseCaseTest {
    private final SubmissionHistoryRepository history = mock(SubmissionHistoryRepository.class);
    private final KnowledgePointDirectory directory = mock(KnowledgePointDirectory.class);
    private final ListSubmissionHistoryUseCase useCase = new ListSubmissionHistoryUseCase(history, directory);
    private final UUID user = UUID.randomUUID();

    private static List<Entry> rows(int count) {
        return IntStream.range(0, count).mapToObj(index -> new Entry("WRITING", UUID.randomUUID(),
                Instant.parse("2026-11-14T10:00:00Z"), "WRITING", "TASK_2", "stem " + index, null, null, null, null, null,
                "GRADED", null, null, null, null, null, 1)).toList();
    }

    @Test
    void asksForOneRowMoreThanThePageToKnowWhetherThereIsANextPage() {
        when(history.findCourse(user, 4, 0)).thenReturn(rows(4));

        var page = useCase.execute(user, Scope.COURSE, null, 0, 3);

        assertEquals(3, page.items().size());
        assertTrue(page.hasNext());
        assertEquals(3, page.size());
    }

    @Test
    void theLastPageHasNoNext() {
        when(history.findCourse(user, 4, 3)).thenReturn(rows(2));

        var page = useCase.execute(user, Scope.COURSE, "SPEAKING", 1, 3);

        assertEquals(2, page.items().size());
        assertFalse(page.hasNext());
        assertEquals(1, page.page());
    }

    @Test
    void freePracticeReadsTheSkillOfTheRequest() {
        when(history.findFree(user, "SPEAKING", 21, 0)).thenReturn(rows(1));

        var page = useCase.execute(user, Scope.FREE, "SPEAKING", 0, 20);

        assertEquals(1, page.items().size());
        verify(history).findFree(user, "SPEAKING", 21, 0);
    }

    @Test
    void freePracticeNeedsWritingOrSpeaking() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(user, Scope.FREE, null, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(user, Scope.FREE, "READING", 0, 20));
        verifyNoInteractions(history);
    }

    @Test
    void keepsThePageWithinBounds() {
        when(history.findCourse(user, ListSubmissionHistoryUseCase.MAX_PAGE_SIZE + 1, 0)).thenReturn(rows(0));

        assertEquals(ListSubmissionHistoryUseCase.MAX_PAGE_SIZE, useCase.execute(user, Scope.COURSE, null, -4, 10_000).size());
        useCase.execute(user, Scope.COURSE, null, 0, 0);

        verify(history).findCourse(user, 2, 0);
    }

    private static Entry practised(UUID point) {
        UUID attempt = UUID.randomUUID();
        return new Entry("KP_PRACTICE", attempt, Instant.parse("2026-11-14T10:00:00Z"), "READING", null, null, null, null, 3, 4,
                true, null, null, null, null, attempt, null, null, point, null, null);
    }

    @Test
    void namesEachPractisedKnowledgePointFromTheCatalog() {
        UUID named = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        when(history.findKnowledgePractice(user, 21, 0)).thenReturn(List.of(practised(named), practised(unknown)));
        when(directory.describe()).thenReturn(Map.of(named, new KnowledgePointDirectory.Entry("TREND", "Từ vựng xu hướng", "WRITING", null)));

        var page = useCase.execute(user, Scope.KNOWLEDGE, null, 0, 20);

        assertEquals("Từ vựng xu hướng", page.items().getFirst().knowledgePointName());
        assertEquals("TREND", page.items().getFirst().knowledgePointCode());
        assertNull(page.items().getLast().knowledgePointName());
        assertEquals(unknown, page.items().getLast().knowledgePointId());
    }

    @Test
    void stillListsThePractisedPointsWhenTheCatalogIsDown() {
        when(history.findKnowledgePractice(user, 21, 0)).thenReturn(List.of(practised(UUID.randomUUID())));
        when(directory.describe()).thenThrow(new IllegalStateException("content is down"));

        var page = useCase.execute(user, Scope.KNOWLEDGE, null, 0, 20);

        assertEquals(1, page.items().size());
        assertNull(page.items().getFirst().knowledgePointName());
    }

    @Test
    void readsTheCatalogOnlyForAPageOfPractisedPoints() {
        when(history.findKnowledgePractice(user, 21, 0)).thenReturn(List.of());
        when(history.findCourse(user, 21, 0)).thenReturn(rows(1));

        useCase.execute(user, Scope.KNOWLEDGE, null, 0, 20);
        useCase.execute(user, Scope.COURSE, null, 0, 20);

        verifyNoInteractions(directory);
    }
}
