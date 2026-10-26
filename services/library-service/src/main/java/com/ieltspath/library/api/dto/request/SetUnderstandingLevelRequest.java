package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.vo.UnderstandingLevel;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetUnderstandingLevelRequest(@NotNull UnderstandingLevel level, @NotNull UUID requestId) {}
