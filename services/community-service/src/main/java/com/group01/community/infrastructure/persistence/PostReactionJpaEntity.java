package com.group01.community.infrastructure.persistence;

import com.group01.community.domain.vo.ReactionType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_reactions")
@IdClass(PostReactionId.class)
public class PostReactionJpaEntity {
    @Id
    @Column(name = "post_id")
    public UUID postId;
    @Id
    @Column(name = "user_id")
    public UUID userId;
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type")
    public ReactionType reactionType;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    protected PostReactionJpaEntity() {
    }

    public PostReactionJpaEntity(UUID postId, UUID userId, ReactionType type) {
        this.postId = postId;
        this.userId = userId;
        this.reactionType = type;
        this.createdAt = Instant.now();
    }
}
