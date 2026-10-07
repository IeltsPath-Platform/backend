package com.ieltspath.community.api.dto.request;

import com.ieltspath.community.domain.vo.ContentStatus;
import jakarta.validation.constraints.NotNull;

public record ModerationRequest(@NotNull ContentStatus status) {
}
