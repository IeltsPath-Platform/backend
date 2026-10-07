package com.ieltspath.community.infrastructure.persistence.entity;

import com.ieltspath.community.domain.vo.ReactionType;

import java.io.Serializable;
import java.util.UUID;

public record PostReactionId(UUID postId, UUID userId, ReactionType reactionType) implements Serializable {
}
