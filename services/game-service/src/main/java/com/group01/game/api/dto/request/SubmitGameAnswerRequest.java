package com.group01.game.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;

public record SubmitGameAnswerRequest(
        @NotEmpty Map<String, Object> responsePayload,
        @PositiveOrZero long durationMilliseconds
) {}
