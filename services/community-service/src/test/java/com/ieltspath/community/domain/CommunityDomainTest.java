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
}
