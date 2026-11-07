package com.ieltspath.community.infrastructure.persistence.entity;

import com.ieltspath.community.domain.vo.ReactionType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comment_reactions")
@IdClass(CommentReactionId.class)
public class CommentReactionJpaEntity {
    @Id
    @Column(name = "comment_id")
    public UUID commentId;

    @Id
    @Column(name = "user_id")
    public UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false)
    public ReactionType reactionType;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    protected CommentReactionJpaEntity() {
    }
}
