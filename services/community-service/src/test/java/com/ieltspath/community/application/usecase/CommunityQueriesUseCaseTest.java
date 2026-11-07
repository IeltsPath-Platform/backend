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
    @Mock com.ieltspath.community.domain.repository.CommentReactionRepository commentReactions;
    @InjectMocks CommunityQueriesUseCase queries;

    @Test
    void commentsBatchInteractionsOnceAndUseCurrentViewerWithFourZeroDefaults() {
        UUID viewer = UUID.randomUUID(), author = UUID.randomUUID();
        var content = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> com.ieltspath.community.application.result.CommentResult.from(
                        Comment.create(UUID.randomUUID(), author, null, "body " + i))).toList();
        var ids = content.stream().map(com.ieltspath.community.application.result.CommentResult::id)
                .collect(java.util.stream.Collectors.toSet());
        UUID reacted = content.getFirst().id();
        when(profiles.findProfiles(Set.of(author))).thenReturn(Map.of());
        when(reads.replyCounts(ids)).thenReturn(Map.of(reacted, 3L));
        when(commentReactions.interactions(ids, viewer)).thenReturn(Map.of(reacted,
                new com.ieltspath.community.domain.vo.CommentInteractions(Map.of(
                        com.ieltspath.community.domain.vo.ReactionType.LIKE, 2L),
                        com.ieltspath.community.domain.vo.ReactionType.LIKE)));
        var result = queries.enrichComments(new CommunityActor(viewer, false), content);
        assertEquals(content.stream().map(com.ieltspath.community.application.result.CommentResult::id).toList(),
                result.stream().map(com.ieltspath.community.application.result.CommentResult::id).toList());
        assertEquals(3L, result.getFirst().replyCount());
        assertEquals(2L, result.getFirst().reactionCounts().get(com.ieltspath.community.domain.vo.ReactionType.LIKE));
        assertEquals(com.ieltspath.community.domain.vo.ReactionType.LIKE, result.getFirst().myReaction());
        for (var item : result) {
            assertEquals(4, item.reactionCounts().size());
            assertEquals(author, item.author().id());
        }
        assertTrue(result.getLast().reactionCounts().values().stream().allMatch(count -> count == 0L));
        assertNull(result.getLast().myReaction());
        verify(commentReactions, times(1)).interactions(ids, viewer);
        verifyNoMoreInteractions(commentReactions);
    }

    @Test
    void emptyCommentsAvoidAllEnrichmentQueries() {
        assertEquals(List.of(), queries.enrichComments(new CommunityActor(UUID.randomUUID(), false), List.of()));
        verifyNoInteractions(reads, profiles, commentReactions);
    }

    @Test
    void threadedRootsAndRepliesBothEnrichUsingViewer() {
        UUID viewer = UUID.randomUUID();
        var actor = new CommunityActor(viewer, false);
        Post post = Post.create(viewer, PostCategory.GENERAL, null, "post");
        Comment root = Comment.create(post.getId(), viewer, null, "root");
        Comment reply = Comment.create(post.getId(), viewer, root.getId(), "reply");
        when(posts.findById(post.getId())).thenReturn(java.util.Optional.of(post));
        when(comments.findById(root.getId())).thenReturn(java.util.Optional.of(root));
        when(reads.thread(post.getId(), null, 0, 20)).thenReturn(new com.ieltspath.community.domain.vo.CommunityPage<>(List.of(root), 1));
        when(reads.thread(post.getId(), root.getId(), 0, 20)).thenReturn(new com.ieltspath.community.domain.vo.CommunityPage<>(List.of(reply), 1));
        when(profiles.findProfiles(anySet())).thenReturn(Map.of());
        when(reads.replyCounts(anySet())).thenReturn(Map.of());
        when(commentReactions.interactions(anySet(), eq(viewer))).thenAnswer(invocation -> {
            Set<UUID> ids = invocation.getArgument(0);
            return ids.stream().collect(java.util.stream.Collectors.toMap(id -> id, id ->
                    new com.ieltspath.community.domain.vo.CommentInteractions(Map.of(), com.ieltspath.community.domain.vo.ReactionType.ACCURATE)));
        });
        var query = new com.ieltspath.community.application.query.PageQuery(0, 20);
        assertEquals(com.ieltspath.community.domain.vo.ReactionType.ACCURATE,
                queries.thread(actor, post.getId(), null, query).content().getFirst().myReaction());
        assertEquals(com.ieltspath.community.domain.vo.ReactionType.ACCURATE,
                queries.thread(actor, null, root.getId(), query).content().getFirst().myReaction());
        verify(commentReactions).interactions(Set.of(root.getId()), viewer);
        verify(commentReactions).interactions(Set.of(reply.getId()), viewer);
    }

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
