package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.BlockType;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record LessonBlockRequest(@NotNull BlockType blockType, Integer sortOrder, String textContent, UUID assetId,
                                List<UUID> vocabularySenseIds, List<UUID> questionVersionIds,
                                List<UUID> knowledgePointIds) {}
