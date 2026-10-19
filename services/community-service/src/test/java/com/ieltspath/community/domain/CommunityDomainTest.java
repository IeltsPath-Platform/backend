package com.ieltspath.community.domain;

import com.ieltspath.community.domain.aggregate.Comment;
import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.exception.CommunityException;
import com.ieltspath.community.domain.exception.CommunityForbiddenException;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.PostCategory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommunityDomainTest {
    @Test void postAuthorCanEditAndDelete(){UUID author=UUID.randomUUID();Post p=Post.create(author,PostCategory.GENERAL,"Title","Body");p.edit(author,PostCategory.QUESTION,null,"Updated");assertEquals("Updated",p.getBody());p.delete(author);assertEquals(ContentStatus.DELETED,p.getStatus());}
    @Test void nonAuthorCannotEditPost(){Post p=Post.create(UUID.randomUUID(),PostCategory.GENERAL,null,"Body");assertThrows(CommunityForbiddenException.class,()->p.edit(UUID.randomUUID(),PostCategory.GENERAL,null,"Other"));}

    @Test void shortTextPostAcceptsBackgroundAndItCanBeCleared() {
        Post p=Post.create(UUID.randomUUID(),PostCategory.GENERAL,null,"Short text");
        p.setBackground(com.ieltspath.community.domain.vo.PostBackground.SUNSET);
        assertEquals(com.ieltspath.community.domain.vo.PostBackground.SUNSET,p.getBackground());
        p.setBackground(null);
        assertEquals(null,p.getBackground());
    }
    @Test void backgroundIsRejectedWhenPostHasMediaOrLongBody() {
        var background=com.ieltspath.community.domain.vo.PostBackground.OCEAN;
        Post withImage=Post.create(UUID.randomUUID(),PostCategory.GENERAL,null,"Short");
        withImage.setMedia(java.util.List.of("https://images.unsplash.com/a.png"),null,null);
        assertThrows(CommunityException.class,()->withImage.setBackground(background));
        Post withVideo=Post.create(UUID.randomUUID(),PostCategory.GENERAL,null,"Short");
        withVideo.setMedia(java.util.List.of(),"https://www.youtube.com/watch?v=abc",null);
        assertThrows(CommunityException.class,()->withVideo.setBackground(background));
        Post longText=Post.create(UUID.randomUUID(),PostCategory.GENERAL,null,"x".repeat(com.ieltspath.community.domain.vo.PostBackground.MAX_BODY_LENGTH+1));
        assertThrows(CommunityException.class,()->longText.setBackground(background));
        Post limit=Post.create(UUID.randomUUID(),PostCategory.GENERAL,null,"x".repeat(com.ieltspath.community.domain.vo.PostBackground.MAX_BODY_LENGTH));
        limit.setBackground(background);
    }

    @Test
    void commentAuthorCanEditAndDelete() {
        UUID author = UUID.randomUUID();
        Comment comment = Comment.create(UUID.randomUUID(), author, null, "Body");
        comment.edit(author, "Updated");
        assertEquals("Updated", comment.getBody());
        comment.delete(author);
        assertEquals(ContentStatus.DELETED, comment.getStatus());
    }

    @Test
    void nonAuthorCannotEditComment() {
        Comment comment = Comment.create(UUID.randomUUID(), UUID.randomUUID(), null, "Body");
        assertThrows(CommunityForbiddenException.class, () -> comment.edit(UUID.randomUUID(), "Other"));
    }

    @Test
    void moderationCannotRestoreDeletedPost() {
        UUID author = UUID.randomUUID();
        Post p = Post.create(author, PostCategory.GENERAL, null, "Body");
        p.delete(author);
        assertThrows(CommunityException.class, () -> p.moderate(ContentStatus.ACTIVE));
    }
    @Test void commentKeepsParentReference(){UUID parent=UUID.randomUUID();Comment c=Comment.create(UUID.randomUUID(),UUID.randomUUID(),parent,"Reply");assertEquals(parent,c.getParentCommentId());}

    @Test
    void administratorCannotEditPostOfAnotherAuthor() {
        UUID author = UUID.randomUUID();
        Post p = Post.create(author, PostCategory.GENERAL, "Title", "Body");
        assertThrows(CommunityForbiddenException.class, () -> p.edit(UUID.randomUUID(), PostCategory.QUESTION, "New Title", "New Body"));
    }

    @Test
    void administratorCannotEditCommentOfAnotherAuthor() {
        UUID author = UUID.randomUUID();
        Comment comment = Comment.create(UUID.randomUUID(), author, null, "Body");
        assertThrows(CommunityForbiddenException.class, () -> comment.edit(UUID.randomUUID(), "New Body"));
    }

    @Test
    void administratorCanDeletePostAndComment() {
        UUID author = UUID.randomUUID(), admin = UUID.randomUUID();
        Post p = Post.create(author, PostCategory.GENERAL, "Title", "Body");
        p.delete(admin, true);
        assertEquals(ContentStatus.DELETED, p.getStatus());

        Comment c = Comment.create(UUID.randomUUID(), author, null, "Body");
        c.delete(admin, true);
        assertEquals(ContentStatus.DELETED, c.getStatus());
    }
}
