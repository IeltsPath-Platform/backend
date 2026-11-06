package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.exception.CommunityException;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.CommunityReadRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.PostCategory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FeedQueryLimitsTest {
    private final CommunityReadRepository reads = mock(CommunityReadRepository.class);
    private final PublicProfileClient profiles = mock(PublicProfileClient.class);
    private final CommunityQueriesUseCase queries = new CommunityQueriesUseCase(
            reads, profiles, mock(PostRepository.class), mock(CommentRepository.class),
            mock(com.ieltspath.community.domain.repository.CommentReactionRepository.class));

    private static Post post(UUID author) {
        Instant created = Instant.parse("2026-10-16T10:00:00Z");
        return new Post(UUID.randomUUID(), author, PostCategory.QUESTION, null, "body", ContentStatus.ACTIVE, created, created, 0L);
    }

    @Test
    void lastPageHasNoNextCursor() {
        UUID viewer = UUID.randomUUID();
        when(reads.feed(any(), any(), anyBoolean(), anyBoolean(), any(), any(), anyInt()))
                .thenReturn(List.of(post(viewer), post(viewer)));

        var result = queries.feed(new CommunityActor(viewer, false), null, false, false, null, null, 2);

        assertEquals(2, result.content().size());
        assertFalse(result.hasNext());
        assertNull(result.nextCursor());
    }

    @Test
    void feedWithoutACursorAsksForTheFirstPage() {
        UUID viewer = UUID.randomUUID();
        var actor = new CommunityActor(viewer, false);

        queries.feed(actor, null, false, false, null, null, 20);
        queries.feed(actor, null, false, false, null, "  ", 20);

        org.mockito.Mockito.verify(reads, org.mockito.Mockito.times(2))
                .feed(eq(viewer), isNull(), eq(false), eq(false), isNull(), isNull(), eq(21));
    }

    @Test
    void searchQueryOfExactly120CharactersRuns() {
        UUID viewer = UUID.randomUUID();
        String query = "x".repeat(120);

        queries.searchFeed(new CommunityActor(viewer, false), query, false, null, null, 10);

        verify(reads).search(eq(viewer), eq(false), isNull(), eq(query), isNull(), eq(11));
    }

    @Test
    void missingSearchQueryIsRejectedBeforeAnyLookup() {
        var error = assertThrows(CommunityException.class,
                () -> queries.searchFeed(new CommunityActor(UUID.randomUUID(), false), null, false, null, null, 10));
        assertEquals("Search query is required", error.getMessage());
        verifyNoInteractions(reads, profiles);
    }
}
