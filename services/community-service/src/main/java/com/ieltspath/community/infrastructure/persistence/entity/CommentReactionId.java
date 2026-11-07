package com.ieltspath.community.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

public record CommentReactionId(UUID commentId, UUID userId) implements Serializable {
}
