package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.domain.aggregate.Comment;
import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.exception.CommunityException;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.CommunityReadRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.FeedCursor;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.domain.vo.PostInteractions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityQueriesUseCaseTest {
    @Mock CommunityReadRepository reads;
    @Mock PublicProfileClient profiles;
    @Mock PostRepository posts;
    @Mock CommentRepository comments;
    @InjectMocks CommunityQueriesUseCase queries;

    @Test
    void followingFeedUsesViewerAndCursorAndReturnsNextCursor() {
        UUID viewer = UUID.randomUUID();
        Instant created = Instant.parse("2026-10-16T10:00:00Z");
        Post first = post(UUID.randomUUID(), viewer, created);
        Post second = post(UUID.randomUUID(), viewer, created);
        Post lookahead = post(UUID.randomUUID(), viewer, created);
        when(reads.feed(eq(viewer), isNull(), eq(false), eq(true), eq(PostCategory.QUESTION),
                eq(new FeedCursor(created, first.getId())), eq(3)))
                .thenReturn(List.of(first, second, lookahead));
        when(profiles.findProfiles(anySet())).thenReturn(Map.of());
        when(reads.interactions(anySet(), eq(viewer))).thenReturn(Map.of());

        var result = queries.feed(new CommunityActor(viewer, false), null, false, true,
                PostCategory.QUESTION, new FeedCursor(created, first.getId()).encode(), 2);

        assertEquals(List.of(first.getId(), second.getId()), result.content().stream().map(item -> item.id()).toList());
        assertTrue(result.hasNext());
        assertEquals(new FeedCursor(created, second.getId()), FeedCursor.parse(result.nextCursor()));
        verify(reads).feed(eq(viewer), isNull(), eq(false), eq(true), eq(PostCategory.QUESTION),
                eq(new FeedCursor(created, first.getId())), eq(3));
    }

    @Test
    void invalidCursorIsRejectedBeforeRepositoryAccess() {
        assertThrows(CommunityException.class,
                () -> queries.feed(new CommunityActor(UUID.randomUUID(), false), null, false, true,
                        null, "malformed", 10));
        verifyNoInteractions(reads, profiles);
    }

    @Test
    void invalidPageSizeIsRejectedBeforeRepositoryAccess() {
        assertThrows(RuntimeException.class,
                () -> queries.feed(new CommunityActor(UUID.randomUUID(), false), null, false, true,
                        null, null, 101));
        verifyNoInteractions(reads, profiles);
    }

    @Test
    void searchFeedNormalizesQueryAndUsesTheCursorLookahead() {
        UUID viewer = UUID.randomUUID();
        Instant created = Instant.parse("2026-10-16T10:00:00Z");
        Post first = post(UUID.randomUUID(), viewer, created);
        Post second = post(UUID.randomUUID(), viewer, created);
        Post lookahead = post(UUID.randomUUID(), viewer, created);
        var cursor = new FeedCursor(created, first.getId());
        when(reads.search(eq(viewer), eq(true), eq(PostCategory.QUESTION), eq("Reading strategy"),
                eq(cursor), eq(3))).thenReturn(List.of(first, second, lookahead));
        when(profiles.findProfiles(anySet())).thenReturn(Map.of());
        when(reads.interactions(anySet(), eq(viewer))).thenReturn(Map.of());

        var result = queries.searchFeed(new CommunityActor(viewer, false), "  Reading   strategy ", true,
                PostCategory.QUESTION, cursor.encode(), 2);

        assertEquals(List.of(first.getId(), second.getId()), result.content().stream().map(item -> item.id()).toList());
        assertTrue(result.hasNext());
        assertEquals(new FeedCursor(created, second.getId()), FeedCursor.parse(result.nextCursor()));
        verify(reads).search(eq(viewer), eq(true), eq(PostCategory.QUESTION), eq("Reading strategy"),
                eq(cursor), eq(3));
    }

    @Test
    void searchRejectsBlankOrOverlongQueryBeforeRepositoryAccess() {
        var actor = new CommunityActor(UUID.randomUUID(), false);
        assertAll(
                () -> assertThrows(CommunityException.class,
                        () -> queries.searchFeed(actor, "   ", false, null, null, 10)),
                () -> assertThrows(CommunityException.class,
                        () -> queries.searchFeed(actor, "x".repeat(121), false, null, null, 10))
        );
        verifyNoInteractions(reads, profiles);
    }

    private static Post post(UUID id, UUID author, Instant created) {
        return new Post(id, author, PostCategory.QUESTION, null, "body", ContentStatus.ACTIVE,
                created, created, 0L);
    }
}
