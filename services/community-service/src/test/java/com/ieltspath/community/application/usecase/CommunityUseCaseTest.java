package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.AddReactionCommand;
import com.ieltspath.community.application.command.ModerateCommentCommand;
import com.ieltspath.community.application.command.ModeratePostCommand;
import com.ieltspath.community.application.command.RemoveReactionCommand;
import com.ieltspath.community.application.query.PageQuery;
import com.ieltspath.community.domain.aggregate.Comment;
import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.exception.CommunityException;
import com.ieltspath.community.domain.exception.CommunityForbiddenException;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.repository.ReactionRepository;
import com.ieltspath.community.domain.vo.CommunityPage;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.domain.vo.ReactionType;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommunityUseCaseTest {
    private final PostRepository posts = mock(PostRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);
    private final ReactionRepository reactions = mock(ReactionRepository.class);
    private final com.ieltspath.community.application.port.CommunityEventOutbox outbox = mock(com.ieltspath.community.application.port.CommunityEventOutbox.class);
    private final com.ieltspath.community.application.port.PublicProfileClient profiles = mock(com.ieltspath.community.application.port.PublicProfileClient.class);

    @Test
    void createsMentionEventsOncePerExistingUserAndNeverForSelf() {
        UUID actorId = UUID.randomUUID(), target = UUID.randomUUID();
        Post post = Post.create(UUID.randomUUID(), PostCategory.GENERAL, null, "body");
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        when(comments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(profiles.findProfiles(Set.of(target, actorId))).thenReturn(Map.of(target, com.ieltspath.community.application.result.PublicProfile.anonymous(target), actorId, com.ieltspath.community.application.result.PublicProfile.anonymous(actorId)));
        var body = "@[A](" + target + ") @[B](" + target + ") @[Me](" + actorId + ")";
        new CreateCommentUseCase(posts, comments, outbox, profiles).execute(new CommunityActor(actorId, false),
                new com.ieltspath.community.application.command.CreateCommentCommand(post.getId(), null, body));
        verify(outbox, times(1)).record(eq("community.comment-mentioned.v1"), eq(actorId), eq(target), eq(post.getId()), any());
        verify(outbox, never()).record(eq("community.comment-mentioned.v1"), eq(actorId), eq(actorId), any(), any());
    }

    @Test
    void rejectsMoreThanFiveDistinctMentions() {
        UUID actorId = UUID.randomUUID();
        Post post = Post.create(UUID.randomUUID(), PostCategory.GENERAL, null, "body");
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        String body = java.util.stream.IntStream.range(0, 6).mapToObj(i -> "@[M](" + UUID.randomUUID() + ")").collect(java.util.stream.Collectors.joining(" "));
        assertThrows(CommunityException.class, () -> new CreateCommentUseCase(posts, comments, outbox, profiles).execute(
                new CommunityActor(actorId, false), new com.ieltspath.community.application.command.CreateCommentCommand(post.getId(), null, body)));
        verifyNoInteractions(outbox);
    }

    @Test
    void rejectsMissingMentionedUsers() {
        UUID actorId = UUID.randomUUID(), target = UUID.randomUUID();
        Post post = Post.create(UUID.randomUUID(), PostCategory.GENERAL, null, "body");
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        when(profiles.findProfiles(Set.of(target))).thenReturn(Map.of());
        assertThrows(CommunityException.class, () -> new CreateCommentUseCase(posts, comments, outbox, profiles).execute(
                new CommunityActor(actorId, false), new com.ieltspath.community.application.command.CreateCommentCommand(post.getId(), null, "@[Member](" + target + ")")));
    }

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
    void listPostsAppliesCategoryFilterInRepository() {
        Post post = Post.create(UUID.randomUUID(), PostCategory.QUESTION, "question", "body");
        when(posts.findByStatusAndCategory(ContentStatus.ACTIVE, PostCategory.QUESTION, 0, 10))
                .thenReturn(new CommunityPage<>(List.of(post), 1));
        when(reactions.counts(anySet())).thenReturn(Map.of());

        var result = new ListPostsUseCase(posts, reactions).execute(new PageQuery(0, 10), PostCategory.QUESTION);

        assertEquals(PostCategory.QUESTION, result.content().getFirst().category());
        assertEquals(1, result.totalElements());
        verify(posts).findByStatusAndCategory(ContentStatus.ACTIVE, PostCategory.QUESTION, 0, 10);
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

        assertThrows(CommunityException.class, () -> new CreateCommentUseCase(posts, comments, outbox, id -> Map.of()).execute(
                new CommunityActor(UUID.randomUUID(), false),
                new com.ieltspath.community.application.command.CreateCommentCommand(
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
                new com.ieltspath.community.application.command.EditCommentCommand(comment.getId(), "changed")
        ));
        verify(comments, never()).save(any());
    }

    @Test
    void editPostEnforcesAuthorOwnershipAndRejectsAdministrator() {
        UUID authorId = UUID.randomUUID();
        Post post = Post.create(authorId, PostCategory.GENERAL, "title", "body");
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));

        assertThrows(CommunityForbiddenException.class, () -> new EditPostUseCase(posts, reactions, (imgs, vid) -> {}).execute(
                new CommunityActor(UUID.randomUUID(), true),
                new com.ieltspath.community.application.command.EditPostCommand(
                        post.getId(), PostCategory.QUESTION, "new title", "new body", java.util.List.of(), null, null
                )
        ));
        verify(posts, never()).save(any());
    }

    @Test
    void editCommentRejectsAdministratorEditingAnotherUserComment() {
        UUID authorId = UUID.randomUUID();
        Comment comment = Comment.create(UUID.randomUUID(), authorId, null, "body");
        when(comments.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertThrows(CommunityForbiddenException.class, () -> new EditCommentUseCase(comments).execute(
                new CommunityActor(UUID.randomUUID(), true),
                new com.ieltspath.community.application.command.EditCommentCommand(comment.getId(), "changed")
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

        new AddReactionUseCase(posts, reactions, outbox).execute(actor, new AddReactionCommand(postId, ReactionType.LIKE));

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
