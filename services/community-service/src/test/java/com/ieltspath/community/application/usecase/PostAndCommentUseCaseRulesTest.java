package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.AddReactionCommand;
import com.ieltspath.community.application.command.CreateCommentCommand;
import com.ieltspath.community.application.command.CreatePostCommand;
import com.ieltspath.community.application.command.DeleteCommentCommand;
import com.ieltspath.community.application.command.DeletePostCommand;
import com.ieltspath.community.application.command.EditCommentCommand;
import com.ieltspath.community.application.command.EditPostCommand;
import com.ieltspath.community.application.command.ModerateCommentCommand;
import com.ieltspath.community.application.command.ModeratePostCommand;
import com.ieltspath.community.application.port.CommunityEventOutbox;
import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.application.query.PageQuery;
import com.ieltspath.community.application.result.PublicProfile;
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
import com.ieltspath.events.community.CommunityEventTypes;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostAndCommentUseCaseRulesTest {
    private final PostRepository posts = mock(PostRepository.class);
    private final CommentRepository comments = mock(CommentRepository.class);
    private final ReactionRepository reactions = mock(ReactionRepository.class);
    private final CommunityEventOutbox outbox = mock(CommunityEventOutbox.class);
    private final PublicProfileClient profiles = mock(PublicProfileClient.class);

    private static CommunityActor user(UUID id) {
        return new CommunityActor(id, false);
    }

    private static CommunityActor admin() {
        return new CommunityActor(UUID.randomUUID(), true);
    }

    private static Post post(UUID author) {
        return Post.create(author, PostCategory.GENERAL, "Title", "Body");
    }

    private static TransactionTemplate immediateTransaction() {
        var template = mock(TransactionTemplate.class);
        when(template.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(null));
        return template;
    }

    private CreateCommentUseCase createComment() {
        return new CreateCommentUseCase(posts, comments, outbox, profiles, immediateTransaction());
    }

    // ---- create and edit post -------------------------------------------------------------------------------

    @Test
    void createdPostBelongsToTheSignedInActor() {
        UUID actorId = UUID.randomUUID();
        when(posts.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reactions.counts(any(UUID.class))).thenReturn(Map.of());

        new CreatePostUseCase(posts, reactions, (author, images, video) -> { })
                .execute(user(actorId), new CreatePostCommand(PostCategory.GENERAL, null, "text"));

        var saved = ArgumentCaptor.forClass(Post.class);
        verify(posts).save(saved.capture());
        assertEquals(actorId, saved.getValue().getAuthorId());
    }

    @Test
    void mediaPolicyRejectionSavesNoPost() {
        var useCase = new CreatePostUseCase(posts, reactions, (author, images, video) -> {
            throw new CommunityException("Media URL must use HTTPS and an allowed host");
        });
        var command = new CreatePostCommand(PostCategory.GENERAL, null, "text",
                List.of("https://evil.com/a.png"), null, null, null);
        assertThrows(CommunityException.class, () -> useCase.execute(user(UUID.randomUUID()), command));
        verify(posts, never()).save(any());
    }

    @Test
    void editingAPostThatDoesNotExistIsNotFound() {
        UUID postId = UUID.randomUUID();
        when(posts.findById(postId)).thenReturn(Optional.empty());
        var error = assertThrows(CommunityNotFoundException.class,
                () -> new EditPostUseCase(posts, reactions, (author, images, video) -> { }).execute(user(UUID.randomUUID()),
                        new EditPostCommand(postId, PostCategory.GENERAL, null, "New", List.of(), null, null, null)));
        assertEquals("Post not found", error.getMessage());
        verify(posts, never()).save(any());
    }

    @Test
    void authorCannotEditTheirHiddenPostThroughTheUseCase() {
        UUID author = UUID.randomUUID();
        Post p = post(author);
        p.moderate(ContentStatus.HIDDEN);
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        var error = assertThrows(CommunityException.class,
                () -> new EditPostUseCase(posts, reactions, (a, images, video) -> { }).execute(user(author),
                        new EditPostCommand(p.getId(), PostCategory.GENERAL, null, "New", List.of(), null, null, null)));
        assertEquals("Only active posts may be changed", error.getMessage());
        verify(posts, never()).save(any());
    }

    // ---- delete post and comment, edit comment ---------------------------------------------------------------

    @Test
    void authorDeletesTheirPostAndTheRecordIsKept() {
        UUID author = UUID.randomUUID();
        Post p = post(author);
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));

        new DeletePostUseCase(posts).execute(user(author), new DeletePostCommand(p.getId()));

        assertEquals(ContentStatus.DELETED, p.getStatus());
        verify(posts).save(p);
    }

    @Test
    void anotherUserCannotDeleteAPostThroughTheUseCase() {
        Post p = post(UUID.randomUUID());
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        var error = assertThrows(CommunityForbiddenException.class,
                () -> new DeletePostUseCase(posts).execute(user(UUID.randomUUID()), new DeletePostCommand(p.getId())));
        assertEquals("Only the author may change this post", error.getMessage());
        verify(posts, never()).save(any());
    }

    @Test
    void deletingAPostThatDoesNotExistIsNotFound() {
        UUID postId = UUID.randomUUID();
        when(posts.findById(postId)).thenReturn(Optional.empty());
        var error = assertThrows(CommunityNotFoundException.class,
                () -> new DeletePostUseCase(posts).execute(user(UUID.randomUUID()), new DeletePostCommand(postId)));
        assertEquals("Post not found", error.getMessage());
        verify(posts, never()).save(any());
    }

    @Test
    void authorEditsTheirCommentThroughTheUseCase() {
        UUID author = UUID.randomUUID();
        Comment c = Comment.create(UUID.randomUUID(), author, null, "body");
        when(comments.findById(c.getId())).thenReturn(Optional.of(c));
        when(comments.save(c)).thenReturn(c);

        var result = new EditCommentUseCase(comments).execute(user(author), new EditCommentCommand(c.getId(), "changed"));

        assertEquals("changed", result.body());
        verify(comments).save(c);
    }

    @Test
    void authorDeletesTheirCommentThroughTheUseCase() {
        UUID author = UUID.randomUUID();
        Comment c = Comment.create(UUID.randomUUID(), author, null, "body");
        when(comments.findById(c.getId())).thenReturn(Optional.of(c));

        new DeleteCommentUseCase(comments).execute(user(author), new DeleteCommentCommand(c.getId()));

        assertEquals(ContentStatus.DELETED, c.getStatus());
        verify(comments).save(c);
    }

    @Test
    void deletingACommentThatDoesNotExistIsNotFound() {
        UUID commentId = UUID.randomUUID();
        when(comments.findById(commentId)).thenReturn(Optional.empty());
        var error = assertThrows(CommunityNotFoundException.class,
                () -> new DeleteCommentUseCase(comments).execute(user(UUID.randomUUID()), new DeleteCommentCommand(commentId)));
        assertEquals("Comment not found", error.getMessage());
        verify(comments, never()).save(any());
    }

    // ---- create comment -------------------------------------------------------------------------------------

    @Test
    void commentOnAnotherUsersPostNotifiesThePostAuthor() {
        UUID postAuthor = UUID.randomUUID(), commenter = UUID.randomUUID();
        Post p = post(postAuthor);
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        when(comments.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = createComment().execute(user(commenter), new CreateCommentCommand(p.getId(), null, "text"));

        assertNull(result.parentCommentId());
        verify(outbox).record(eq(CommunityEventTypes.POST_COMMENTED), eq(commenter), eq(postAuthor), eq(p.getId()), any());
    }

    @Test
    void replyNotifiesTheAuthorOfTheParentComment() {
        UUID commenter = UUID.randomUUID(), parentAuthor = UUID.randomUUID();
        Post p = post(UUID.randomUUID());
        Comment parent = Comment.create(p.getId(), parentAuthor, null, "parent");
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        when(comments.findByIdForUpdate(parent.getId())).thenReturn(Optional.of(parent));
        when(comments.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = createComment().execute(user(commenter), new CreateCommentCommand(p.getId(), parent.getId(), "reply"));

        assertEquals(parent.getId(), result.parentCommentId());
        verify(outbox).record(eq(CommunityEventTypes.COMMENT_REPLIED), eq(commenter), eq(parentAuthor), eq(p.getId()), any());
        verify(outbox, never()).record(eq(CommunityEventTypes.POST_COMMENTED), any(), any(), any(), any());
    }

    @Test
    void replyToAReplyIsAttachedToTheRootComment() {
        UUID commenter = UUID.randomUUID();
        Post p = post(UUID.randomUUID());
        Comment root = Comment.create(p.getId(), UUID.randomUUID(), null, "root");
        Comment firstReply = Comment.create(p.getId(), UUID.randomUUID(), root.getId(), "first reply");
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        when(comments.findByIdForUpdate(firstReply.getId())).thenReturn(Optional.of(firstReply));
        when(comments.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = createComment().execute(user(commenter), new CreateCommentCommand(p.getId(), firstReply.getId(), "reply to reply"));

        assertEquals(root.getId(), result.parentCommentId());
    }

    @Test
    void replyToAHiddenOrDeletedCommentIsRejected() {
        Post p = post(UUID.randomUUID());
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        Comment hidden = Comment.create(p.getId(), UUID.randomUUID(), null, "hidden");
        hidden.moderate(ContentStatus.HIDDEN);
        UUID deletedAuthor = UUID.randomUUID();
        Comment deleted = Comment.create(p.getId(), deletedAuthor, null, "deleted");
        deleted.delete(deletedAuthor);
        when(comments.findByIdForUpdate(hidden.getId())).thenReturn(Optional.of(hidden));
        when(comments.findByIdForUpdate(deleted.getId())).thenReturn(Optional.of(deleted));

        for (Comment parent : List.of(hidden, deleted)) {
            var error = assertThrows(CommunityException.class, () -> createComment().execute(user(UUID.randomUUID()),
                    new CreateCommentCommand(p.getId(), parent.getId(), "child")));
            assertEquals("Parent comment must be active and belong to the same post", error.getMessage());
        }
        verify(comments, never()).save(any());
    }

    @Test
    void replyToACommentThatDoesNotExistIsNotFound() {
        Post p = post(UUID.randomUUID());
        UUID parentId = UUID.randomUUID();
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        when(comments.findByIdForUpdate(parentId)).thenReturn(Optional.empty());
        var error = assertThrows(CommunityNotFoundException.class, () -> createComment().execute(user(UUID.randomUUID()),
                new CreateCommentCommand(p.getId(), parentId, "child")));
        assertEquals("Comment not found", error.getMessage());
        verify(comments, never()).save(any());
    }

    @Test
    void commentOnAHiddenPostByANormalUserIsNotFound() {
        Post p = post(UUID.randomUUID());
        p.moderate(ContentStatus.HIDDEN);
        UUID missing = UUID.randomUUID();
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        when(posts.findById(missing)).thenReturn(Optional.empty());
        for (UUID postId : List.of(p.getId(), missing)) {
            var error = assertThrows(CommunityNotFoundException.class,
                    () -> createComment().execute(user(UUID.randomUUID()), new CreateCommentCommand(postId, null, "text")));
            assertEquals("Post not found", error.getMessage());
        }
        verify(comments, never()).save(any());
    }

    @Test
    void exactlyFiveDistinctMentionsAreAccepted() {
        UUID actor = UUID.randomUUID();
        Post p = post(UUID.randomUUID());
        Set<UUID> mentioned = IntStream.range(0, 5).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toSet());
        String body = mentioned.stream().map(id -> "@[M](" + id + ")").collect(Collectors.joining(" "));
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        when(comments.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profiles.findProfiles(mentioned)).thenReturn(
                mentioned.stream().collect(Collectors.toMap(id -> id, PublicProfile::anonymous)));

        createComment().execute(user(actor), new CreateCommentCommand(p.getId(), null, body));

        verify(outbox, times(5)).record(eq(CommunityEventTypes.COMMENT_MENTIONED), eq(actor), any(), eq(p.getId()), any());
    }

    @Test
    void mentionedPostAuthorGetsOnlyTheMentionEvent() {
        UUID actor = UUID.randomUUID(), postAuthor = UUID.randomUUID();
        Post p = post(postAuthor);
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        when(comments.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(profiles.findProfiles(Set.of(postAuthor))).thenReturn(Map.of(postAuthor, PublicProfile.anonymous(postAuthor)));

        createComment().execute(user(actor), new CreateCommentCommand(p.getId(), null, "@[Author](" + postAuthor + ") thanks"));

        verify(outbox, times(1)).record(eq(CommunityEventTypes.COMMENT_MENTIONED), eq(actor), eq(postAuthor), eq(p.getId()), any());
        verify(outbox, never()).record(eq(CommunityEventTypes.POST_COMMENTED), any(), any(), any(), any());
    }

    // ---- reactions ------------------------------------------------------------------------------------------

    @Test
    void firstReactionOnAnotherUsersPostNotifiesTheAuthor() {
        UUID postAuthor = UUID.randomUUID(), reactor = UUID.randomUUID();
        Post p = post(postAuthor);
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        when(reactions.add(p.getId(), reactor, ReactionType.LIKE)).thenReturn(true);

        new AddReactionUseCase(posts, reactions, outbox).execute(user(reactor), new AddReactionCommand(p.getId(), ReactionType.LIKE));

        verify(outbox).record(CommunityEventTypes.POST_REACTED, reactor, postAuthor, p.getId(), null);
    }

    @Test
    void repeatingTheSameReactionChangesNothingAndSendsNoEvent() {
        UUID reactor = UUID.randomUUID();
        Post p = post(UUID.randomUUID());
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        when(reactions.add(p.getId(), reactor, ReactionType.LIKE)).thenReturn(false);

        new AddReactionUseCase(posts, reactions, outbox).execute(user(reactor), new AddReactionCommand(p.getId(), ReactionType.LIKE));

        verifyNoInteractions(outbox);
    }

    @Test
    void reactionOnAHiddenPostByANormalUserIsNotFound() {
        Post p = post(UUID.randomUUID());
        p.moderate(ContentStatus.HIDDEN);
        UUID missing = UUID.randomUUID();
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        when(posts.findById(missing)).thenReturn(Optional.empty());
        for (UUID postId : List.of(p.getId(), missing)) {
            var error = assertThrows(CommunityNotFoundException.class, () -> new AddReactionUseCase(posts, reactions, outbox)
                    .execute(user(UUID.randomUUID()), new AddReactionCommand(postId, ReactionType.LIKE)));
            assertEquals("Post not found", error.getMessage());
        }
        verify(reactions, never()).add(any(), any(), any());
    }

    @Test
    void allFourReactionTypesAreAccepted() {
        UUID reactor = UUID.randomUUID();
        Post p = post(reactor);
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        assertEquals(4, ReactionType.values().length);
        for (ReactionType type : ReactionType.values()) {
            new AddReactionUseCase(posts, reactions, outbox).execute(user(reactor), new AddReactionCommand(p.getId(), type));
            verify(reactions).add(p.getId(), reactor, type);
        }
    }

    // ---- moderation -----------------------------------------------------------------------------------------

    @Test
    void administratorRestoresAHiddenPost() {
        Post p = post(UUID.randomUUID());
        p.moderate(ContentStatus.HIDDEN);
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        when(posts.save(p)).thenReturn(p);
        when(reactions.counts(p.getId())).thenReturn(Map.of());

        var result = new ModeratePostUseCase(posts, reactions).execute(admin(), new ModeratePostCommand(p.getId(), ContentStatus.ACTIVE));

        assertEquals(ContentStatus.ACTIVE, result.status());
        verify(posts).save(p);
    }

    @Test
    void administratorCannotHideADeletedPost() {
        UUID author = UUID.randomUUID();
        Post p = post(author);
        p.delete(author);
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        var error = assertThrows(CommunityException.class,
                () -> new ModeratePostUseCase(posts, reactions).execute(admin(), new ModeratePostCommand(p.getId(), ContentStatus.HIDDEN)));
        assertEquals("Deleted post cannot be moderated", error.getMessage());
        verify(posts, never()).save(any());
    }

    @Test
    void administratorRestoresAHiddenComment() {
        Comment c = Comment.create(UUID.randomUUID(), UUID.randomUUID(), null, "body");
        c.moderate(ContentStatus.HIDDEN);
        when(comments.findById(c.getId())).thenReturn(Optional.of(c));
        when(comments.save(c)).thenReturn(c);

        var result = new ModerateCommentUseCase(comments).execute(admin(), new ModerateCommentCommand(c.getId(), ContentStatus.ACTIVE));

        assertEquals(ContentStatus.ACTIVE, result.status());
    }

    @Test
    void administratorCannotHideADeletedComment() {
        UUID author = UUID.randomUUID();
        Comment c = Comment.create(UUID.randomUUID(), author, null, "body");
        c.delete(author);
        when(comments.findById(c.getId())).thenReturn(Optional.of(c));
        var error = assertThrows(CommunityException.class,
                () -> new ModerateCommentUseCase(comments).execute(admin(), new ModerateCommentCommand(c.getId(), ContentStatus.HIDDEN)));
        assertEquals("Deleted comment cannot be moderated", error.getMessage());
        verify(comments, never()).save(any());
    }

    // ---- who sees what --------------------------------------------------------------------------------------

    @Test
    void normalUserCannotOpenADeletedPost() {
        UUID author = UUID.randomUUID();
        Post p = post(author);
        p.delete(author);
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        var error = assertThrows(CommunityNotFoundException.class,
                () -> new GetPostUseCase(posts, reactions).execute(p.getId(), user(UUID.randomUUID())));
        assertEquals("Post not found", error.getMessage());
        verifyNoInteractions(reactions);
    }

    @Test
    void normalUserCannotListCommentsOfAHiddenPost() {
        Post p = post(UUID.randomUUID());
        p.moderate(ContentStatus.HIDDEN);
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        var error = assertThrows(CommunityNotFoundException.class,
                () -> new ListCommentsUseCase(posts, comments).execute(user(UUID.randomUUID()), p.getId(), new PageQuery(0, 20)));
        assertEquals("Post not found", error.getMessage());
        verifyNoInteractions(comments);
    }

    @Test
    void commentListAsksForActiveCommentsOnly() {
        Post p = post(UUID.randomUUID());
        when(posts.findById(p.getId())).thenReturn(Optional.of(p));
        when(comments.findByPostIdAndStatus(p.getId(), ContentStatus.ACTIVE, 0, 20)).thenReturn(new CommunityPage<Comment>(List.of(), 0));

        new ListCommentsUseCase(posts, comments).execute(user(UUID.randomUUID()), p.getId(), new PageQuery(0, 20));

        verify(comments).findByPostIdAndStatus(p.getId(), ContentStatus.ACTIVE, 0, 20);
    }
}
