package com.group01.community.application.usecase;

import com.group01.community.application.CommunityActor;
import com.group01.community.application.command.AddReactionCommand;
import com.group01.community.application.command.ModerateCommentCommand;
import com.group01.community.application.command.ModeratePostCommand;
import com.group01.community.application.command.RemoveReactionCommand;
import com.group01.community.application.query.PageQuery;
import com.group01.community.domain.aggregate.Comment;
import com.group01.community.domain.aggregate.Post;
import com.group01.community.domain.exception.CommunityException;
import com.group01.community.domain.exception.CommunityForbiddenException;
import com.group01.community.domain.exception.CommunityNotFoundException;
import com.group01.community.domain.repository.CommentRepository;
import com.group01.community.domain.repository.PostRepository;
import com.group01.community.domain.repository.ReactionRepository;
import com.group01.community.domain.vo.CommunityPage;
import com.group01.community.domain.vo.ContentStatus;
import com.group01.community.domain.vo.PostCategory;
import com.group01.community.domain.vo.ReactionType;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

class CommunityUseCaseTest {
    private final PostRepository posts = mock(PostRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);
    private final ReactionRepository reactions = mock(ReactionRepository.class);

    @Test
    void adminCanModeratePost() {
        UUID adminId = UUID.randomUUID();
        Post post = Post.create(UUID.randomUUID(), PostCategory.GENERAL, null, "body");
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        when(posts.save(post)).thenReturn(post);
        when(reactions.counts(post.getId())).thenReturn(Map.of());

        var result = new ModeratePostUseCase(posts, reactions).execute(
                new CommunityActor(adminId, true), new ModeratePostCommand(post.getId(), ContentStatus.HIDDEN)
        );

        assertEquals(ContentStatus.HIDDEN, result.status());
    }

    @Test
    void nonAdminCannotModeratePost() {
        var useCase = new ModeratePostUseCase(posts, reactions);

        assertThrows(CommunityForbiddenException.class, () -> useCase.execute(
                new CommunityActor(UUID.randomUUID(), false),
                new ModeratePostCommand(UUID.randomUUID(), ContentStatus.HIDDEN)
        ));
        verifyNoInteractions(posts, reactions);
    }

    @Test
    void adminCanModerateComment() {
        Comment comment = Comment.create(UUID.randomUUID(), UUID.randomUUID(), null, "body");
        when(comments.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(comments.save(comment)).thenReturn(comment);

        var result = new ModerateCommentUseCase(comments).execute(
                new CommunityActor(UUID.randomUUID(), true),
                new ModerateCommentCommand(comment.getId(), ContentStatus.HIDDEN)
        );

        assertEquals(ContentStatus.HIDDEN, result.status());
    }

    @Test
    void nonAdminCannotModerateComment() {
        var useCase = new ModerateCommentUseCase(comments);

        assertThrows(CommunityForbiddenException.class, () -> useCase.execute(
                new CommunityActor(UUID.randomUUID(), false),
                new ModerateCommentCommand(UUID.randomUUID(), ContentStatus.HIDDEN)
        ));
        verifyNoInteractions(comments);
    }

    @Test
    void regularUserCannotReadHiddenPost() {
        Post post = Post.create(UUID.randomUUID(), PostCategory.GENERAL, null, "body");
        post.moderate(ContentStatus.HIDDEN);
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));

        assertThrows(CommunityNotFoundException.class, () ->
                new GetPostUseCase(posts, reactions).execute(post.getId(), new CommunityActor(UUID.randomUUID(), false))
        );
        verifyNoInteractions(reactions);
    }

    @Test
    void adminCanReadHiddenPostAndGetsReactionCounts() {
        Post post = Post.create(UUID.randomUUID(), PostCategory.GENERAL, null, "body");
        post.moderate(ContentStatus.HIDDEN);
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        when(reactions.counts(post.getId())).thenReturn(Map.of(ReactionType.LIKE, 2L));

        var result = new GetPostUseCase(posts, reactions).execute(
                post.getId(), new CommunityActor(UUID.randomUUID(), true)
        );

        assertEquals(Map.of(ReactionType.LIKE, 2L), result.reactions());
    }

    @Test
    void listPostsLoadsReactionCountsInOneBatch() {
        Post post = Post.create(UUID.randomUUID(), PostCategory.GENERAL, "title", "body");
        when(posts.findByStatus(ContentStatus.ACTIVE, 2, 5))
                .thenReturn(new CommunityPage<>(List.of(post), 11));
        when(reactions.counts(anySet())).thenReturn(Map.of(post.getId(), Map.of(ReactionType.LIKE, 1L)));

        var result = new ListPostsUseCase(posts, reactions).execute(new PageQuery(2, 5));

        assertEquals(2, result.page());
        assertEquals(5, result.size());
        assertEquals(11, result.totalElements());
        assertEquals(3, result.totalPages());
        assertEquals(Map.of(ReactionType.LIKE, 1L), result.content().getFirst().reactions());
        verify(reactions).counts(Set.of(post.getId()));
    }

    @Test
    void rejectsInvalidPageSize() {
        assertThrows(CommunityException.class, () -> new PageQuery(0, 101));
        verifyNoInteractions(posts, comments, reactions);
    }

    @Test
    void createCommentRejectsParentFromAnotherPost() {
        UUID postId = UUID.randomUUID();
        Post post = Post.create(UUID.randomUUID(), PostCategory.GENERAL, null, "body");
        Comment parent = Comment.create(UUID.randomUUID(), UUID.randomUUID(), null, "parent");
        when(posts.findById(postId)).thenReturn(Optional.of(post));
        when(comments.findByIdForUpdate(parent.getId())).thenReturn(Optional.of(parent));

        assertThrows(CommunityException.class, () -> new CreateCommentUseCase(posts, comments).execute(
                new CommunityActor(UUID.randomUUID(), false),
                new com.group01.community.application.command.CreateCommentCommand(
                        postId, parent.getId(), "child"
                )
        ));
        verify(comments, never()).save(any());
    }

    @Test
    void editCommentEnforcesActorOwnershipThroughDomainAggregate() {
        UUID authorId = UUID.randomUUID();
        Comment comment = Comment.create(UUID.randomUUID(), authorId, null, "body");
        when(comments.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertThrows(CommunityForbiddenException.class, () -> new EditCommentUseCase(comments).execute(
                new CommunityActor(UUID.randomUUID(), false),
                new com.group01.community.application.command.EditCommentCommand(comment.getId(), "changed")
        ));
        verify(comments, never()).save(any());
    }

    @Test
    void addReactionUsesAuthenticatedActorIdAfterVisibilityCheck() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post post = Post.create(userId, PostCategory.GENERAL, null, "body");
        when(posts.findById(postId)).thenReturn(Optional.of(post));
        var actor = new CommunityActor(userId, false);

        new AddReactionUseCase(posts, reactions).execute(actor, new AddReactionCommand(postId, ReactionType.LIKE));

        verify(reactions).add(postId, userId, ReactionType.LIKE);
    }

    @Test
    void removeReactionUsesAuthenticatedActorId() {
        UUID userId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        new RemoveReactionUseCase(reactions).execute(
                new CommunityActor(userId, false), new RemoveReactionCommand(postId, ReactionType.LOVE)
        );

        verify(reactions).remove(postId, userId, ReactionType.LOVE);
    }
}
