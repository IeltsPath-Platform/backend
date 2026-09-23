package com.group01.community.infrastructure.persistence;

import com.group01.community.domain.vo.ReactionType;

import java.io.Serializable;
import java.util.UUID;

public record PostReactionId(UUID postId, UUID userId, ReactionType reactionType) implements Serializable {
}
