package com.ieltspath.community.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

public record PostReactionId(UUID postId, UUID userId) implements Serializable {
}
