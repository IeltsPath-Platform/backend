package com.ieltspath.game.api.dto.request;

import com.ieltspath.game.domain.vo.WordlyRoomSettings;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Solo rooms only choose the question count; lives and seconds per question use the defaults. */
public record CreateRocketRoomRequest(
        @NotNull WordlyRoomSettings.PlayerMode playerMode,
        @NotNull @Min(3) @Max(20) Integer questionCount
) {}
