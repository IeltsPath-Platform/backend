package com.ieltspath.user.api.dto.request;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record StartStudySessionRequest(@NotNull UUID clientSessionId,
        @NotNull @Pattern(regexp="LESSON|PRACTICE|REVIEW|ASSESSMENT|VOCABULARY_REVIEW") String sourceType,
        @NotNull UUID sourceId, boolean takeover) {}
