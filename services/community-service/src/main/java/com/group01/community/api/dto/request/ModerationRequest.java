package com.group01.community.api.dto.request;

import com.group01.community.domain.vo.ContentStatus;
import jakarta.validation.constraints.NotNull;

public record ModerationRequest(@NotNull ContentStatus status) {
}
