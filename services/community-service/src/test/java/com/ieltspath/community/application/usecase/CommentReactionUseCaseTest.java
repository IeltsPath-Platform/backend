package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.AddCommentReactionCommand;
import com.ieltspath.community.application.command.RemoveCommentReactionCommand;
import com.ieltspath.community.domain.aggregate.Comment;
import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.CommentReactionRepository;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.domain.vo.ReactionType;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommentReactionUseCaseTest {
    private final CommentRepository comments = mock(CommentRepository.class);
    private final PostRepository posts = mock(PostRepository.class);
    private final CommentReactionRepository reactions = mock(CommentReactionRepository.class);
    private final AddCommentReactionUseCase add = new AddCommentReactionUseCase(comments, posts, reactions);
    private final RemoveCommentReactionUseCase remove = new RemoveCommentReactionUseCase(comments, posts, reactions);

    @Test
    void allTypesCanBeAddedAndRemovedOnOwnRootCommentsAndReplies() {
        UUID actor = UUID.randomUUID();
        Post post = Post.create(actor, PostCategory.GENERAL, null, "post");
        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        for (UUID parent : new UUID[] {null, UUID.randomUUID()}) {
            Comment comment = Comment.create(post.getId(), actor, parent, "comment");
            when(comments.findById(comment.getId())).thenReturn(Optional.of(comment));
            for (ReactionType type : ReactionType.values()) {
                add.execute(new CommunityActor(actor, false), new AddCommentReactionCommand(comment.getId(), type));
                remove.execute(new CommunityActor(actor, false), new RemoveCommentReactionCommand(comment.getId(), type));
                verify(reactions).add(comment.getId(), actor, type);
                verify(reactions).remove(comment.getId(), actor, type);
            }
        }
    }

    @Test
    void missingCommentRejectsBothMutationsBeforeRepositoryWrites() {
        UUID id = UUID.randomUUID();
        var actor = new CommunityActor(UUID.randomUUID(), false);
        assertThrows(CommunityNotFoundException.class,
                () -> add.execute(actor, new AddCommentReactionCommand(id, ReactionType.LIKE)));
        assertThrows(CommunityNotFoundException.class,
                () -> remove.execute(actor, new RemoveCommentReactionCommand(id, ReactionType.LIKE)));
        verifyNoInteractions(posts, reactions);
    }

    @Test
    void hiddenAndDeletedCommentsRejectBothMutationsForOrdinaryViewers() {
        for (ContentStatus status : new ContentStatus[] {ContentStatus.HIDDEN, ContentStatus.DELETED}) {
            Comment comment = comment(status);
            when(comments.findById(comment.getId())).thenReturn(Optional.of(comment));
            var actor = new CommunityActor(UUID.randomUUID(), false);
            assertThrows(CommunityNotFoundException.class,
                    () -> add.execute(actor, new AddCommentReactionCommand(comment.getId(), ReactionType.LOVE)));
            assertThrows(CommunityNotFoundException.class,
                    () -> remove.execute(actor, new RemoveCommentReactionCommand(comment.getId(), ReactionType.LOVE)));
        }
        verifyNoInteractions(reactions);
    }

    @Test
    void hiddenDeletedAndMissingPostsRejectBothMutationsForOrdinaryViewers() {
        for (ContentStatus status : ContentStatus.values()) {
            Comment comment = comment(ContentStatus.ACTIVE);
            when(comments.findById(comment.getId())).thenReturn(Optional.of(comment));
            Post post = new Post(comment.getPostId(), UUID.randomUUID(), PostCategory.GENERAL, null,
                    "post", status, java.time.Instant.now(), java.time.Instant.now(), 0L);
            when(posts.findById(post.getId())).thenReturn(status == ContentStatus.ACTIVE ? Optional.empty() : Optional.of(post));
            var actor = new CommunityActor(UUID.randomUUID(), false);
            assertThrows(CommunityNotFoundException.class,
                    () -> add.execute(actor, new AddCommentReactionCommand(comment.getId(), ReactionType.LIKE)));
            assertThrows(CommunityNotFoundException.class,
                    () -> remove.execute(actor, new RemoveCommentReactionCommand(comment.getId(), ReactionType.LIKE)));
        }
        verifyNoInteractions(reactions);
    }

    @Test
    void administratorHasTheSameVisibilityBypassAsPostReactions() {
        var actor = new CommunityActor(UUID.randomUUID(), true);
        for (ContentStatus status : ContentStatus.values()) {
            Comment comment = comment(status);
            Post post = new Post(comment.getPostId(), UUID.randomUUID(), PostCategory.GENERAL, null,
                    "post", status, java.time.Instant.now(), java.time.Instant.now(), 0L);
            when(comments.findById(comment.getId())).thenReturn(Optional.of(comment));
            when(posts.findById(post.getId())).thenReturn(Optional.of(post));
            add.execute(actor, new AddCommentReactionCommand(comment.getId(), ReactionType.HELPFUL));
            remove.execute(actor, new RemoveCommentReactionCommand(comment.getId(), ReactionType.HELPFUL));
            verify(reactions).add(comment.getId(), actor.userId(), ReactionType.HELPFUL);
            verify(reactions).remove(comment.getId(), actor.userId(), ReactionType.HELPFUL);
        }
    }

    private static Comment comment(ContentStatus status) {
        var now = java.time.Instant.now();
        return new Comment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                "comment", status, now, now, 0L);
    }
}
