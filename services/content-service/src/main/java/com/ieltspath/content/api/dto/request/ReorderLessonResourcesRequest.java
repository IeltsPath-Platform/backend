package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.LessonResourceType;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** New order of one type's resources. The other type keeps its order. */
public record ReorderLessonResourcesRequest(@NotNull LessonResourceType type, @NotNull List<UUID> resourceIds) {
}
