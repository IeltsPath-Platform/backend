package com.ieltspath.content.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReplaceLessonKnowledgePointsRequest(@NotNull List<UUID> knowledgePointIds) {}
