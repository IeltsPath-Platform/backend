package com.group01.learning.domain.vo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TopicProgress(UUID topicId, Integer sequenceOrder, Instant passedAt) {
    public TopicProgress {
        Objects.requireNonNull(topicId, "topicId");
    }
}
