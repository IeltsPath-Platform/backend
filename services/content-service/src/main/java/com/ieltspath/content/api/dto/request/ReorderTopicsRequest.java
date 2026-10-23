package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** The new order of every topic of one parent; a null parent means the top-level topics. */
public record ReorderTopicsRequest(UUID parentTopicId, @NotEmpty List<UUID> topicIds) {
}
