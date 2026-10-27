package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.domain.repository.HumanReviewRequestRepository;
import com.ieltspath.learning.domain.repository.HumanReviewRequestRepository.Requested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListExaminerReviewsUseCaseTest {
    private final HumanReviewRequestRepository requests = mock(HumanReviewRequestRepository.class);
    private final ListExaminerReviewsUseCase useCase = new ListExaminerReviewsUseCase(requests);
    private final UUID user = UUID.randomUUID();

    private static List<Requested> rows(int count) {
        return IntStream.range(0, count).mapToObj(index -> new Requested(UUID.randomUUID(), UUID.randomUUID(),
                "FREE_WRITING", "TASK_2", "stem " + index, 250, Instant.parse("2026-10-26T10:00:00Z"))).toList();
    }

    @Test
    void asksForOneRowMoreThanThePageToKnowWhetherThereIsANextPage() {
        when(requests.findByUser(user, 4, 0)).thenReturn(rows(4));

        var page = useCase.execute(user, 0, 3);

        assertEquals(3, page.items().size());
        assertTrue(page.hasNext());
        assertEquals(3, page.size());
    }

    @Test
    void theLastPageHasNoNext() {
        when(requests.findByUser(user, 4, 3)).thenReturn(rows(2));

        var page = useCase.execute(user, 1, 3);

        assertEquals(2, page.items().size());
        assertFalse(page.hasNext());
        assertEquals(1, page.page());
    }

    @Test
    void keepsThePageWithinBounds() {
        when(requests.findByUser(user, ListExaminerReviewsUseCase.MAX_PAGE_SIZE + 1, 0)).thenReturn(rows(0));

        assertEquals(ListExaminerReviewsUseCase.MAX_PAGE_SIZE, useCase.execute(user, -4, 10_000).size());
        useCase.execute(user, 0, 0);

        verify(requests).findByUser(user, 2, 0);
    }
}
