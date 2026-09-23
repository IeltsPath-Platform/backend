package com.group01.community.infrastructure.persistence;

import com.group01.community.domain.vo.ContentStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class CommentJpaEntity {
    @Id
    public UUID id;
    @Column(name = "post_id", nullable = false)
    public UUID postId;
    @Column(name = "author_id", nullable = false)
    public UUID authorId;
    @Column(name = "parent_comment_id")
    public UUID parentCommentId;
    @Column(nullable = false, columnDefinition = "text")
    public String body;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ContentStatus status;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
    @Version
    public Long version;

    public CommentJpaEntity() {
    }
}
