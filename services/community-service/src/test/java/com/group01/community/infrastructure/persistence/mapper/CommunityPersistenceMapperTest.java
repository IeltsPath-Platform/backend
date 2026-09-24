package com.group01.community.infrastructure.persistence.mapper;

import com.group01.community.domain.aggregate.Comment;
import com.group01.community.domain.aggregate.Post;
import com.group01.community.domain.vo.ContentStatus;
import com.group01.community.domain.vo.PostCategory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommunityPersistenceMapperTest {
    private final PostPersistenceMapper postMapper = new PostPersistenceMapper();
    private final CommentPersistenceMapper commentMapper = new CommentPersistenceMapper();

    @Test
    void mapsPostBetweenDomainAndPersistenceWithoutLosingState() {
        Instant createdAt = Instant.parse("2026-09-24T10:15:30Z");
        Post post = new Post(
                UUID.randomUUID(), UUID.randomUUID(), PostCategory.QUESTION, "Question", "Post body",
                ContentStatus.ACTIVE, createdAt, createdAt, 7L
        );
        post.moderate(ContentStatus.HIDDEN);

        Post restored = postMapper.toDomain(postMapper.toEntity(post));

        assertEquals(post.getId(), restored.getId());
        assertEquals(post.getAuthorId(), restored.getAuthorId());
        assertEquals(post.getCategory(), restored.getCategory());
        assertEquals(post.getTitle(), restored.getTitle());
        assertEquals(post.getBody(), restored.getBody());
        assertEquals(ContentStatus.HIDDEN, restored.getStatus());
        assertEquals(post.getCreatedAt(), restored.getCreatedAt());
        assertEquals(post.getUpdatedAt(), restored.getUpdatedAt());
        assertEquals(post.getVersion(), restored.getVersion());
    }

    @Test
    void mapsCommentBetweenDomainAndPersistenceWithoutLosingState() {
        Instant createdAt = Instant.parse("2026-09-24T10:15:30Z");
        Comment comment = new Comment(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Comment body",
                ContentStatus.ACTIVE, createdAt, createdAt, 11L
        );
        comment.moderate(ContentStatus.HIDDEN);

        Comment restored = commentMapper.toDomain(commentMapper.toEntity(comment));

        assertEquals(comment.getId(), restored.getId());
        assertEquals(comment.getPostId(), restored.getPostId());
        assertEquals(comment.getAuthorId(), restored.getAuthorId());
        assertEquals(comment.getParentCommentId(), restored.getParentCommentId());
        assertEquals(comment.getBody(), restored.getBody());
        assertEquals(ContentStatus.HIDDEN, restored.getStatus());
        assertEquals(comment.getCreatedAt(), restored.getCreatedAt());
        assertEquals(comment.getUpdatedAt(), restored.getUpdatedAt());
        assertEquals(comment.getVersion(), restored.getVersion());
    }
}
