package com.ieltspath.game.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateRoomSettingsRequest(
        @NotNull @Min(3) @Max(6) Integer initialLives,
        @Min(5) Integer wordLimit,
        Boolean timeEnabled
) {}
